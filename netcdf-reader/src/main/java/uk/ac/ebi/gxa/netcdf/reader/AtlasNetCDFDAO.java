package uk.ac.ebi.gxa.netcdf.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.ArrayFloat;
import uk.ac.ebi.gxa.statistics.*;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class wraps the functionality of retrieving values across multiple instances of NetCDFProxy
 *
 * @author Rober Petryszak
 * @date 13-Sep-2010
 */
public class AtlasNetCDFDAO {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // Location of the NetCDF proxy files
    private File atlasNetCDFRepo;

    /**
     * @param atlasNetCDFRepo
     */
    public void setAtlasNetCDFRepo(File atlasNetCDFRepo) {
        this.atlasNetCDFRepo = atlasNetCDFRepo;
    }

    public String getAtlasNetCDFRepoPath() {
        return atlasNetCDFRepo.getAbsolutePath();
    }

    /**
     * @param geneIds
     * @param experimentID
     * @param proxy
     * @return geneId -> ef -> efv -> ea of best pValue for this geneid-ef-efv combination
     *         Note that ea contains proxyId and designElement index from which it came, so that
     *         the actual expression values can be easily retrieved later
     * @throws IOException
     */

    public Map<Long, Map<String, Map<String, ExpressionAnalysis>>> getExpressionAnalysesForGeneIds(
            final Set<Long> geneIds,
            final String experimentID,
            NetCDFProxy proxy
    ) throws IOException {

        Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA =
                new HashMap<Long, Map<String, Map<String, ExpressionAnalysis>>>();
        // Find first proxy for experimentID if proxy was not passed in
        if (proxy == null) {
            proxy = new NetCDFProxy(new File(findProxyId(experimentID, null)));
        }
        try {
            // Map gene ids to design element ids in which those genes are present
            Map<Long, List<Integer>> geneIdToDEIndexes =
                    getGeneIdToDesignElementIndexes(proxy, geneIds);
            geneIdsToEfToEfvToEA = proxy.getExpressionAnalysesForDesignElementIndexes(geneIdToDEIndexes);
        } finally {
            if (proxy != null) {
                proxy.close();
            }
        }
        return geneIdsToEfToEfvToEA;
    }

    /**
     * @param proxyId
     * @param designElementIndex
     * @return List of expression values retrieved from designElementIndex in proxyId
     * @throws IOException
     */
    public List<Float> getExpressionData(final String proxyId, final Integer designElementIndex)
            throws IOException {
        NetCDFProxy proxy = getNetCDFProxy(proxyId);
        float[] expressionDataArr = null;
        try {
            expressionDataArr = proxy.getExpressionDataForDesignElementAtIndex(designElementIndex);
        } finally {
            proxy.close();
        }
        List<Float> expressionData = new ArrayList<Float>();
        for (int i = 0; i < expressionDataArr.length; i++) {
            expressionData.add(expressionDataArr[i]);
        }

        return expressionData;
    }

    /**
     * @param proxyId
     * @return NetCDFProxy for a given proxyId (i.e. proxy file name)
     */
    public NetCDFProxy getNetCDFProxy(String proxyId) {
        assert (atlasNetCDFRepo != null);
        return new NetCDFProxy(new File(atlasNetCDFRepo + File.separator + proxyId));
    }


    /**
     * @param experimentID
     * @param arrayDesignAcc Array Design accession
     * @return if arrayDesignAcc != null, id of first proxy for experimentID, that matches arrayDesignAcc;
     *         otherwise, id of first proxy in the list returned by getNetCDFProxiesForExperiment()
     */
    public String findProxyId(final String experimentID, final String arrayDesignAcc) {
        List<NetCDFProxy> proxies = getNetCDFProxiesForExperiment(experimentID);
        String proxyId = null;
        for (NetCDFProxy proxy : proxies) {
            String adAcc = null;
            try {
                adAcc = proxy.getArrayDesignAccession();
            } catch (IOException ioe) {
                log.error("Failed to retrieve array design accession for a proxy for experiment id: " + experimentID);
            }
            if (proxyId == null && (arrayDesignAcc == null || arrayDesignAcc.equals(adAcc))) {
                proxyId = proxy.getId();
            }
            proxy.close();
        }
        return proxyId;
    }

    /**
     * @param experimentID
     * @return List of NetCDF proxies corresponding to experimentID
     */
    private List<NetCDFProxy> getNetCDFProxiesForExperiment(final String experimentID) {
        List<NetCDFProxy> proxies = new ArrayList<NetCDFProxy>();
        // lookup NetCDFFiles for this experiment
        File[] netCDFs = atlasNetCDFRepo.listFiles(new FilenameFilter() {

            public boolean accept(File file, String name) {
                return name.matches("^" + experimentID + "_[0-9]+(_ratios)?\\.nc$");
            }
        });

        for (File netCDF : netCDFs) {
            proxies.add(getNetCDFProxy(netCDF.getName()));
        }

        return proxies;
    }

    /**
     * @param proxy
     * @return List of geneIds in proxy
     * @throws IOException
     */
    private List<Long> getGeneIds(final NetCDFProxy proxy) throws IOException {
        List<Long> geneIds = new ArrayList<Long>();
        long[] geneIdsForProxy = proxy.getGenes();
        for (int i = 0; i < geneIdsForProxy.length; i++) {
            geneIds.add(geneIdsForProxy[i]);
        }
        return geneIds;
    }

    /**
     * @param proxy
     * @param geneIds
     * @return Map: geneId -> List of design element indexes in proxy
     * @throws IOException
     */
    private Map<Long, List<Integer>> getGeneIdToDesignElementIndexes(
            final NetCDFProxy proxy,
            final Set<Long> geneIds)
            throws IOException {
        // Note that in a given NetCDF proxy more than one geneIndex (==designElementIndex) may correspond to one geneId
        // (i.e. proxy.getGenes() may contain duplicates, whilst proxy.getDesignElements() will not; and
        // proxy.getGenes().size() == proxy.getDesignElements().size())
        Map<Long, List<Integer>> geneIdToDEIndexes = new HashMap<Long, List<Integer>>();

        // Get gene ids present in proxy
        List<Long> geneIdsInProxy = getGeneIds(proxy);

        int deIndex = 0;
        for (Long geneId : geneIdsInProxy) {
            if (geneIds.contains(geneId)) {
                List<Integer> deIndexes = geneIdToDEIndexes.get(geneId);
                if (deIndexes == null) {
                    deIndexes = new ArrayList<Integer>();
                }
                deIndexes.add(deIndex);
                geneIdToDEIndexes.put(geneId, deIndexes);
            }
            deIndex++;
        }
        return geneIdToDEIndexes;
    }

    /**
     * @param proxyId
     * @param geneId
     * @param ef
     * @return Map: efv -> best ExpressionAnalysis for geneid-ef in this proxy
     * @throws IOException
     */
    public Map<String, ExpressionAnalysis> getBestEAsPerEfvInProxy(
            final String proxyId,
            final Long geneId,
            final String ef)
            throws IOException {
        Set<Long> geneIds = new HashSet<Long>();
        geneIds.add(geneId);

        NetCDFProxy proxy = getNetCDFProxy(proxyId);
        try {
            Map<Long, Map<String, Map<String, ExpressionAnalysis>>> geneIdsToEfToEfvToEA;
            Map<Long, List<Integer>> geneIdToDEIndexes = getGeneIdToDesignElementIndexes(proxy, geneIds);
            geneIdsToEfToEfvToEA = proxy.getExpressionAnalysesForDesignElementIndexes(geneIdToDEIndexes);
            return geneIdsToEfToEfvToEA.get(geneId).get(ef);
        } finally {
            if (proxy != null) {
                proxy.close();
            }
        }
    }

    /**
     * Generates a ConciseSet-based index for all statistics types in StatisticsType enum, across all Atlas ncdfs
     *
     * @param ontomap Map experimentid_ef_efv -> Collection of efo terms
     * @param fnoth how many threads to parallelise thsi tas over
     * @param progressLogFreq how often this operation should be logged (i.e. every progressLogFreq ncfds processed)
     * @return StatisticsStorage containing statistics for all statistics types in StatisticsType enum - collected over all Atlas ncdfs
     * @throws InterruptedException
     */
    public StatisticsStorage bitIndexNetCDFs(
            final Map<String, Collection<String>> ontomap,
            final Integer fnoth,
            final Integer progressLogFreq) throws InterruptedException {
        StatisticsStorage statisticsStorage = new StatisticsStorage();

        final ObjectIndex<Long> geneIndex = new ObjectIndex<Long>();
        final ObjectIndex<Experiment> experimentIndex = new ObjectIndex<Experiment>();

        final Statistics upStats = new Statistics();
        final Statistics dnStats = new Statistics();
        final Statistics updnStats = new Statistics();
        final Statistics noStats = new Statistics();


        List<File> ncdfs = Arrays.asList(atlasNetCDFRepo.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (name.endsWith(".nc"));
            }
        }));

        final AtomicInteger totalStatCount = new AtomicInteger();
        final Integer total = ncdfs.size();
        final AtomicInteger processedNcdfsCoCount = new AtomicInteger(0);
        // Count of ncdfs in which no efvs were found
        final AtomicInteger noEfvsNcdfCount = new AtomicInteger(0);

        final long timeStart = System.currentTimeMillis();

        List<Callable<Void>> tasks = new ArrayList<Callable<Void>>(ncdfs.size());
        for (final File nc : ncdfs)
            tasks.add(new Callable<Void>() {
                public Void call() throws IOException {
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
                            log.debug(nc.getName() + " num uefvs : " + uefvs.length + " [" + car + "]");
                        }


                        int processedNow = processedNcdfsCoCount.incrementAndGet();
                        if (processedNow % progressLogFreq == 0 || processedNow == total) {
                            long timeNow = System.currentTimeMillis();
                            long elapsed = timeNow - timeStart;
                            double speed = (processedNow / (elapsed / Double.valueOf(progressLogFreq)));  // (item/s)
                            double estimated = (total - processedNow) / (speed * 60);

                            log.info(
                                    String.format("Processed %d/%d (# ncdfs with no EFVs so far: %d) ncdfs %d%%, %.1f ncdfs/sec overall, estimated %.1f min remaining",
                                            processedNow, total, noEfvsNcdfCount.get(), (processedNow * 100 / total), speed, estimated));

                            if (processedNow == total) {
                                log.info("Overall processing time: " + (elapsed / 60000) + " min");
                            }
                        }
                    } catch (Throwable t) {
                        log.error("Error occurred: ", t);
                    }
                    return null;
                }
            });

        ExecutorService svc = Executors.newFixedThreadPool(fnoth);
        svc.invokeAll(tasks);
        svc.shutdown();

        log.info("Total set " + (totalStatCount.get() * 8L) / 1024 + " kB");

        statisticsStorage.addStatistics(StatisticsType.UP, upStats);
        statisticsStorage.addStatistics(StatisticsType.DOWN, dnStats);
        statisticsStorage.addStatistics(StatisticsType.UPDOWN, updnStats);
        statisticsStorage.addStatistics(StatisticsType.NONDE, noStats);
        statisticsStorage.setGeneIndex(geneIndex);
        statisticsStorage.setExperimentIndex(experimentIndex);


        // Serialize to a byte array TODO testing only - move out of here
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(statisticsStorage);
            out.close();

            // Get the bytes of the serialized object
            byte[] buf = bos.toByteArray();

            log.info("Serialized statisticsStorage size: " + ((long) (buf.length) / 1024) + " kB");

        } catch (IOException ioe) {
            log.error("Problem serializing statisticsStorage: ", ioe);
        }

        return statisticsStorage;
    }
}
