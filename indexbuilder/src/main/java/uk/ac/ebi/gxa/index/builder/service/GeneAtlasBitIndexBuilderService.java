package uk.ac.ebi.gxa.index.builder.service;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import ucar.ma2.ArrayFloat;
import uk.ac.ebi.gxa.index.builder.IndexAllCommand;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.index.builder.UpdateIndexForExperimentCommand;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.statistics.*;
import uk.ac.ebi.microarray.atlas.model.*;
import uk.ac.ebi.gxa.Experiment;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.io.Closeables.closeQuietly;
import static java.lang.Math.round;

/**
 * Class used to build ConciseSet-based gene expression statistics index
 */
public class GeneAtlasBitIndexBuilderService extends IndexBuilderService {
    private static final float PRECISION = 1e-3F;

    private AtlasNetCDFDAO atlasNetCDFDAO;
    private final String indexFileName;
    private File atlasIndex;
    private File indexFile = null;

    private StatisticsStorage<Long> statistics;
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
        indexFile = new File(atlasIndex, getName());
        if (indexFile.exists() && !indexFile.delete()) {
            throw new IndexBuilderException("Cannot delete " + indexFile.getAbsolutePath());
        }
        statistics = bitIndexNetCDFs(progressUpdater, 200);
    }

    @Override
    public void processCommand(UpdateIndexForExperimentCommand cmd,
                               IndexBuilderService.ProgressUpdater progressUpdater) throws IndexBuilderException {
        /// Re-build the whole bit index even if one experiment only is being updated
        processCommand(new IndexAllCommand(), progressUpdater);
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

    /**
     * Generates a ConciseSet-based index for all statistics types in StatisticsType enum, across all Atlas ncdfs
     *
     * @param progressUpdater
     * @param progressLogFreq how often this operation should be logged (i.e. every progressLogFreq ncfds processed)
     * @return StatisticsStorage containing statistics for all statistics types in StatisticsType enum - collected over all Atlas ncdfs
     */
    private StatisticsStorage<Long> bitIndexNetCDFs(
            final ProgressUpdater progressUpdater,
            final Integer progressLogFreq) {
        StatisticsStorage<Long> statisticsStorage = new StatisticsStorage<Long>();

        final ObjectIndex<Long> geneIndex = new ObjectIndex<Long>();
        final ObjectIndex<ExperimentInfo> experimentIndex = new ObjectIndex<ExperimentInfo>();
        final ObjectIndex<EfvAttribute> attributeIndex = new ObjectIndex<EfvAttribute>();

        final Statistics upStats = new Statistics();
        final Statistics dnStats = new Statistics();
        final Statistics updnStats = new Statistics();
        final Statistics noStats = new Statistics();

        List<File> ncdfs = atlasNetCDFDAO.getAllNcdfs();

        final AtomicInteger totalStatCount = new AtomicInteger();
        final Integer total = ncdfs.size();
        getLog().info("Found total ncdfs to index: " + total);

        // fetch experiments - we want to include public experiments only in the index
        final Collection<Long> publicExperimentIds = Collections2.transform(
                getAtlasModel().getPublicExperiments()
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
                        } else if (!publicExperimentIds.contains(ncdf.getExperimentId())) {
                            processedNcdfsCount.incrementAndGet();
                            getLog().info("Excluding from index private experiment: " + ncdf.getExperimentAccession());
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
                            return null;
                        }


                        long[] genes = ncdf.getGenes();
                        for (long gene : genes) if (gene != 0) geneIndex.addObject(gene);

                        ArrayFloat.D2 tstat = ncdf.getTStatistics();
                        ArrayFloat.D2 pvals = ncdf.getPValues();
                        int[] shape = tstat.getShape();

                        Map<Integer, Map<Integer, Float>> efToGeneToMinUpDownPValue = new HashMap<Integer, Map<Integer, Float>>();
                        Map<Integer, Map<Integer, Float>> efToGeneToMaxUpDownTStat = new HashMap<Integer, Map<Integer, Float>>();
                        for (int j = 0; j < uVals.size(); j++) {
                            String[] arr = uVals.get(j).split(NetCDFProxy.NCDF_PROP_VAL_SEP_REGEX);
                            String ef = arr[0];
                            String efv = arr.length == 1 ? "" : arr[1];

                            Integer efvAttributeIndex = attributeIndex.addObject(new EfvAttribute(ef, efv, null));
                            Integer efAttributeIndex = attributeIndex.addObject(new EfvAttribute(ef, null));

                            SortedSet<Integer> upGeneIndexes = new TreeSet<Integer>();
                            SortedSet<Integer> dnGeneIndexes = new TreeSet<Integer>();
                            SortedSet<Integer> noGeneIndexes = new TreeSet<Integer>();

                            // Initialise if necessary pval/tstat storage for ef
                            if (!efToGeneToMinUpDownPValue.containsKey(efAttributeIndex)) {
                                efToGeneToMinUpDownPValue.put(efAttributeIndex, new HashMap<Integer, Float>());
                            }
                            if (!efToGeneToMaxUpDownTStat.containsKey(efAttributeIndex)) {
                                efToGeneToMaxUpDownTStat.put(efAttributeIndex, new HashMap<Integer, Float>());
                            }
                            // Initialise pval/tstat storage for ef-efv/sc-scv
                            Map<Integer, Float> geneToMinUpDownPValue = new HashMap<Integer, Float>();
                            Map<Integer, Float> geneToMaxUpDownTStat = new HashMap<Integer, Float>();
                            Map<Integer, Float> geneToMinUpPValue = new HashMap<Integer, Float>();
                            Map<Integer, Float> geneToMaxUpTStat = new HashMap<Integer, Float>();
                            Map<Integer, Float> geneToMinDownPValue = new HashMap<Integer, Float>();
                            Map<Integer, Float> geneToMaxDownTStat = new HashMap<Integer, Float>();

                            for (int i = 0; i < shape[0]; i++) {
                                if (genes[i] == 0) continue;

                                float t = tstat.get(i, j);
                                float p = pvals.get(i, j);

                                Integer idx = geneIndex.getIndexForObject(genes[i]);
                                if (null == idx) continue;

                                if (ExpressionAnalysis.isNo(p, t)) {
                                    noGeneIndexes.add(idx);
                                    car++;
                                } else {
                                    if (ExpressionAnalysis.isUp(p, t)) {
                                        upGeneIndexes.add(idx);
                                        car++;
                                        // Store if the lowest pVal/highest absolute value of tStat for ef-efv (up)
                                        if (geneToMaxUpTStat.get(idx) == null ||
                                                Math.abs((int) t) > Math.abs(geneToMaxUpTStat.get(idx)) ||
                                                (Math.abs((int) t) == Math.abs(geneToMaxUpTStat.get(idx)) &&
                                                        p < geneToMinUpPValue.get(idx))) {
                                            geneToMinUpPValue.put(idx, p);
                                            geneToMaxUpTStat.put(idx, t);
                                        }

                                    } else {
                                        dnGeneIndexes.add(idx);
                                        car++;

                                        // Store if the lowest pVal/highest absolute value of tStat for ef-efv/sc-scv (down)
                                        if (geneToMaxDownTStat.get(idx) == null ||
                                                Math.abs((int) t) > Math.abs(geneToMaxDownTStat.get(idx)) ||
                                                (Math.abs((int) t) == Math.abs(geneToMaxDownTStat.get(idx)) &&
                                                        p < geneToMinDownPValue.get(idx))) {
                                            geneToMinDownPValue.put(idx, p);
                                            geneToMaxDownTStat.put(idx, t);
                                        }
                                    }
                                    // Store if the lowest pVal/highest absolute value of tStat for ef-efv/sc-scv (up/down)
                                    if (geneToMaxUpDownTStat.get(idx) == null ||
                                            Math.abs((int) t) > Math.abs(geneToMaxUpDownTStat.get(idx)) ||
                                            (Math.abs((int) t) == Math.abs(geneToMaxUpDownTStat.get(idx)) &&
                                                    p < geneToMinUpDownPValue.get(idx))) {
                                        geneToMinUpDownPValue.put(idx, p);
                                        geneToMaxUpDownTStat.put(idx, t);
                                    }

                                    // Store if the lowest pVal/highest absolute value of tStat for ef/sc  (up/down)
                                    if (efToGeneToMaxUpDownTStat.get(efAttributeIndex).get(idx) == null ||
                                            Math.abs((int) t) > Math.abs(
                                                    efToGeneToMaxUpDownTStat.get(efAttributeIndex).get(idx)) ||

                                            (Math.abs((int) t) == Math.abs(
                                                    efToGeneToMaxUpDownTStat.get(efAttributeIndex).get(idx)) &&
                                                    p < efToGeneToMinUpDownPValue.get(efAttributeIndex).get(idx))) {
                                        efToGeneToMinUpDownPValue.get(efAttributeIndex).put(idx, p);
                                        efToGeneToMaxUpDownTStat.get(efAttributeIndex).put(idx, t);
                                    }
                                }
                            }

                            // Store rounded minimum up pVals per gene for ef-efv/sc-scv
                            for (Map.Entry<Integer, Float> entry : geneToMinUpPValue.entrySet()) {
                                Short tStatRank = StatisticsQueryUtils.getTStatRank(
                                        geneToMaxUpTStat.get(entry.getKey()));
                                // Store min uppVal for efv
                                upStats.addPvalueTstatRank(efvAttributeIndex, roundToDesiredPrecision(entry.getValue()), tStatRank, expIdx,
                                        entry.getKey());
                            }

                            // Store rounded minimum down pVals per gene for ef-efv/sc-scv
                            for (Map.Entry<Integer, Float> entry : geneToMinDownPValue.entrySet()) {
                                Short tStatRank = StatisticsQueryUtils.getTStatRank(
                                        geneToMaxDownTStat.get(entry.getKey()));
                                // Store min down pVal for efv
                                dnStats.addPvalueTstatRank(efvAttributeIndex, roundToDesiredPrecision(entry.getValue()), tStatRank, expIdx,
                                        entry.getKey());
                            }


                            // Store rounded minimum up/down pVals per gene for ef-efv/sc-scv
                            for (Map.Entry<Integer, Float> entry : geneToMinUpDownPValue.entrySet()) {
                                Short tStatRank = StatisticsQueryUtils.getTStatRank(
                                        geneToMaxUpDownTStat.get(entry.getKey()));
                                // Store min up/down pVal for efv
                                updnStats.addPvalueTstatRank(efvAttributeIndex, roundToDesiredPrecision(entry.getValue()), tStatRank, expIdx,
                                        entry.getKey());
                            }

                            // Store stats for ef-efv/sc-scv
                            upStats.addStatistics(efvAttributeIndex, expIdx, upGeneIndexes);
                            dnStats.addStatistics(efvAttributeIndex, expIdx, dnGeneIndexes);
                            updnStats.addStatistics(efvAttributeIndex, expIdx, upGeneIndexes);
                            updnStats.addStatistics(efvAttributeIndex, expIdx, dnGeneIndexes);
                            noStats.addStatistics(efvAttributeIndex, expIdx, noGeneIndexes);

                            // Store stats for ef/sc
                            upStats.addStatistics(efAttributeIndex, expIdx, upGeneIndexes);
                            dnStats.addStatistics(efAttributeIndex, expIdx, dnGeneIndexes);
                            updnStats.addStatistics(efAttributeIndex, expIdx, upGeneIndexes);
                            updnStats.addStatistics(efAttributeIndex, expIdx, dnGeneIndexes);
                            noStats.addStatistics(efAttributeIndex, expIdx, noGeneIndexes);

                            // Add genes for ef/sc attributes across all experiments
                            updnStats.addGenesForEfAttribute(efAttributeIndex, upGeneIndexes);
                            updnStats.addGenesForEfAttribute(efAttributeIndex, dnGeneIndexes);

                            // Add genes for ef-efv/sc-scv attributes across all experiments
                            updnStats.addGenesForEfvAttribute(efvAttributeIndex, upGeneIndexes);
                            updnStats.addGenesForEfvAttribute(efvAttributeIndex, dnGeneIndexes);
                        }

                        // Store rounded minimum up/down pVals per gene for all efs/scs
                        for (Map.Entry<Integer, Map<Integer, Float>> entry : efToGeneToMinUpDownPValue.entrySet()) {
                            Map<Integer, Float> geneToMinUpDownPValue = entry.getValue();
                            Map<Integer, Float> geneToMaxTStat = efToGeneToMaxUpDownTStat.get(entry.getKey());
                            for (Map.Entry<Integer, Float> geneEntry : geneToMinUpDownPValue.entrySet()) {
                                Short tStatRank = StatisticsQueryUtils.getTStatRank(
                                        geneToMaxTStat.get(geneEntry.getKey()));
                                // Store min pVal for ef
                                updnStats.addPvalueTstatRank(entry.getKey(), roundToDesiredPrecision(geneEntry.getValue()), tStatRank, expIdx,
                                        geneEntry.getKey());
                            }
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
                        getLog().error("Error occurred: ", t);
                    } finally {
                        closeQuietly(ncdf);
                    }
                    return false;
                }
            });


        try {
            executor.invokeAll(tasks);

            getLog().info("Total statistics data set " + (totalStatCount.get() * 8L) / 1024 + " kB");

            // Set statistics
            statisticsStorage.addStatistics(StatisticsType.UP, upStats);
            statisticsStorage.addStatistics(StatisticsType.DOWN, dnStats);
            statisticsStorage.addStatistics(StatisticsType.UP_DOWN, updnStats);
            statisticsStorage.addStatistics(StatisticsType.NON_D_E, noStats);

            // Set indexes for genes, experiments and indexes
            statisticsStorage.setGeneIndex(geneIndex);
            statisticsStorage.setExperimentIndex(experimentIndex);
            statisticsStorage.setAttributeIndex(attributeIndex);

            // Load efo index
            EfoIndex efoIndex = loadEfoMapping(attributeIndex, experimentIndex);
            statisticsStorage.setEfoIndex(efoIndex);

            // Pre-compute scores for all genes across all efo's. These scores are used to score and then sort
            // genes in user queries with no efv/efo conditions specified.
            computeScoresAcrossAllEfos(statisticsStorage);

        } catch (InterruptedException e) {
            getLog().error("Indexing interrupted!", e);
        }

        return statisticsStorage;
    }

    private Float roundToDesiredPrecision(float value) {
        return round(value / PRECISION) * PRECISION;
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

    /**
     * Populated all statistics in statisticsStorage with pre-computed scores for all genes across all efo's. These scores
     * are used in user queries containing no efv/efo conditions.
     *
     * @param statisticsStorage
     */
    private void computeScoresAcrossAllEfos(StatisticsStorage<Long> statisticsStorage) {
        // Pre-computing UP stats scores for all genes across all efo's
        getLog().info("Pre-computing scores across all efo mappings for statistics: " + StatisticsType.UP + "...");
        long start = System.currentTimeMillis();
        Multiset<Integer> upCounts = StatisticsQueryUtils.getScoresAcrossAllEfos(StatisticsType.UP, statisticsStorage);
        statisticsStorage.setScoresAcrossAllEfos(upCounts, StatisticsType.UP);
        getLog().info(
                "Pre-computed scores across all efo mappings for statistics: " + StatisticsType.UP + " in " + (System.currentTimeMillis() - start) + " ms");

        // Pre-computing DOWN stats scores for all genes across all efo's
        getLog().info("Pre-computing scores across all efo mappings for statistics: " + StatisticsType.DOWN + "...");
        start = System.currentTimeMillis();
        Multiset<Integer> dnCounts = StatisticsQueryUtils.getScoresAcrossAllEfos(StatisticsType.DOWN,
                statisticsStorage);
        statisticsStorage.setScoresAcrossAllEfos(dnCounts, StatisticsType.DOWN);
        getLog().info(
                "Pre-computed scores across all efo mappings for statistics: " + StatisticsType.DOWN + " in " + (System.currentTimeMillis() - start) + " ms");

        // Pre-computing UP_DOWN stats scores for all genes across all efo's
        getLog().info("Pre-computing scores across all efo mappings for statistics: " + StatisticsType.UP_DOWN + "...");
        start = System.currentTimeMillis();
        Multiset<Integer> upDnCounts = HashMultiset.create();
        upDnCounts.addAll(upCounts);
        upDnCounts.addAll(dnCounts);
        statisticsStorage.setScoresAcrossAllEfos(upDnCounts, StatisticsType.UP_DOWN);
        getLog().info(
                "Pre-computed scores across all efo mappings for statistics: " + StatisticsType.UP_DOWN + " in " + (System.currentTimeMillis() - start) + " ms");

        // Pre-computing NON_D_E stats scores for all genes across all efo's
        getLog().info("Pre-computing scores across all efo mappings for statistics: " + StatisticsType.NON_D_E + "...");
        start = System.currentTimeMillis();
        Multiset<Integer> nonDECounts = StatisticsQueryUtils.getScoresAcrossAllEfos(StatisticsType.NON_D_E,
                statisticsStorage);
        statisticsStorage.setScoresAcrossAllEfos(nonDECounts, StatisticsType.NON_D_E);
        getLog().info(
                "Pre-computed scores across all efo mappings for statistics: " + StatisticsType.NON_D_E + " in " + (System.currentTimeMillis() - start) + " ms");
    }
}
