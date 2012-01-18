package uk.ac.ebi.gxa.index.builder.service;

import com.google.common.base.Predicate;
import it.uniroma3.mat.extendedset.FastSet;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.data.*;
import uk.ac.ebi.gxa.index.builder.IndexAllCommand;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.statistics.*;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.OntologyMapping;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Predicates.*;
import static com.google.common.io.Closeables.closeQuietly;
import static java.util.Arrays.asList;
import static java.util.Collections.sort;

/**
 * Class used to build ConciseSet-based gene expression statistics index
 */
public class GeneAtlasBitIndexBuilderService extends IndexBuilderService {
    /**
     * Filter for bioentities to index.
     * <p/>
     * In order to build test index, use {@link #TEST_BIOENTITY_FILTER} instead
     */
    private static final Predicate<Long> BIOENTITY_FILTER = not(equalTo(0L));


    /**
     * Filter for bioentities used in the tests
     * <p/>
     * For production code, use {@link #BIOENTITY_FILTER} instead
     */
    @SuppressWarnings("unused")
    private static final Predicate<Long> TEST_BIOENTITY_FILTER = in(asList(516248L, 838592L));

    private AtlasDataDAO atlasDataDAO;
    private String indexFileName;
    private File atlasIndex;
    private File indexFile = null;

    private StatisticsStorage statistics;

    public void setAtlasDataDAO(AtlasDataDAO atlasDataDAO) {
        this.atlasDataDAO = atlasDataDAO;
    }

    public void setAtlasIndex(File atlasIndex) {
        this.atlasIndex = atlasIndex;
    }

    public void setIndexFileName(String indexFileName) {
        this.indexFileName = indexFileName;
    }


    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processCommand(IndexAllCommand indexAll,
                               IndexBuilderService.ProgressUpdater progressUpdater) throws IndexBuilderException {
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

    private void indexAll(ProgressUpdater progressUpdater) {
        indexFile = new File(atlasIndex, getName());
        if (indexFile.exists() && !indexFile.delete()) {
            throw new IndexBuilderException("Cannot delete " + indexFile.getAbsolutePath());
        }
        statistics = bitIndexExperiments(progressUpdater);
    }

    /**
     * Generates a ConciseSet-based index for all statistics types in StatisticsType enum, across all Atlas data
     *
     * @param progressUpdater a listener to be informed about the indexing process
     * @return StatisticsStorage containing statistics for all statistics types in StatisticsType enum - collected over all Atlas data
     */
    private StatisticsStorage bitIndexExperiments(final ProgressUpdater progressUpdater) {
        StatisticsStorage statisticsStorage = new StatisticsStorage();

        final ObjectPool<ExperimentInfo> experimentPool = new ObjectPool<ExperimentInfo>();
        final ObjectPool<EfvAttribute> efvAttributePool = new ObjectPool<EfvAttribute>();
        final ObjectPool<EfAttribute> efAttributePool = new ObjectPool<EfAttribute>();

        final ThreadSafeStatisticsBuilder upStats = new ThreadSafeStatisticsBuilder();
        final ThreadSafeStatisticsBuilder dnStats = new ThreadSafeStatisticsBuilder();
        final ThreadSafeStatisticsBuilder updnStats = new ThreadSafeStatisticsBuilder();
        final ThreadSafeStatisticsBuilder noStats = new ThreadSafeStatisticsBuilder();

        final BitIndexTask task = new BitIndexTask(experimentsToProcess());

        getLog().info("Found total experiments to index: " + task.getTotalExperiments());

        final ExecutorService summarizer = Executors.newFixedThreadPool(10);

        for (final Experiment exp : task.getExperiments()) {
            getLog().info("Processing exp: {}, # assays: {}", exp.getAccession(), exp.getAssays().size());
            final ExperimentWithData experimentWithData = atlasDataDAO.createExperimentWithData(exp);
            try {
                final ExperimentInfo experimentInfo = experimentPool.intern(new ExperimentInfo(exp.getAccession(), exp.getId()));

                for (ArrayDesign ad : exp.getArrayDesigns()) {
                    int car = 0; // count of all Statistics records added for this experiment/array design pair

                    StatisticsIterator stats = new StatisticsIterator(experimentWithData, ad, BIOENTITY_FILTER);

                    if (stats.isEmpty()) {
                        //task.skipEmpty(f);
                        getLog().info("Skipping empty " + stats);
                        continue;
                    }


                    getLog().info("   Processing ad: {}, # de's: {}, # uEFVs: {}",
                            new Object[]{ad.getAccession(), stats.getDeCount(), stats.getEfvCount()});
                    final Map<EfAttribute, MinPMaxT> efToPTUpDown = new HashMap<EfAttribute, MinPMaxT>();
                    while (stats.nextEFV()) {
                        final Pair<String, String> efv = stats.getEFV();

                        final EfvAttribute efvAttribute = efvAttributePool.intern(new EfvAttribute(efv.getKey(), efv.getValue()));
                        final EfAttribute efAttribute = efAttributePool.intern(new EfAttribute(efv.getKey()));

                        final Set<Integer> upBioEntityIds = new FastSet();
                        final Set<Integer> dnBioEntityIds = new FastSet();
                        final Set<Integer> noBioEntityIds = new FastSet();

                        // Initialise if necessary pval/tstat storage for ef
                        MinPMaxT ptUpDownForEf = efToPTUpDown.get(efAttribute);
                        if (ptUpDownForEf == null) {
                            efToPTUpDown.put(efAttribute, ptUpDownForEf = new MinPMaxT());
                        }

                        // Initialise pval/tstat storage for ef-efv
                        final MinPMaxT ptUpDown = new MinPMaxT();
                        final MinPMaxT ptUp = new MinPMaxT();
                        final MinPMaxT ptDown = new MinPMaxT();

                        while (stats.nextBioEntity()) {
                            int bioEntityId = stats.getIntegerBioEntityId();

                            final PTRank pt = PTRank.of(stats.getP(), stats.getT());
                            car++;
                            if (stats.isNonDe()) {
                                noBioEntityIds.add(bioEntityId);
                            } else {
                                if (stats.isUp()) {
                                    upBioEntityIds.add(bioEntityId);
                                    // Store if the lowest pVal/highest absolute value of tStat for ef-efv (up)
                                    ptUp.update(bioEntityId, pt);
                                } else {
                                    dnBioEntityIds.add(bioEntityId);
                                    // Store if the lowest pVal/highest absolute value of tStat for ef-efv (down)
                                    ptDown.update(bioEntityId, pt);
                                }
                                // Store if the lowest pVal/highest absolute value of tStat for ef-efv (up/down)
                                ptUpDown.update(bioEntityId, pt);
                                // Store if the lowest pVal/highest absolute value of tStat for ef  (up/down)
                                ptUpDownForEf.update(bioEntityId, pt);
                            }
                        }

                        summarizer.submit(new Runnable() {
                            @Override
                            public void run() {
                                // Store rounded minimum up pVals per gene for ef-efv
                                ptUp.storeStats(upStats, experimentInfo, efvAttribute);
                                // Store rounded minimum down pVals per gene for ef-efv
                                ptDown.storeStats(dnStats, experimentInfo, efvAttribute);
                                // Store rounded minimum up/down pVals per gene for ef-efv
                                ptUpDown.storeStats(updnStats, experimentInfo, efvAttribute);
                            }
                        });

                        // Store stats for ef-efv
                        upStats.addStatistics(efvAttribute, experimentInfo, upBioEntityIds);
                        dnStats.addStatistics(efvAttribute, experimentInfo, dnBioEntityIds);
                        updnStats.addStatistics(efvAttribute, experimentInfo, upBioEntityIds);
                        updnStats.addStatistics(efvAttribute, experimentInfo, dnBioEntityIds);
                        noStats.addStatistics(efvAttribute, experimentInfo, noBioEntityIds);

                        // Store stats for ef
                        upStats.addStatistics(efAttribute, experimentInfo, upBioEntityIds);
                        dnStats.addStatistics(efAttribute, experimentInfo, dnBioEntityIds);
                        updnStats.addStatistics(efAttribute, experimentInfo, upBioEntityIds);
                        updnStats.addStatistics(efAttribute, experimentInfo, dnBioEntityIds);
                        noStats.addStatistics(efAttribute, experimentInfo, noBioEntityIds);

                        // Add genes for ef attributes across all experiments
                        updnStats.addBioEntitiesForEfAttribute(efAttribute, upBioEntityIds);
                        updnStats.addBioEntitiesForEfAttribute(efAttribute, dnBioEntityIds);

                        // Add genes for ef-efv attributes across all experiments
                        updnStats.addBioEntitiesForEfvAttribute(efvAttribute, upBioEntityIds);
                        updnStats.addBioEntitiesForEfvAttribute(efvAttribute, dnBioEntityIds);
                    }

                    summarizer.submit(new Runnable() {
                        @Override
                        public void run() {
                            // Store rounded minimum up/down pVals per gene for all efs/scs
                            for (Map.Entry<EfAttribute, MinPMaxT> entry : efToPTUpDown.entrySet()) {
                                // Store min up/down pVal for efv
                                entry.getValue().storeStats(updnStats, experimentInfo, entry.getKey());
                            }
                        }
                    });

                    task.processedStats(car);
                    if (car == 0) {
                        getLog().debug("{} num uEFVs : {} [{}]", new Object[]{stats, stats.getEfvCount(), car});
                    }
                }

                task.done(exp);
                progressUpdater.update(task.progress());
            } catch (AtlasDataException e) {
                getLog().warn("Cannot access data for experiment " + exp.getAccession() + ", skipping", e);
            } catch (StatisticsNotFoundException e) {
                // this is just info, not warning because Atlas normally includes
                // some experiments with no statistics
                getLog().info("Cannot access statistics for experiment {}, skipping", exp.getAccession());
            } finally {
                closeQuietly(experimentWithData);
            }
        }

        try {
            // Load efo index
            EfoIndex efoIndex = loadEfoMapping(efvAttributePool, experimentPool);
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
        EfoIndex efoIndex = new EfoIndex(attributePool, experimentPool);
        getLog().info("Fetching ontology mappings...");

        List<OntologyMapping> mappings = getAtlasDAO().getOntologyMappingsByOntology("EFO");
        for (OntologyMapping mapping : mappings) {
            efoIndex.addMapping(mapping.getOntologyTerm(),
                    new EfvAttribute(mapping.getProperty(), mapping.getPropertyValue()),
                    new ExperimentInfo(mapping.getExperimentAccession(), mapping.getExperimentId()));
        }
        return efoIndex;
    }

    static class MinPMaxT {
        private final Map<Integer, PTRank> geneToBestPTRank = new HashMap<Integer, PTRank>();

        public void update(int bioEntityId, PTRank pt) {
            final PTRank bestSoFar = geneToBestPTRank.get(bioEntityId);
            if (bestSoFar == null || pt.compareTo(bestSoFar) < 0) {
                geneToBestPTRank.put(bioEntityId, pt);
            }
        }

        public void storeStats(StatisticsBuilder stats, ExperimentInfo expIdx, EfAttribute efAttribute) {
            for (Map.Entry<Integer, PTRank> entry : geneToBestPTRank.entrySet()) {
                stats.addPvalueTstatRank(efAttribute, entry.getValue(), expIdx, entry.getKey());
            }
        }
    }
}
