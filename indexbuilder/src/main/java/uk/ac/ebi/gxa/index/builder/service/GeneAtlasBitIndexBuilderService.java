package uk.ac.ebi.gxa.index.builder.service;

import com.google.common.base.Function;
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

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.io.Closeables.closeQuietly;
import static java.lang.Math.round;
import static java.util.Collections.sort;

/**
 * Class used to build ConciseSet-based gene expression statistics index
 */
public class GeneAtlasBitIndexBuilderService extends IndexBuilderService {
    private static final double GiB = 1024.0 * 1024.0 * 1024.0;

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

        final ObjectIndex<ExperimentInfo> experimentIndex = new ObjectIndex<ExperimentInfo>();
        final ObjectIndex<EfvAttribute> attributeIndex = new ObjectIndex<EfvAttribute>();

        final StatisticsBuilder upStats = new ThreadSafeStatisticsBuilder();
        final StatisticsBuilder dnStats = new ThreadSafeStatisticsBuilder();
        final StatisticsBuilder updnStats = new ThreadSafeStatisticsBuilder();
        final StatisticsBuilder noStats = new ThreadSafeStatisticsBuilder();

        List<File> ncdfs = ncdfsToProcess();

        final AtomicInteger totalStatCount = new AtomicInteger();
        final int total = ncdfs.size();
        final long totalSize = size(ncdfs);
        getLog().info("Found total ncdfs to index: " + total);

        // fetch experiments - we want to include public experiments only in the index
        final Collection<Long> allExperimentIds = transform(
                getAtlasDAO().getAllExperiments(),
                new Function<Experiment, Long>() {
                    public Long apply(@Nonnull Experiment input) {
                        return input.getId();
                    }
                });

        int processedNcdfsCount = 0;
        long processedNcdfsSize = 0;
        // Count of ncdfs in which no efvs were found
        final AtomicInteger noEfvsNcdfCount = new AtomicInteger(0);

        final long timeStart = System.currentTimeMillis();

        final ExecutorService summarizer = Executors.newFixedThreadPool(10);

        for (final File nc : ncdfs) {
            NetCDFProxy ncdf = null;
            getLog().debug("Processing {}", nc);
            try {
                ncdf = new NetCDFProxy(nc);
                if (ncdf.isOutOfDate()) {
                    throw new IndexBuilderException("NetCDF " + nc.getCanonicalPath() + " is out of date");
                }

                if (!allExperimentIds.contains(ncdf.getExperimentId())) {
                    processedNcdfsCount++;
                    getLog().info("Excluding from index private experiment: " + ncdf.getExperimentAccession());
                    continue;
                }

                ExperimentInfo experiment = new ExperimentInfo(ncdf.getExperimentAccession(), ncdf.getExperimentId());
                final Integer expIdx = experimentIndex.addObject(experiment);

                // TODO when we switch on inclusion of sc-scv stats in bit index, the call below
                // TODO should change to ncdf.getUniqueValues()
                List<String> uVals = ncdf.getUniqueFactorValues();
                int car = 0; // count of all Statistics records added for this ncdf

                if (uVals.size() == 0) {
                    processedNcdfsCount++;
                    noEfvsNcdfCount.incrementAndGet();
                    getLog().info("Skipping empty " + nc.getCanonicalPath());
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

                final Map<Integer, MinPMaxT> efToPTUpDown = new HashMap<Integer, MinPMaxT>();
                for (int j = 0; j < uVals.size(); j++) {
                    String[] arr = uVals.get(j).split(NetCDFProxy.NCDF_PROP_VAL_SEP_REGEX);
                    String ef = arr[0];
                    String efv = arr.length == 1 ? "" : arr[1];

                    final Integer efvAttributeIndex = attributeIndex.addObject(new EfvAttribute(ef, efv, null));
                    final Integer efAttributeIndex = attributeIndex.addObject(new EfvAttribute(ef, null));

                    final Set<Integer> upBioEntityIds = new FastSet();
                    final Set<Integer> dnBioEntityIds = new FastSet();
                    final Set<Integer> noBioEntityIds = new FastSet();

                    // Initialise if necessary pval/tstat storage for ef
                    MinPMaxT ptUpDownForEf = efToPTUpDown.get(efAttributeIndex);
                    if (ptUpDownForEf == null) {
                        efToPTUpDown.put(efAttributeIndex, ptUpDownForEf = new MinPMaxT());
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
                            ptUp.storeStats(upStats, expIdx, efvAttributeIndex);
                            // Store rounded minimum down pVals per gene for ef-efv/sc-scv
                            ptDown.storeStats(dnStats, expIdx, efvAttributeIndex);
                            // Store rounded minimum up/down pVals per gene for ef-efv/sc-scv
                            ptUpDown.storeStats(updnStats, expIdx, efvAttributeIndex);
                        }
                    });

                    // Store stats for ef-efv/sc-scv
                    upStats.addStatistics(efvAttributeIndex, expIdx, upBioEntityIds);
                    dnStats.addStatistics(efvAttributeIndex, expIdx, dnBioEntityIds);
                    updnStats.addStatistics(efvAttributeIndex, expIdx, upBioEntityIds);
                    updnStats.addStatistics(efvAttributeIndex, expIdx, dnBioEntityIds);
                    noStats.addStatistics(efvAttributeIndex, expIdx, noBioEntityIds);

                    // Store stats for ef/sc
                    upStats.addStatistics(efAttributeIndex, expIdx, upBioEntityIds);
                    dnStats.addStatistics(efAttributeIndex, expIdx, dnBioEntityIds);
                    updnStats.addStatistics(efAttributeIndex, expIdx, upBioEntityIds);
                    updnStats.addStatistics(efAttributeIndex, expIdx, dnBioEntityIds);
                    noStats.addStatistics(efAttributeIndex, expIdx, noBioEntityIds);

                    // Add genes for ef/sc attributes across all experiments
                    updnStats.addBioEntitiesForEfAttribute(efAttributeIndex, upBioEntityIds);
                    updnStats.addBioEntitiesForEfAttribute(efAttributeIndex, dnBioEntityIds);

                    // Add genes for ef-efv/sc-scv attributes across all experiments
                    updnStats.addBioEntitiesForEfvAttribute(efvAttributeIndex, upBioEntityIds);
                    updnStats.addBioEntitiesForEfvAttribute(efvAttributeIndex, dnBioEntityIds);
                }

                summarizer.submit(new Runnable() {
                    @Override
                    public void run() {
                        // Store rounded minimum up/down pVals per gene for all efs/scs
                        for (Map.Entry<Integer, MinPMaxT> entry : efToPTUpDown.entrySet()) {
                            // Store min up/down pVal for efv
                            entry.getValue().storeStats(updnStats, expIdx, entry.getKey());
                        }
                    }
                });

                totalStatCount.addAndGet(car);
                if (car == 0) {
                    getLog().debug(nc.getName() + " num uVals : " + uVals.size() + " [" + car + "]");
                }

                processedNcdfsCount++;
                processedNcdfsSize += nc.length();
                progressUpdater.update(String.format("%d/%d(%.1f/%.1fG)", processedNcdfsCount, total,
                        processedNcdfsSize / GiB, totalSize / GiB));
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

            getLog().info("Total statistics data set " + (totalStatCount.get() * 8L) / 1024 + " kB");

            // Set statistics
            statisticsStorage.addStatistics(StatisticsType.UP, upStats.getStatistics());
            statisticsStorage.addStatistics(StatisticsType.DOWN, dnStats.getStatistics());
            statisticsStorage.addStatistics(StatisticsType.UP_DOWN, updnStats.getStatistics());
            statisticsStorage.addStatistics(StatisticsType.NON_D_E, noStats.getStatistics());

            // Set indexes for experiments and attributes
            statisticsStorage.setExperimentIndex(experimentIndex);
            statisticsStorage.setAttributeIndex(attributeIndex);

            // Pre-compute scores for all genes across all efo's. These scores are used to score and then sort
            // genes in user queries with no efv/efo conditions specified.
            statisticsStorage.computeScoresAcrossAllEfos();
        } catch (InterruptedException e) {
            getLog().error("Indexing interrupted!", e);
            throw new IndexBuilderException(e.getMessage(), e);
        }

        return statisticsStorage;
    }

    private long size(List<File> files) {
        long result = 0;
        for (File f : files) {
            result += f.length();
        }
        return result;
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

    private EfoIndex loadEfoMapping(ObjectIndex<EfvAttribute> attributeIndex, ObjectIndex<ExperimentInfo> experimentIndex) {

        EfoIndex efoIndex = new EfoIndex();
        getLog().info("Fetching ontology mappings...");

        Set<String> allEfos = new HashSet<String>();
        int missingExpsNum = 0, missingAttrsNum = 0, LoadedCompleteEfos = 0, LoadedInCompleteEfos = 0;
        List<OntologyMapping> mappings = getAtlasDAO().getOntologyMappingsByOntology("EFO");
        for (OntologyMapping mapping : mappings) {
            ExperimentInfo exp = new ExperimentInfo(mapping.getExperimentAccession(), mapping.getExperimentId());
            EfvAttribute attr = new EfvAttribute(mapping.getProperty(), mapping.getPropertyValue(), null);
            Integer attributeIdx = attributeIndex.getIndexForObject(attr);
            Integer experimentIdx = experimentIndex.getIndexForObject(exp);

            if (attributeIdx == null) {
                attributeIdx = attributeIndex.addObject(attr);
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

        public void storeStats(StatisticsBuilder stats, int expIdx, int efvAttributeIndex) {
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
