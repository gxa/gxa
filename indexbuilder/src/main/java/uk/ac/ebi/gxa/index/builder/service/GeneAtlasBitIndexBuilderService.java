package uk.ac.ebi.gxa.index.builder.service;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import ucar.ma2.ArrayFloat;
import uk.ac.ebi.gxa.index.builder.IndexAllCommand;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.index.builder.UpdateIndexForExperimentCommand;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.statistics.*;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;
import uk.ac.ebi.microarray.atlas.model.OntologyMapping;
import uk.ac.ebi.microarray.atlas.model.UpDownExpression;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.io.Closeables.closeQuietly;
import static java.lang.Math.round;

/**
 * Class used to build ConciseSet-based gene expression statistics index
 */
public class GeneAtlasBitIndexBuilderService extends IndexBuilderService {
    private AtlasNetCDFDAO atlasNetCDFDAO;
    private final String indexFileName;
    private File atlasIndex;
    private File indexFile = null;

    private StatisticsStorage statistics;
    private ExecutorService executor;


    public void setAtlasNetCDFDAO(AtlasNetCDFDAO atlasNetCDFDAO) {
        this.atlasNetCDFDAO = atlasNetCDFDAO;
    }

    public void setAtlasIndex(File atlasIndex) {
        this.atlasIndex = atlasIndex;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
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

        List<File> ncdfs = atlasNetCDFDAO.getAllNcdfs();

        final AtomicInteger totalStatCount = new AtomicInteger();
        final Integer total = ncdfs.size();
        getLog().info("Found total ncdfs to index: " + total);

        // fetch experiments - we want to include public experiments only in the index
        final Collection<Long> allExperimentIds = Collections2.transform(
                getAtlasModel().getAllExperiments()
                , new Function<Experiment, Long>() {
                    public Long apply(@Nonnull Experiment input) {
                        return input.getId();
                    }
                });

        final AtomicInteger processedNcdfsCount = new AtomicInteger(0);
        // Count of ncdfs in which no efvs were found
        final AtomicInteger noEfvsNcdfCount = new AtomicInteger(0);

        final long timeStart = System.currentTimeMillis();

        List<Callable<Boolean>> tasks = new ArrayList<Callable<Boolean>>(total);
        for (final File nc : ncdfs)
            tasks.add(new Callable<Boolean>() {
                public Boolean call() throws IOException {
                    NetCDFProxy ncdf = null;
                    getLog().debug("Processing {}", nc);
                    try {
                        ncdf = new NetCDFProxy(nc);
                        if (ncdf.isOutOfDate()) {
                            // Fail index build if a given ncdf is out of date
                            return false;
                        } else if (!allExperimentIds.contains(ncdf.getExperimentId())) {
                            processedNcdfsCount.incrementAndGet();
                            getLog().info("Excluding from index private experiment: " + ncdf.getExperimentAccession());
                            // TODO: returning true-false-null is a bug prone approach.
                            return null;
                        }

                        ExperimentInfo experiment = new ExperimentInfo(ncdf.getExperimentAccession(), ncdf.getExperimentId());
                        Integer expIdx = experimentIndex.addObject(experiment);

                        // TODO when we switch on inclusion of sc-scv stats in bit index, the call below
                        // TODO should change to ncdf.getUniqueValues()
                        List<String> uVals = ncdf.getUniqueFactorValues();
                        int car = 0; // count of all Statistics records added for this ncdf

                        if (uVals.size() == 0) {
                            processedNcdfsCount.incrementAndGet();
                            noEfvsNcdfCount.incrementAndGet();
                            // TODO: returning true-false-null is a bug prone approach.
                            return null;
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

                        Map<Integer, MinPMaxT> efToPTUpDown = new HashMap<Integer, MinPMaxT>();
                        for (int j = 0; j < uVals.size(); j++) {
                            String[] arr = uVals.get(j).split(NetCDFProxy.NCDF_PROP_VAL_SEP_REGEX);
                            String ef = arr[0];
                            String efv = arr.length == 1 ? "" : arr[1];

                            final Integer efvAttributeIndex = attributeIndex.addObject(new EfvAttribute(ef, efv, null));
                            final Integer efAttributeIndex = attributeIndex.addObject(new EfvAttribute(ef, null));

                            final SortedSet<Integer> upBioEntityIds = new TreeSet<Integer>();
                            final SortedSet<Integer> dnBioEntityIds = new TreeSet<Integer>();
                            final SortedSet<Integer> noBioEntityIds = new TreeSet<Integer>();

                            // Initialise if necessary pval/tstat storage for ef
                            MinPMaxT ptUpDownForEf = efToPTUpDown.get(efAttributeIndex);
                            if (ptUpDownForEf == null) {
                                efToPTUpDown.put(efAttributeIndex, ptUpDownForEf = new MinPMaxT());
                            }

                            // Initialise pval/tstat storage for ef-efv/sc-scv
                            MinPMaxT ptUpDown = new MinPMaxT();
                            MinPMaxT ptUp = new MinPMaxT();
                            MinPMaxT ptDown = new MinPMaxT();

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

                            // Store rounded minimum up pVals per gene for ef-efv/sc-scv
                            ptUp.storeStats(upStats, expIdx, efvAttributeIndex);
                            // Store rounded minimum down pVals per gene for ef-efv/sc-scv
                            ptDown.storeStats(dnStats, expIdx, efvAttributeIndex);
                            // Store rounded minimum up/down pVals per gene for ef-efv/sc-scv
                            ptUpDown.storeStats(updnStats, expIdx, efvAttributeIndex);

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

                        // Store rounded minimum up/down pVals per gene for all efs/scs
                        for (Map.Entry<Integer, MinPMaxT> entry : efToPTUpDown.entrySet()) {
                            // Store min up/down pVal for efv
                            entry.getValue().storeStats(updnStats, expIdx, entry.getKey());
                        }

                        totalStatCount.addAndGet(car);
                        if (car == 0) {
                            getLog().debug(nc.getName() + " num uVals : " + uVals.size() + " [" + car + "]");
                        }


                        int processedNow = processedNcdfsCount.incrementAndGet();
                        if (processedNow % progressLogFreq == 0 || processedNow == total) {
                            long timeNow = System.currentTimeMillis();
                            long elapsed = timeNow - timeStart;
                            double speed = (processedNow / (elapsed / Double.valueOf(progressLogFreq)));  // (item/s)
                            double estimated = (total - processedNow) / (speed * 60);

                            getLog().info(
                                    String.format(
                                            "Processed %d/%d (# ncdfs with no EFVs so far: %d) ncdfs %d%%, %.1f ncdfs/sec overall, estimated %.1f min remaining",
                                            processedNow, total, noEfvsNcdfCount.get(), (processedNow * 100 / total),
                                            speed, estimated));

                            if (processedNow == total) {
                                getLog().info("Overall processing time: " + (elapsed / 60000) + " min");
                            }

                            progressUpdater.update(processedNow + "/" + total);
                        }

                        return true;
                    } catch (Throwable t) {
                        // TODO: in case an error occured here, we do not do any error processing, thus leaving the user with
                        // a broken/incomplete index
                        getLog().error("Error occurred: ", t);
                        return false;
                    } finally {
                        closeQuietly(ncdf);
                    }
                }
            });


        try {
            final List<Future<Boolean>> futures = executor.invokeAll(tasks);
            for (Future<Boolean> next : futures) {
                next.get();
            }

            getLog().info("Total statistics data set " + (totalStatCount.get() * 8L) / 1024 + " kB");

            // Set statistics
            statisticsStorage.addStatistics(StatisticsType.UP, upStats.getStatistics());
            statisticsStorage.addStatistics(StatisticsType.DOWN, dnStats.getStatistics());
            statisticsStorage.addStatistics(StatisticsType.UP_DOWN, updnStats.getStatistics());
            statisticsStorage.addStatistics(StatisticsType.NON_D_E, noStats.getStatistics());

            // Set indexes for experiments and attributes
            statisticsStorage.setExperimentIndex(experimentIndex);
            statisticsStorage.setAttributeIndex(attributeIndex);

            // Load efo index
            EfoIndex efoIndex = loadEfoMapping(attributeIndex, experimentIndex);
            statisticsStorage.setEfoIndex(efoIndex);

            // Pre-compute scores for all genes across all efo's. These scores are used to score and then sort
            // genes in user queries with no efv/efo conditions specified.
            statisticsStorage.computeScoresAcrossAllEfos();
        } catch (InterruptedException e) {
            getLog().error("Indexing interrupted!", e);
        } catch (ExecutionException e) {
            getLog().error("Error in indexing", e);
        }

        return statisticsStorage;
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
