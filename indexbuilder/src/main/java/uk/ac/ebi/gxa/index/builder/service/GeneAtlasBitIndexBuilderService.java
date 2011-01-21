package uk.ac.ebi.gxa.index.builder.service;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import ucar.ma2.ArrayFloat;
import uk.ac.ebi.gxa.index.builder.IndexAllCommand;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.index.builder.UpdateIndexForExperimentCommand;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.statistics.*;
import uk.ac.ebi.gxa.utils.EscapeUtil;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;
import uk.ac.ebi.microarray.atlas.model.OntologyMapping;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: Nov 2, 2010
 * Time: 12:01:22 PM
 * Class used to build ConciseSet-based gene expression statistics index
 */
public class GeneAtlasBitIndexBuilderService extends IndexBuilderService {

    private static final String NCDF_EF_EFV_SEP = "\\|\\|";


    private AtlasProperties atlasProperties;
    private AtlasNetCDFDAO atlasNetCDFDAO;
    private String indexFileName;
    private File atlasIndex;
    File indexFile = null;

    private StatisticsStorage statistics;


    public void setAtlasNetCDFDAO(AtlasNetCDFDAO atlasNetCDFDAO) {
        this.atlasNetCDFDAO = atlasNetCDFDAO;
    }

    public void setAtlasProperties(AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
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
    public void processCommand(IndexAllCommand indexAll, IndexBuilderService.ProgressUpdater progressUpdater) throws IndexBuilderException {
        indexFile = new File(atlasIndex + File.separator + getName());
        if (indexFile.exists()) {
            indexFile.delete();
        }
        statistics = bitIndexNetCDFs(progressUpdater, atlasProperties.getGeneAtlasIndexBuilderNumberOfThreads(), 500);
    }

    @Override
    public void processCommand(UpdateIndexForExperimentCommand cmd, IndexBuilderService.ProgressUpdater progressUpdater) throws IndexBuilderException {
        throw new IndexBuilderException("Unsupported Operation - genes bit index can be built only for all experiments");
    }


    @Override
    public void finalizeCommand() throws IndexBuilderException {
        try {
            indexFile.createNewFile();
            FileOutputStream fout = new FileOutputStream(indexFile);
              ObjectOutputStream oos = new ObjectOutputStream(fout);
              oos.writeObject(statistics);
              oos.close();
              getLog().info("Wrote serialized index successfully to: " + indexFile.getAbsolutePath());
        } catch (IOException ioe) {
            getLog().error("Error when saving serialized index: " + indexFile.getAbsolutePath(), ioe);
        }
    }

    @Override
    public void finalizeCommand(UpdateIndexForExperimentCommand updateIndexForExperimentCommand, ProgressUpdater progressUpdater) throws IndexBuilderException {
        throw new IndexBuilderException("Unsupported Operation - genes bit index can be built only for all experiments");
    }

    /**
     * Generates a ConciseSet-based index for all statistics types in StatisticsType enum, across all Atlas ncdfs
     *
     * @param progressUpdater
     * @param fnoth           how many threads to parallelise thsi tas over
     * @param progressLogFreq how often this operation should be logged (i.e. every progressLogFreq ncfds processed)
     * @return StatisticsStorage containing statistics for all statistics types in StatisticsType enum - collected over all Atlas ncdfs
     */
    private StatisticsStorage bitIndexNetCDFs(
            final ProgressUpdater progressUpdater,
            final Integer fnoth,
            final Integer progressLogFreq) {
        StatisticsStorage statisticsStorage = new StatisticsStorage<Long>();

        final ObjectIndex<Long> geneIndex = new ObjectIndex<Long>();
        final ObjectIndex<Experiment> experimentIndex = new ObjectIndex<Experiment>();
        final ObjectIndex<Attribute> attributeIndex = new ObjectIndex<Attribute>();

        final Statistics upStats = new Statistics();
        final Statistics dnStats = new Statistics();
        final Statistics updnStats = new Statistics();
        final Statistics noStats = new Statistics();

        List<File> ncdfs = atlasNetCDFDAO.getAllNcdfs();

        final AtomicInteger totalStatCount = new AtomicInteger();
        final Integer total = ncdfs.size();
        getLog().info("Found total ncdfs to index: " + total);

        final AtomicInteger processedNcdfsCoCount = new AtomicInteger(0);
        // Count of ncdfs in which no efvs were found
        final AtomicInteger noEfvsNcdfCount = new AtomicInteger(0);

        final long timeStart = System.currentTimeMillis();

        List<Callable<Boolean>> tasks = new ArrayList<Callable<Boolean>>(total);
        for (final File nc : ncdfs)
            tasks.add(new Callable<Boolean>() {
                public Boolean call() throws IOException {
                    try {
                        NetCDFProxy ncdf = new NetCDFProxy(nc);

                        Experiment experiment = new Experiment(ncdf.getExperiment(), ncdf.getExperimentId() + "");
                        Integer expIdx = experimentIndex.addObject(experiment);

                        String[] uefvs = ncdf.getUniqueFactorValues();
                        int car = 0; // count of all Statistics records added for this ncdf

                        if (uefvs.length == 0) {
                            ncdf.close();
                            processedNcdfsCoCount.incrementAndGet();
                            noEfvsNcdfCount.incrementAndGet();
                            return null;
                        }


                        long[] genes = ncdf.getGenes();
                        for (long gene : genes) if (gene != 0) geneIndex.addObject(gene);

                        ArrayFloat.D2 tstat = ncdf.getTStatistics();
                        ArrayFloat.D2 pvals = ncdf.getPValues();
                        int[] shape = tstat.getShape();

                        for (int j = 0; j < uefvs.length; j++) {
                            String[] arr = uefvs[j].split(NCDF_EF_EFV_SEP);
                            String ef = arr[0];
                            String efv = arr.length == 1 ? "" : arr[1];

                            Integer efvAttributeIndex = attributeIndex.addObject(new Attribute(ef, efv));

                            SortedSet<Integer> upGeneIndexes = new TreeSet<Integer>();
                            SortedSet<Integer> dnGeneIndexes = new TreeSet<Integer>();
                            SortedSet<Integer> noGeneIndexes = new TreeSet<Integer>();

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
                                    } else {
                                        dnGeneIndexes.add(idx);
                                        car++;
                                    }
                                }
                            }

                            upStats.addStatistics(efvAttributeIndex, expIdx, upGeneIndexes);
                            dnStats.addStatistics(efvAttributeIndex, expIdx, dnGeneIndexes);
                            updnStats.addStatistics(efvAttributeIndex, expIdx, upGeneIndexes);
                            updnStats.addStatistics(efvAttributeIndex, expIdx, dnGeneIndexes);
                            noStats.addStatistics(efvAttributeIndex, expIdx, noGeneIndexes);
                        }

                        tstat = null;
                        pvals = null;
                        ncdf.close();

                        totalStatCount.addAndGet(car);
                        if (car == 0) {
                            getLog().debug(nc.getName() + " num uefvs : " + uefvs.length + " [" + car + "]");
                        }


                        int processedNow = processedNcdfsCoCount.incrementAndGet();
                        if (processedNow % progressLogFreq == 0 || processedNow == total) {
                            long timeNow = System.currentTimeMillis();
                            long elapsed = timeNow - timeStart;
                            double speed = (processedNow / (elapsed / Double.valueOf(progressLogFreq)));  // (item/s)
                            double estimated = (total - processedNow) / (speed * 60);

                            getLog().info(
                                    String.format("Processed %d/%d (# ncdfs with no EFVs so far: %d) ncdfs %d%%, %.1f ncdfs/sec overall, estimated %.1f min remaining",
                                            processedNow, total, noEfvsNcdfCount.get(), (processedNow * 100 / total), speed, estimated));

                            if (processedNow == total) {
                                getLog().info("Overall processing time: " + (elapsed / 60000) + " min");
                            }

                            progressUpdater.update(processedNow + "/" + total);
                        }

                        return true;
                    } catch (Throwable t) {
                        getLog().error("Error occurred: ", t);
                    }
                    return false;
                }
            });


        ExecutorService svc = Executors.newFixedThreadPool(fnoth);
        try {
            svc.invokeAll(tasks);

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
        } finally {
            // shutdown the service
            getLog().info("Gene statistics index building tasks finished, cleaning up resources and exiting");
            svc.shutdown();
        }

        return statisticsStorage;
    }

    public String getName() {
        return indexFileName;
    }

    private EfoIndex loadEfoMapping(ObjectIndex<Attribute> attributeIndex, ObjectIndex<Experiment> experimentIndex) {

        EfoIndex efoIndex = new EfoIndex();
        getLog().info("Fetching ontology mappings...");

        Set<String> allEfos = new HashSet<String>();
        int missingExpsNum = 0, missingAttrsNum = 0, LoadedCompleteEfos = 0, LoadedInCompleteEfos = 0;
        List<OntologyMapping> mappings = getAtlasDAO().getOntologyMappingsByOntology("EFO");
        for (OntologyMapping mapping : mappings) {
            Experiment exp = new Experiment(mapping.getExperimentAccession(), String.valueOf(mapping.getExperimentId()));
            Attribute attr = new Attribute(mapping.getProperty(), mapping.getPropertyValue());
            Integer attributeIdx = attributeIndex.getIndexForObject(attr);
            Integer experimentIdx = experimentIndex.getIndexForObject(exp);
            if (attributeIdx == null) {
                attributeIndex.addObject(attr);
                getLog().debug("BitIndex build: efo term: " + mapping.getOntologyTerm() + " maps to a missing attribute: " + attr + " -> adding it to Attribute Index");
            }
            if (experimentIdx == null) {
                missingExpsNum++;
                getLog().error("BitIndex build: Incomplete load for efo term: " + mapping.getOntologyTerm() + " because experiment: " + exp + " could not be found in Experiment Index");
            }
            if (attributeIdx == null) {
                missingAttrsNum++;
                getLog().error("BitIndex build: Incomplete load for efo term: " + mapping.getOntologyTerm() + " because attribute: " + attr + " could not be found in Attribute Index");
            }

            if (attributeIdx != null && experimentIdx != null) {
                LoadedCompleteEfos++;
                efoIndex.addMapping(mapping.getOntologyTerm(), attributeIdx, experimentIdx);
                getLog().debug("Adding: " + mapping.getOntologyTerm() + ":" + attr + " (" + attributeIdx + "):" + exp + " (" + experimentIdx + ")");
            } else {
                LoadedInCompleteEfos++;
            }
            allEfos.add(mapping.getOntologyTerm());
        }
        getLog().info(String.format("Loaded %d ontology mappings (Load incomplete for %d due to missing %d experiments or missing %d attributes",
                LoadedCompleteEfos, LoadedInCompleteEfos, missingExpsNum, missingAttrsNum));

        allEfos.removeAll(efoIndex.getEfos());
        getLog().info("The following " + allEfos.size() + " efo's have not been loaded at all:" + allEfos);
        return efoIndex;
    }

    /**
     * Populated all statistics in statisticsStorage with pre-computed scores for all genes across all efo's. These scores
     * are used in user queries containing no efv/efo conditions.
     * @param statisticsStorage
     */
    private void computeScoresAcrossAllEfos(StatisticsStorage<Integer> statisticsStorage) {
        // Pre-computing UP stats scores for all genes across all efo's
        getLog().info("Pre-computing scores across all efo mappings for statistics: " + StatisticsType.UP + "...");
        long start = System.currentTimeMillis();
        Multiset<Integer> upCounts = StatisticsQueryUtils.getScoresAcrossAllEfos(StatisticsType.UP, statisticsStorage);
        statisticsStorage.setScoresAcrossAllEfos(upCounts, StatisticsType.UP);
        getLog().info("Pre-computed scores across all efo mappings for statistics: " + StatisticsType.UP + " in " + (System.currentTimeMillis() - start) + " ms");

        // Pre-computing DOWN stats scores for all genes across all efo's
        getLog().info("Pre-computing scores across all efo mappings for statistics: " + StatisticsType.DOWN + "...");
        start = System.currentTimeMillis();
        Multiset<Integer> dnCounts = StatisticsQueryUtils.getScoresAcrossAllEfos(StatisticsType.DOWN, statisticsStorage);
        statisticsStorage.setScoresAcrossAllEfos(dnCounts, StatisticsType.DOWN);
        getLog().info("Pre-computed scores across all efo mappings for statistics: " + StatisticsType.DOWN + " in " + (System.currentTimeMillis() - start) + " ms");

        // Pre-computing UP_DOWN stats scores for all genes across all efo's
        getLog().info("Pre-computing scores across all efo mappings for statistics: " + StatisticsType.UP_DOWN + "...");
        start = System.currentTimeMillis();
        Multiset<Integer> upDnCounts = HashMultiset.create();
        upDnCounts.addAll(upCounts);
        upDnCounts.addAll(dnCounts);
        statisticsStorage.setScoresAcrossAllEfos(upDnCounts, StatisticsType.UP_DOWN);
        getLog().info("Pre-computed scores across all efo mappings for statistics: " + StatisticsType.UP_DOWN + " in " + (System.currentTimeMillis() - start) + " ms");

        // Pre-computing NON_D_E stats scores for all genes across all efo's
        getLog().info("Pre-computing scores across all efo mappings for statistics: " + StatisticsType.NON_D_E + "...");
        start = System.currentTimeMillis();
        Multiset<Integer> nonDECounts = StatisticsQueryUtils.getScoresAcrossAllEfos(StatisticsType.NON_D_E, statisticsStorage);
        statisticsStorage.setScoresAcrossAllEfos(nonDECounts, StatisticsType.NON_D_E);
        getLog().info("Pre-computed scores across all efo mappings for statistics: " + StatisticsType.NON_D_E + " in " + (System.currentTimeMillis() - start) + " ms");
    }
}
