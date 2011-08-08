package uk.ac.ebi.gxa.index.builder.service;

import it.uniroma3.mat.extendedset.FastSet;
import ucar.ma2.ArrayFloat;
import uk.ac.ebi.gxa.index.builder.IndexAllCommand;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.index.builder.UpdateIndexForExperimentCommand;
import uk.ac.ebi.gxa.data.AtlasDataException;
import uk.ac.ebi.gxa.data.AtlasDataDAO;
import uk.ac.ebi.gxa.data.ExperimentWithData;
import uk.ac.ebi.gxa.data.KeyValuePair;
import uk.ac.ebi.gxa.statistics.*;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
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
import static java.util.Collections.sort;

/**
 * Class used to build ConciseSet-based gene expression statistics index
 */
public class GeneAtlasBitIndexBuilderService extends IndexBuilderService {
    private AtlasDataDAO atlasDataDAO;
    private final String indexFileName;
    private File atlasIndex;
    private File indexFile = null;

    private StatisticsStorage statistics;

    public void setAtlasDataDAO(AtlasDataDAO atlasDataDAO) {
        this.atlasDataDAO = atlasDataDAO;
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
        statistics = bitIndexExperiments(progressUpdater, 50);
    }

    /**
     * Generates a ConciseSet-based index for all statistics types in StatisticsType enum, across all Atlas data
     *
     * @param progressUpdater
     * @param progressLogFreq how often this operation should be logged (i.e. every progressLogFreq ncfds processed)
     * @return StatisticsStorage containing statistics for all statistics types in StatisticsType enum - collected over all Atlas data
     */
    private StatisticsStorage bitIndexExperiments(
            final ProgressUpdater progressUpdater,
            final Integer progressLogFreq) {
        getAtlasDAO().startSession();
        try {
            return bitIndexExperimentsInSession(progressUpdater, progressLogFreq);
        } finally {
            getAtlasDAO().finishSession();
        }
    }

    private StatisticsStorage bitIndexExperimentsInSession(
            final ProgressUpdater progressUpdater,
            final Integer progressLogFreq) {
        StatisticsStorage statisticsStorage = new StatisticsStorage();

        final ObjectPool<ExperimentInfo> experimentPool = new ObjectPool<ExperimentInfo>();
        final ObjectPool<EfvAttribute> attributePool = new ObjectPool<EfvAttribute>();
        final ObjectPool<String> stringPool = new ObjectPool<String>();

        final ThreadSafeStatisticsBuilder upStats = new ThreadSafeStatisticsBuilder();
        final ThreadSafeStatisticsBuilder dnStats = new ThreadSafeStatisticsBuilder();
        final ThreadSafeStatisticsBuilder updnStats = new ThreadSafeStatisticsBuilder();
        final ThreadSafeStatisticsBuilder noStats = new ThreadSafeStatisticsBuilder();

        final BitIndexTask task = new BitIndexTask(experimentsToProcess());

        getLog().info("Found total experiments to index: " + task.getTotalExperiments());

        final ExecutorService summarizer = Executors.newFixedThreadPool(10);

        for (final Experiment exp : task.getExperiments()) {
            getLog().debug("Processing {}", exp);
            final ExperimentWithData experimentWithData = atlasDataDAO.createExperimentWithData(exp);
            try {
                final ExperimentInfo experimentInfo = experimentPool.intern(new ExperimentInfo(exp.getAccession(), exp.getId()));

                for (ArrayDesign ad : exp.getArrayDesigns()) {
                    // TODO when we switch on inclusion of sc-scv stats in bit index, the call below
                    // TODO should change to experimentWithData.getUniqueValues()
                    final List<KeyValuePair> uVals = experimentWithData.getProxy(ad).getUniqueFactorValues();
                    int car = 0; // count of all Statistics records added for this experiment/array design pair
                
                    if (uVals.size() == 0) {
                        //task.skipEmpty(f);
                        getLog().info("Skipping empty " + exp.getAccession() + "/" + ad.getAccession());
                        continue;
                    }
                
                    final long[] bioEntityIdsArr = experimentWithData.getGenes(ad);
                    final ArrayFloat.D2 tstat = experimentWithData.getProxy(ad).getTStatistics();
                    final ArrayFloat.D2 pvals = experimentWithData.getProxy(ad).getPValues();
                    final int[] shape = tstat.getShape();
                
                    final Map<EfvAttribute, MinPMaxT> efToPTUpDown = new HashMap<EfvAttribute, MinPMaxT>();
                    for (int j = 0; j < uVals.size(); j++) {
                        final KeyValuePair efv = uVals.get(j);
                        final EfvAttribute efvAttribute = attributePool.intern(new EfvAttribute(efv.key, efv.value, null));
                        final EfvAttribute efAttribute = attributePool.intern(new EfvAttribute(efv.key, null));
                
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
                            int bioEntityId = safelyCastToInt(bioEntityIdsArr[i]);
                
                            // in order to create a resource used for unit tests,
                            // use <code>|| (bioEntityId != 516248 && bioEntityId != 838592)</code>
                            // so that you would only index the bio entities used in tests
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
                                ptUp.storeStats(upStats, experimentInfo, efvAttribute);
                                // Store rounded minimum down pVals per gene for ef-efv/sc-scv
                                ptDown.storeStats(dnStats, experimentInfo, efvAttribute);
                                // Store rounded minimum up/down pVals per gene for ef-efv/sc-scv
                                ptUpDown.storeStats(updnStats, experimentInfo, efvAttribute);
                            }
                        });
                
                        // Store stats for ef-efv/sc-scv
                        upStats.addStatistics(efvAttribute, experimentInfo, upBioEntityIds);
                        dnStats.addStatistics(efvAttribute, experimentInfo, dnBioEntityIds);
                        updnStats.addStatistics(efvAttribute, experimentInfo, upBioEntityIds);
                        updnStats.addStatistics(efvAttribute, experimentInfo, dnBioEntityIds);
                        noStats.addStatistics(efvAttribute, experimentInfo, noBioEntityIds);
                
                        // Store stats for ef/sc
                        upStats.addStatistics(efAttribute, experimentInfo, upBioEntityIds);
                        dnStats.addStatistics(efAttribute, experimentInfo, dnBioEntityIds);
                        updnStats.addStatistics(efAttribute, experimentInfo, upBioEntityIds);
                        updnStats.addStatistics(efAttribute, experimentInfo, dnBioEntityIds);
                        noStats.addStatistics(efAttribute, experimentInfo, noBioEntityIds);
                
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
                                entry.getValue().storeStats(updnStats, experimentInfo, entry.getKey());
                            }
                        }
                    });
                
                    task.processedStats(car);
                    if (car == 0) {
                        getLog().debug(exp.getAccession() + "/" + ad.getAccession() + " num uVals : " + uVals.size() + " [" + car + "]");
                    }
                }

                task.done(exp);
                progressUpdater.update(task.progress());
            } catch (AtlasDataException e) {
                throw new IndexBuilderException(e.getMessage(), e);
            } catch (IOException e) {
                throw new IndexBuilderException(e.getMessage(), e);
            } finally {
                experimentWithData.closeAllDataSources();
            }
        }

        try {
            // Load efo index
            EfoIndex efoIndex = loadEfoMapping(attributePool, experimentPool);
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
        } finally {
            updnStats.destroy();
            dnStats.destroy();
            updnStats.destroy();
            noStats.destroy();
        }

        return statisticsStorage;
    }

    private int safelyCastToInt(long l) {
        int i = (int) l;
        if ((long) i != l)
            throw new IndexBuilderException("bioEntityId: " + i + " is too large to be cast to int safely- unable to build bit index");
        return i;
    }

    private String internedCopy(ObjectPool<String> stringPool, String s) {
        // Please keep <code>new String(s)</code> intact - substrings have internal references to the underlying String,
        // hence memory footprint might be much bigger than expected.
        return stringPool.intern(new String(s));
    }

    private List<Experiment> experimentsToProcess() {
        final List<Experiment> experiments = getAtlasDAO().getAllExperiments();
        sort(experiments, new Comparator<Experiment>() {
            @Override
            public int compare(Experiment e1, Experiment e2) {
                // if e1 assays number is greater than e2 assays number
                // then e1 should be processed before e2
                return e2.getAssays().size() - e1.getAssays().size();
            }
        });
        return experiments;
    }

    public String getName() {
        return indexFileName;
    }

    private EfoIndex loadEfoMapping(ObjectPool<EfvAttribute> attributePool, ObjectPool<ExperimentInfo> experimentPool) {
        EfoIndex efoIndex = new EfoIndex();
        getLog().info("Fetching ontology mappings...");

        final Set<String> missingEFOs = new HashSet<String>();
        int completeEfos = 0;

        List<OntologyMapping> mappings = getAtlasDAO().getOntologyMappingsByOntology("EFO");
        for (OntologyMapping mapping : mappings) {
            EfvAttribute attribute = attributePool.intern(new EfvAttribute(mapping.getProperty(), mapping.getPropertyValue(), null));

            ExperimentInfo exp = new ExperimentInfo(mapping.getExperimentAccession(), mapping.getExperimentId());
            ExperimentInfo internedExp = experimentPool.intern(exp);

            if (internedExp != null) {
                efoIndex.addMapping(mapping.getOntologyTerm(), attribute, internedExp);
                getLog().debug("Adding '{}': {} in {} ({})",
                        new Object[]{mapping.getOntologyTerm(), attribute, exp, internedExp});
                completeEfos++;
            } else {
                getLog().error(
                        "BitIndex build: Incomplete load for efo term: '{}' because experiment {} could not be found in Experiment Index",
                        mapping.getOntologyTerm(), exp);
                missingEFOs.add(mapping.getOntologyTerm());
            }
        }
        getLog().info("Loaded {} ontology mappings (Load incomplete for {} due to missing experiments)",
                completeEfos, missingEFOs.size());
        getLog().info("The following {} EFOs have not been mapped: {}", missingEFOs.size(), missingEFOs);
        return efoIndex;
    }

    static class MinPMaxT {
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
                stats.addPvalueTstatRank(efvAttributeIndex,
                        PTRank.of(entry.getValue(), geneToMaxT.get(entry.getKey())),
                        expIdx, entry.getKey());
            }
        }
    }
}
