package uk.ac.ebi.gxa.index.builder.service;

import com.google.common.primitives.Longs;
import it.uniroma3.mat.extendedset.FastSet;
import ucar.ma2.ArrayFloat;
import uk.ac.ebi.gxa.index.builder.IndexAllCommand;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.index.builder.UpdateIndexForExperimentCommand;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.statistics.*;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.OntologyMapping;
import uk.ac.ebi.microarray.atlas.model.UpDownExpression;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.io.Closeables.closeQuietly;
import static java.lang.Math.round;
import static java.util.Collections.sort;

/**
 * Class used to build ConciseSet-based gene expression statistics index
 */
public class GeneAtlasBitIndexBuilderService extends IndexBuilderService {
    private AtlasNetCDFDAO atlasNetCDFDAO;
    private final String indexFileName;
    private File atlasIndex;
    private File indexFile = null;

    private StatisticsStorage statistics;

    public void setAtlasNetCDFDAO(AtlasNetCDFDAO atlasNetCDFDAO) {
        this.atlasNetCDFDAO = atlasNetCDFDAO;
    }

    public void setAtlasIndex(File atlasIndex) {
        this.atlasIndex = atlasIndex;
    }

    /**
     * Constructor
     *
     * @param indexFileName name of the serialized index file
     */
    public GeneAtlasBitIndexBuilderService(String indexFileName) {
        this.indexFileName = indexFileName;
    }


    @Override
    public void processCommand(IndexAllCommand indexAll,
                               IndexBuilderService.ProgressUpdater progressUpdater) throws IndexBuilderException {
        indexAll(progressUpdater);
    }

    @Override
    public void processCommand(UpdateIndexForExperimentCommand cmd,
                               IndexBuilderService.ProgressUpdater progressUpdater) throws IndexBuilderException {
        /// Re-build the whole bit index even if one experiment only is being updated
        indexAll(progressUpdater);
    }


    @Override
    public void finalizeCommand() throws IndexBuilderException {
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(new FileOutputStream(indexFile));
            oos.writeObject(statistics);
            getLog().info("Wrote serialized index successfully to: " + indexFile.getAbsolutePath());
        } catch (IOException ioe) {
            getLog().error("Error when saving serialized index: " + indexFile.getAbsolutePath(), ioe);
        } finally {
            closeQuietly(oos);
        }
    }

    @Override
    public void finalizeCommand(UpdateIndexForExperimentCommand updateIndexForExperimentCommand,
                                ProgressUpdater progressUpdater) throws IndexBuilderException {
        finalizeCommand();
    }

    private void indexAll(ProgressUpdater progressUpdater) {
        indexFile = new File(atlasIndex, getName());
        if (indexFile.exists() && !indexFile.delete()) {
            throw new IndexBuilderException("Cannot delete " + indexFile.getAbsolutePath());
        }
        statistics = bitIndexNetCDFs(progressUpdater, 50);
    }

    /**
     * Generates a ConciseSet-based index for all statistics types in StatisticsType enum, across all Atlas ncdfs
     *
     * @param progressUpdater
     * @param progressLogFreq how often this operation should be logged (i.e. every progressLogFreq ncfds processed)
     * @return StatisticsStorage containing statistics for all statistics types in StatisticsType enum - collected over all Atlas ncdfs
     */
    private StatisticsStorage bitIndexNetCDFs(
            final ProgressUpdater progressUpdater,
            final Integer progressLogFreq) {
        StatisticsStorage statisticsStorage = new StatisticsStorage();

        final ObjectPool<ExperimentInfo> experimentIndex = new ObjectPool<ExperimentInfo>();
        final ObjectPool<EfvAttribute> attributeIndex = new ObjectPool<EfvAttribute>();

        final StatisticsBuilder upStats = new ThreadSafeStatisticsBuilder();
        final StatisticsBuilder dnStats = new ThreadSafeStatisticsBuilder();
        final StatisticsBuilder updnStats = new ThreadSafeStatisticsBuilder();
        final StatisticsBuilder noStats = new ThreadSafeStatisticsBuilder();

        BitIndexTask task = new BitIndexTask(ncdfsToProcess());

        getLog().info("Found total ncdfs to index: " + task.getTotalFiles());

        final ExecutorService summarizer = Executors.newFixedThreadPool(10);

        for (final File f : task.getFiles()) {
            NetCDFProxy ncdf = null;
            getLog().debug("Processing {}", f);
            try {
                ncdf = new NetCDFProxy(f);
                if (ncdf.isOutOfDate()) {
                    throw new IndexBuilderException("NetCDF " + f.getCanonicalPath() + " is out of date");
                }

                final Experiment exp = getAtlasDAO().getExperimentByAccession(ncdf.getExperimentAccession());
                final ExperimentInfo experiment = experimentIndex.intern(new ExperimentInfo(exp.getAccession(), exp.getId()));

                // TODO when we switch on inclusion of sc-scv stats in bit index, the call below
                // TODO should change to ncdf.getUniqueValues()
                List<String> uVals = ncdf.getUniqueFactorValues();
                int car = 0; // count of all Statistics records added for this ncdf

                if (uVals.size() == 0) {
                    task.skipEmpty(f);
                    getLog().info("Skipping empty " + f.getCanonicalPath());
                    continue;
                }

                long[] bioEntityIdsArr = ncdf.getGenes();
                List<Integer> bioEntityIds = new ArrayList<Integer>(bioEntityIdsArr.length);
                for (long bioEntityId : bioEntityIdsArr) {
                    if (bioEntityId <= Integer.MAX_VALUE) {
                        bioEntityIds.add((int) bioEntityId);
                    } else {
                        throw new IndexBuilderException("bioEntityId: " + bioEntityId + " too large to be cast to int safely- unable to build bit index");
                    }
                }

                ArrayFloat.D2 tstat = ncdf.getTStatistics();
                ArrayFloat.D2 pvals = ncdf.getPValues();
                int[] shape = tstat.getShape();

                final Map<EfvAttribute, MinPMaxT> efToPTUpDown = new HashMap<EfvAttribute, MinPMaxT>();
                for (int j = 0; j < uVals.size(); j++) {
                    String[] arr = uVals.get(j).split(NetCDFProxy.NCDF_PROP_VAL_SEP_REGEX);
                    String ef = arr[0];
                    String efv = arr.length == 1 ? "" : arr[1];

                    final EfvAttribute efvAttribute = attributeIndex.intern(new EfvAttribute(ef, efv, null));
                    final EfvAttribute efAttribute = attributeIndex.intern(new EfvAttribute(ef, null));

                    final Set<Integer> upBioEntityIds = new FastSet();
                    final Set<Integer> dnBioEntityIds = new FastSet();
                    final Set<Integer> noBioEntityIds = new FastSet();

                    // Initialise if necessary pval/tstat storage for ef
                    MinPMaxT ptUpDownForEf = efToPTUpDown.get(efAttribute);
                    if (ptUpDownForEf == null) {
                        efToPTUpDown.put(efAttribute, ptUpDownForEf = new MinPMaxT());
                    }

                    // Initialise pval/tstat storage for ef-efv/sc-scv
                    final MinPMaxT ptUpDown = new MinPMaxT();
                    final MinPMaxT ptUp = new MinPMaxT();
                    final MinPMaxT ptDown = new MinPMaxT();

                    for (int i = 0; i < shape[0]; i++) {
                        int bioEntityId = bioEntityIds.get(i);
                        if (bioEntityId == 0) continue;

                        float t = tstat.get(i, j);
                        float p = pvals.get(i, j);
                        UpDownExpression upDown = UpDownExpression.valueOf(p, t);

                        car++;
                        if (upDown.isNonDe()) {
                            noBioEntityIds.add(bioEntityId);
                        } else {
                            if (upDown.isUp()) {
                                upBioEntityIds.add(bioEntityId);
                                // Store if the lowest pVal/highest absolute value of tStat for ef-efv (up)
                                ptUp.update(bioEntityId, p, t);
                            } else {
                                dnBioEntityIds.add(bioEntityId);
                                // Store if the lowest pVal/highest absolute value of tStat for ef-efv/sc-scv (down)
                                ptDown.update(bioEntityId, p, t);
                            }
                            // Store if the lowest pVal/highest absolute value of tStat for ef-efv/sc-scv (up/down)
                            ptUpDown.update(bioEntityId, p, t);
                            // Store if the lowest pVal/highest absolute value of tStat for ef/sc  (up/down)
                            ptUpDownForEf.update(bioEntityId, p, t);
                        }
                    }

                    summarizer.submit(new Runnable() {
                        @Override
                        public void run() {
                            // Store rounded minimum up pVals per gene for ef-efv/sc-scv
                            ptUp.storeStats(upStats, experiment, efvAttribute);
                            // Store rounded minimum down pVals per gene for ef-efv/sc-scv
                            ptDown.storeStats(dnStats, experiment, efvAttribute);
                            // Store rounded minimum up/down pVals per gene for ef-efv/sc-scv
                            ptUpDown.storeStats(updnStats, experiment, efvAttribute);
                        }
                    });

                    // Store stats for ef-efv/sc-scv
                    upStats.addStatistics(efvAttribute, experiment, upBioEntityIds);
                    dnStats.addStatistics(efvAttribute, experiment, dnBioEntityIds);
                    updnStats.addStatistics(efvAttribute, experiment, upBioEntityIds);
                    updnStats.addStatistics(efvAttribute, experiment, dnBioEntityIds);
                    noStats.addStatistics(efvAttribute, experiment, noBioEntityIds);

                    // Store stats for ef/sc
                    upStats.addStatistics(efAttribute, experiment, upBioEntityIds);
                    dnStats.addStatistics(efAttribute, experiment, dnBioEntityIds);
                    updnStats.addStatistics(efAttribute, experiment, upBioEntityIds);
                    updnStats.addStatistics(efAttribute, experiment, dnBioEntityIds);
                    noStats.addStatistics(efAttribute, experiment, noBioEntityIds);

                    // Add genes for ef/sc attributes across all experiments
                    updnStats.addBioEntitiesForEfAttribute(efAttribute, upBioEntityIds);
                    updnStats.addBioEntitiesForEfAttribute(efAttribute, dnBioEntityIds);

                    // Add genes for ef-efv/sc-scv attributes across all experiments
                    updnStats.addBioEntitiesForEfvAttribute(efvAttribute, upBioEntityIds);
                    updnStats.addBioEntitiesForEfvAttribute(efvAttribute, dnBioEntityIds);
                }

                summarizer.submit(new Runnable() {
                    @Override
                    public void run() {
                        // Store rounded minimum up/down pVals per gene for all efs/scs
                        for (Map.Entry<EfvAttribute, MinPMaxT> entry : efToPTUpDown.entrySet()) {
                            // Store min up/down pVal for efv
                            entry.getValue().storeStats(updnStats, experiment, entry.getKey());
                        }
                    }
                });

                task.processedStats(car);
                if (car == 0) {
                    getLog().debug(f.getName() + " num uVals : " + uVals.size() + " [" + car + "]");
                }

                task.done(f);
                progressUpdater.update(task.progress());
            } catch (IOException e) {
                throw new IndexBuilderException(e.getMessage(), e);
            } finally {
                closeQuietly(ncdf);
            }
        }

        try {
            // Load efo index
            EfoIndex efoIndex = loadEfoMapping(attributeIndex, experimentIndex);
            statisticsStorage.setEfoIndex(efoIndex);

            // wait for statistics updates to finish
            getLog().info("Waiting for summarizers to finish..");
            summarizer.shutdown();
            summarizer.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
            getLog().info("Background jobs done");

            getLog().info("Total statistics data set " + (task.getTotalStatCount() * 8L) / 1024 + " kB");

            // Set statistics
            statisticsStorage.addStatistics(StatisticsType.UP, upStats.getStatistics());
            statisticsStorage.addStatistics(StatisticsType.DOWN, dnStats.getStatistics());
            statisticsStorage.addStatistics(StatisticsType.UP_DOWN, updnStats.getStatistics());
            statisticsStorage.addStatistics(StatisticsType.NON_D_E, noStats.getStatistics());

            // Pre-compute scores for all genes across all efo's. These scores are used to score and then sort
            // genes in user queries with no efv/efo conditions specified.
            statisticsStorage.computeScoresAcrossAllEfos();
        } catch (InterruptedException e) {
            getLog().error("Indexing interrupted!", e);
            throw new IndexBuilderException(e.getMessage(), e);
        }

        return statisticsStorage;
    }

    private List<File> ncdfsToProcess() {
        final List<File> files = atlasNetCDFDAO.getAllNcdfs();
        sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return -Longs.compare(o1.length(), o2.length());
            }
        });
        return files;
    }

    public String getName() {
        return indexFileName;
    }

    private EfoIndex loadEfoMapping(ObjectPool<EfvAttribute> attributeIndex, ObjectPool<ExperimentInfo> experimentIndex) {

        EfoIndex efoIndex = new EfoIndex();
        getLog().info("Fetching ontology mappings...");

        Set<String> allEfos = new HashSet<String>();
        int missingExpsNum = 0, missingAttrsNum = 0, LoadedCompleteEfos = 0, LoadedInCompleteEfos = 0;
        List<OntologyMapping> mappings = getAtlasDAO().getOntologyMappingsByOntology("EFO");
        for (OntologyMapping mapping : mappings) {
            ExperimentInfo exp = new ExperimentInfo(mapping.getExperimentAccession(), mapping.getExperimentId());
            EfvAttribute attr = new EfvAttribute(mapping.getProperty(), mapping.getPropertyValue(), null);
            EfvAttribute attributeIdx = attributeIndex.intern(attr);
            ExperimentInfo experimentIdx = experimentIndex.intern(exp);

            if (attributeIdx == null) {
                attributeIdx = attributeIndex.intern(attr);
                getLog().debug(
                        "BitIndex build: efo term: " + mapping.getOntologyTerm() + " maps to a missing attribute: " + attr + " -> adding it to Attribute Index");
            }
            if (experimentIdx == null) {
                missingExpsNum++;
                getLog().error(
                        "BitIndex build: Incomplete load for efo term: " + mapping.getOntologyTerm() + " because experiment: " + exp + " could not be found in Experiment Index");
            }

            if (attributeIdx != null && experimentIdx != null) {
                LoadedCompleteEfos++;
                efoIndex.addMapping(mapping.getOntologyTerm(), attributeIdx, experimentIdx);
                getLog().debug(
                        "Adding: " + mapping.getOntologyTerm() + ":" + attr + " (" + attributeIdx + "):" + exp + " (" + experimentIdx + ")");
            } else {
                LoadedInCompleteEfos++;
            }
            allEfos.add(mapping.getOntologyTerm());
        }
        getLog().info(String.format(
                "Loaded %d ontology mappings (Load incomplete for %d due to missing %d experiments or missing %d attributes",
                LoadedCompleteEfos, LoadedInCompleteEfos, missingExpsNum, missingAttrsNum));

        allEfos.removeAll(efoIndex.getEfos());
        getLog().info("The following " + allEfos.size() + " efo's have not been loaded at all:" + allEfos);
        return efoIndex;
    }

    static class MinPMaxT {
        private static final float PRECISION = 1e-3F;

        private Map<Integer, Float> geneToMinP = new HashMap<Integer, Float>();
        private Map<Integer, Float> geneToMaxT = new HashMap<Integer, Float>();

        public void update(int bioEntityId, float p, float t) {
            final int absT = Math.abs((int) t);

            final Float maxT = geneToMaxT.get(bioEntityId);
            // TODO: for some reason, we trim max T stat value, but do not trim the actual value we've stored. Is it okay?
            if (maxT == null || absT > Math.abs(maxT) || absT == Math.abs(maxT) && p < geneToMinP.get(bioEntityId)) {
                geneToMinP.put(bioEntityId, p);
                geneToMaxT.put(bioEntityId, t);
            }
        }

        public void storeStats(StatisticsBuilder stats, ExperimentInfo expIdx, EfvAttribute efvAttributeIndex) {
            for (Map.Entry<Integer, Float> entry : geneToMinP.entrySet()) {
                Short tStatRank = StatisticsQueryUtils.getTStatRank(geneToMaxT.get(entry.getKey()));
                stats.addPvalueTstatRank(efvAttributeIndex, roundToDesiredPrecision(entry.getValue()), tStatRank, expIdx, entry.getKey());
            }
        }

        private Float roundToDesiredPrecision(float value) {
            return round(value / PRECISION) * PRECISION;
        }
    }
}
