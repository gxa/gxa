package uk.ac.ebi.gxa.index.builder.service;

import ucar.ma2.ArrayFloat;
import uk.ac.ebi.gxa.index.builder.IndexAllCommand;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.index.builder.UpdateIndexForExperimentCommand;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.statistics.*;

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
        statistics = bitIndexNetCDFs(progressUpdater, getOntologyMap(), atlasProperties.getGeneAtlasIndexBuilderNumberOfThreads() , 500);
    }

    @Override
    public void processCommand(UpdateIndexForExperimentCommand cmd, IndexBuilderService.ProgressUpdater progressUpdater) throws IndexBuilderException {
        throw new IndexBuilderException("Unsupported Operation - genes bit index can be built only for all experiments");
    }


    @Override
    public void finalizeCommand(IndexAllCommand indexAll, ProgressUpdater progressUpdater) throws IndexBuilderException {
        try {
            indexFile.createNewFile();
            FileOutputStream fout = new FileOutputStream(indexFile);
              ObjectOutputStream oos = new ObjectOutputStream(fout);
              oos.writeObject(statistics);
              oos.close();
              getLog().info("Wrote serialized index successfully to: " + indexFile.getAbsolutePath());
        } catch (IOException ioe) {
            getLog().error("Error when saving serialized index: " + indexFile.getAbsolutePath());
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
     * @param ontomap Map experimentid_ef_efv -> Collection of efo terms
     * @param fnoth how many threads to parallelise thsi tas over
     * @param progressLogFreq how often this operation should be logged (i.e. every progressLogFreq ncfds processed)
     * @return StatisticsStorage containing statistics for all statistics types in StatisticsType enum - collected over all Atlas ncdfs
     */
    private StatisticsStorage bitIndexNetCDFs(
            final ProgressUpdater progressUpdater,
            final Map<String, Collection<String>> ontomap,
            final Integer fnoth,
            final Integer progressLogFreq) {
        StatisticsStorage statisticsStorage = new StatisticsStorage();

        final ObjectIndex<Long> geneIndex = new ObjectIndex<Long>();
        final ObjectIndex<Experiment> experimentIndex = new ObjectIndex<Experiment>();

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

                        Experiment experiment = new Experiment(ncdf.getExperiment(), ncdf.getExperimentId() + "");
                        Integer expIdx = experimentIndex.addObject(experiment);
                        Set<Attribute> efAttrs = new HashSet<Attribute>();
                        Set<Attribute> efoAttrs = new HashSet<Attribute>();


                        for (int j = 0; j < uefvs.length; j++) {
                            String[] efefv = uefvs[j].split("\\|\\|");
                            Attribute efvAttribute = new Attribute(uefvs[j]);
                            Attribute efAttribute = new Attribute(efefv[0]);
                            efAttrs.add(efAttribute);
                            Collection<String> efos =
                                    ontomap.get(ncdf.getExperimentId() + "_" + efefv[0] + (efefv.length > 1 ? "_" + efefv[1] : ""));

                            SortedSet<Integer> upGeneIndexes = new TreeSet<Integer>();
                            SortedSet<Integer> dnGeneIndexes = new TreeSet<Integer>();
                            SortedSet<Integer> noGeneIndexes = new TreeSet<Integer>();

                            for (int i = 0; i < shape[0]; i++) {
                                if (genes[i] == 0) continue;

                                float t = tstat.get(i, j);
                                float p = pvals.get(i, j);

                                Integer idx = geneIndex.getIndexForObject(genes[i]);
                                if (null == idx) continue;

                                if (p > 0.05) {
                                    noGeneIndexes.add(idx);
                                    car++;
                                } else {
                                    if (t > 0) {
                                        upGeneIndexes.add(idx);
                                        car++;
                                    } else {
                                        dnGeneIndexes.add(idx);
                                        car++;
                                    }
                                }
                            }

                            upStats.addStatistics(efvAttribute, expIdx, upGeneIndexes);
                            dnStats.addStatistics(efvAttribute, expIdx, dnGeneIndexes);
                            updnStats.addStatistics(efvAttribute, expIdx, upGeneIndexes);
                            updnStats.addStatistics(efvAttribute, expIdx, dnGeneIndexes);
                            noStats.addStatistics(efvAttribute, expIdx, noGeneIndexes);

                            upStats.addStatistics(efAttribute, expIdx, upGeneIndexes);
                            dnStats.addStatistics(efAttribute, expIdx, dnGeneIndexes);
                            updnStats.addStatistics(efAttribute, expIdx, upGeneIndexes);
                            updnStats.addStatistics(efAttribute, expIdx, dnGeneIndexes);
                            noStats.addStatistics(efAttribute, expIdx, noGeneIndexes);

                            if (efos != null) {
                                for (String efo : efos) {
                                    Attribute efoAttribute = new Attribute(efo);
                                    efoAttrs.add(efoAttribute);
                                    upStats.addStatistics(efoAttribute, expIdx, upGeneIndexes);
                                    dnStats.addStatistics(efoAttribute, expIdx, dnGeneIndexes);
                                    updnStats.addStatistics(efoAttribute, expIdx, upGeneIndexes);
                                    updnStats.addStatistics(efoAttribute, expIdx, dnGeneIndexes);
                                    noStats.addStatistics(efoAttribute, expIdx, noGeneIndexes);
                                }
                            }
                        }

                        // Add data count for aggregations of efvs also (ef, efo)
                        for (Attribute efAttribute : efAttrs) {
                            car += upStats.getNumStatistics(efAttribute, expIdx);
                            car += dnStats.getNumStatistics(efAttribute, expIdx);
                            car += updnStats.getNumStatistics(efAttribute, expIdx);
                            car += noStats.getNumStatistics(efAttribute, expIdx);
                        }
                        for (Attribute efoAttribute : efoAttrs) {
                            car += upStats.getNumStatistics(efoAttribute, expIdx);
                            car += dnStats.getNumStatistics(efoAttribute, expIdx);
                            car += updnStats.getNumStatistics(efoAttribute, expIdx);
                            car += noStats.getNumStatistics(efoAttribute, expIdx);
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

            statisticsStorage.addStatistics(StatisticsType.UP, upStats);
            statisticsStorage.addStatistics(StatisticsType.DOWN, dnStats);
            statisticsStorage.addStatistics(StatisticsType.UPDOWN, updnStats);
            statisticsStorage.addStatistics(StatisticsType.NONDE, noStats);
            statisticsStorage.setGeneIndex(geneIndex);
            statisticsStorage.setExperimentIndex(experimentIndex);

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
}
