package uk.ac.ebi.gxa.index.builder.service;

import ucar.ma2.ArrayFloat;
import uk.ac.ebi.gxa.index.builder.IndexAllCommand;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.index.builder.UpdateIndexForExperimentCommand;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.statistics.*;
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

    private static final String EF_EFV_SEP = "_";
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
        statistics = bitIndexNetCDFs(progressUpdater, atlasProperties.getGeneAtlasIndexBuilderNumberOfThreads() , 500);
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
     * @param fnoth how many threads to parallelise thsi tas over
     * @param progressLogFreq how often this operation should be logged (i.e. every progressLogFreq ncfds processed)
     * @return StatisticsStorage containing statistics for all statistics types in StatisticsType enum - collected over all Atlas ncdfs
     */
    private StatisticsStorage bitIndexNetCDFs(
            final ProgressUpdater progressUpdater,
            final Integer fnoth,
            final Integer progressLogFreq) {
        StatisticsStorage statisticsStorage = new StatisticsStorage();

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

                        Set<Integer> efAttrIndexes = new HashSet<Integer>();
                        for (int j = 0; j < uefvs.length; j++) {
                            String[] efefv = uefvs[j].split(NCDF_EF_EFV_SEP);
                            Integer efvAttributeIndex = attributeIndex.addObject(new Attribute(uefvs[j].replaceAll(NCDF_EF_EFV_SEP, EF_EFV_SEP)));
                            Integer efAttributeIndex = attributeIndex.addObject( new Attribute(efefv[0]));
                            efAttrIndexes.add(efAttributeIndex);

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

                            upStats.addStatistics(efvAttributeIndex, expIdx, upGeneIndexes);
                            dnStats.addStatistics(efvAttributeIndex, expIdx, dnGeneIndexes);
                            updnStats.addStatistics(efvAttributeIndex, expIdx, upGeneIndexes);
                            updnStats.addStatistics(efvAttributeIndex, expIdx, dnGeneIndexes);
                            noStats.addStatistics(efvAttributeIndex, expIdx, noGeneIndexes);

                            upStats.addStatistics(efAttributeIndex, expIdx, upGeneIndexes);
                            dnStats.addStatistics(efAttributeIndex, expIdx, dnGeneIndexes);
                            updnStats.addStatistics(efAttributeIndex, expIdx, upGeneIndexes);
                            updnStats.addStatistics(efAttributeIndex, expIdx, dnGeneIndexes);
                            noStats.addStatistics(efAttributeIndex, expIdx, noGeneIndexes);
                        }

                        // Add data count for aggregations of efvs also (ef, efo)
                        for (Integer efAttrIndex : efAttrIndexes) {
                            car += upStats.getNumStatistics(efAttrIndex, expIdx);
                            car += dnStats.getNumStatistics(efAttrIndex, expIdx);
                            car += updnStats.getNumStatistics(efAttrIndex, expIdx);
                            car += noStats.getNumStatistics(efAttrIndex, expIdx);
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

            EfoIndex efoIndex = loadEfoMapping(attributeIndex, experimentIndex);
            statisticsStorage.setEfoIndex(efoIndex);

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

        int missingExpsNum = 0, loadedEfos = 0, notLoadedEfos = 0;
        List<OntologyMapping> mappings = getAtlasDAO().getOntologyMappingsByOntology("EFO");
        for (OntologyMapping mapping : mappings) {
            Experiment exp = new Experiment(mapping.getExperimentAccession(), String.valueOf(mapping.getExperimentId()));
            Attribute attr = new Attribute(mapping.getProperty() + EF_EFV_SEP + mapping.getPropertyValue());
            Integer attributeIdx = attributeIndex.getIndexForObject(attr);
            Integer experimentIdx =  experimentIndex.getIndexForObject(exp);
            if (attributeIdx == null) {
                attributeIndex.addObject(attr);
                getLog().debug("BitIndex build: efo term: " + mapping.getOntologyTerm() + " maps to a missing attribute: " + attr + " -> adding it to Attribute Index");
            }
            if (experimentIdx == null) {
                missingExpsNum++;
                getLog().error("BitIndex build: Failed to load efo term: " + mapping.getOntologyTerm() + " because experiment: " + exp + " could not be found in Experiment Index");
                // TODO should RuntimeException be thrown here??
            }
            if (attributeIdx != null && experimentIdx != null) {
                loadedEfos++;
                efoIndex.addMapping(mapping.getOntologyTerm(), attributeIdx, experimentIdx);
                getLog().debug("Adding: " + mapping.getOntologyTerm() + ":" + attr + " (" + attributeIdx + "):" + exp + " (" + experimentIdx + ")");
            } else {
                notLoadedEfos++;
            }
        }
        getLog().info(String.format("Loaded %d ontology mappings (not loaded: %d due to missing %d experiments",
                loadedEfos, notLoadedEfos, missingExpsNum));
        return efoIndex;
    }
}
