package ae3.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.index.StatisticsStorageFactory;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.IndexBuilderEventHandler;
import uk.ac.ebi.gxa.statistics.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: Nov 2, 2010
 * Time: 5:27:03 PM
 * This class provides gene expression statistics query service:
 * - manages the index storage management and interaction with IndexBuider service
 * - delegates statistics queries to StatisticsQueryUtils
 */
public class AtlasStatisticsQueryService implements IndexBuilderEventHandler, DisposableBean {

    final private Logger log = LoggerFactory.getLogger(getClass());

    // Handler for the BitIndex builder
    private IndexBuilder indexBuilder;
    // Bitindex Object which is de-serialized from indexFileName in atlasIndexDir
    private StatisticsStorage<Long> statisticsStorage;
    private File atlasIndexDir;
    private String indexFileName;

    // Used for finding children for query efo's
    private Efo efo;

    public AtlasStatisticsQueryService(String indexFileName) {
        this.indexFileName = indexFileName;
    }

    public void setAtlasIndex(File atlasIndexDir) {
        this.atlasIndexDir = atlasIndexDir;
    }

    public void setIndexBuilder(IndexBuilder indexBuilder) {
        this.indexBuilder = indexBuilder;
        indexBuilder.registerIndexBuildEventHandler(this);
    }

    public void setStatisticsStorage(StatisticsStorage<Long> statisticsStorage) {
        this.statisticsStorage = statisticsStorage;

    }

    public void setEfo(Efo efo) {
        this.efo = efo;
    }

    /**
     * Index rebuild notification handler - after bit index is re-built, de-serialize it into statisticsStorage and re-populate statTypeToEfoToScores cache
     */
    public void onIndexBuildFinish() {
        StatisticsStorageFactory statisticsStorageFactory = new StatisticsStorageFactory(indexFileName);
        statisticsStorageFactory.setAtlasIndex(atlasIndexDir);
        try {
            statisticsStorage = statisticsStorageFactory.createStatisticsStorage();
        } catch (IOException ioe) {
            String errMsg = "Failed to create statisticsStorage from " + atlasIndexDir.getAbsolutePath() + File.separator + indexFileName;
            log.error(errMsg, ioe);
            throw new RuntimeException(errMsg, ioe);
        }
    }

    public void onIndexBuildStart() {
        // Nothing to do here
    }

    /**
     * Destructor called by Spring
     *
     * @throws Exception
     */
    public void destroy() throws Exception {
        if (indexBuilder != null)
            indexBuilder.unregisterIndexBuildEventHandler(this);
    }

    /**
     * @param efvOrEfo
     * @param statisticsType
     * @param isEfo
     * @param geneId
     * @return Experiment count for statisticsType, attributes and geneId
     */
    public Integer getExperimentCountsForGene(String efvOrEfo, StatisticsType statisticsType, boolean isEfo, Long geneId) {
        return getExperimentCountsForGene(efvOrEfo, statisticsType, isEfo, geneId, null, null);
    }

    /**
     * @param efvOrEfo
     * @param statisticsType
     * @param isEfo
     * @param geneId
     * @return Experiment count for statisticsType, attributes and geneId
     */
    public Integer getExperimentCountsForGene(
            String efvOrEfo,
            StatisticsType statisticsType,
            boolean isEfo,
            Long geneId,
            Set<Long> geneRestrictionSet,
            HashMap<String, Multiset<Integer>> scoresCacheForStatType) {

        Attribute attr = new Attribute(efvOrEfo, isEfo, statisticsType);

        if (geneRestrictionSet == null) {
            geneRestrictionSet = new HashSet<Long>(Collections.singletonList(geneId));
        }

        StatisticsQueryCondition statsQuery = new StatisticsQueryCondition(geneRestrictionSet);
        statsQuery.and(getStatisticsOrQuery(Collections.singletonList(attr)));
        Multiset<Integer> scores = StatisticsQueryUtils.getExperimentCounts(statsQuery, statisticsStorage);

        // Cache geneRestrictionSet's scores for efvOrEfo - this cache will be re-used in heatmaps for rows other than the first one
        if (scoresCacheForStatType != null) {
            scoresCacheForStatType.put(efvOrEfo, scores);
        }
        Integer geneIndex = statisticsStorage.getIndexForGeneId(geneId);

        if (scores != null) {
            long time = System.currentTimeMillis();
            int expCountForGene = scores.count(geneIndex);
            if (expCountForGene > 0) {
                log.debug(statisticsType + " " + efvOrEfo + " expCountForGene: " + geneId + " (" + geneIndex + ") = " + expCountForGene + " got in:  " + (System.currentTimeMillis() - time) + " ms");
            }
            return expCountForGene;
        }
        return 0;
    }

    /**
     * @param orAttributes
     * @return StatisticsQueryOrConditions, including children of all efo's in orAttributes
     */
    public StatisticsQueryOrConditions<StatisticsQueryCondition> getStatisticsOrQuery(List<Attribute> orAttributes) {
        List<Attribute> efoPlusChildren = includeEfoChildren(orAttributes);
        return StatisticsQueryUtils.getStatisticsOrQuery(efoPlusChildren, statisticsStorage);
    }

    /**
     * @param orAttributes
     * @return List containing all (afv and efo) attributes in orAttributes, plus the children of all efo's in orAttributes
     */
    private List<Attribute> includeEfoChildren(List<Attribute> orAttributes) {
        Set<Attribute> attrsPlusChildren = new HashSet<Attribute>();
        for (Attribute attr : orAttributes) {
            if (attr.isEfo() == StatisticsQueryUtils.EFO_QUERY) {
                Collection<String> efoPlusChildren = efo.getTermAndAllChildrenIds(attr.getEfv());
                log.info("Expanded efo: " + attr + " into: " + efoPlusChildren);
                for (String efoTerm : efoPlusChildren) {
                    attrsPlusChildren.add(new Attribute(efoTerm, StatisticsQueryUtils.EFO_QUERY, attr.getStatType()));
                }
            } else {
                attrsPlusChildren.add(attr);
            }
        }
        return new ArrayList<Attribute>(attrsPlusChildren);
    }


    public Integer getIndexForGene(Long geneId) {
        return statisticsStorage.getIndexForGeneId(geneId);
    }


    /**
     * http://stackoverflow.com/questions/3029151/find-top-n-elements-in-a-multiset-from-google-collections
     *
     * @param multiset
     * @param <T>
     * @return
     */
    private static <T> ImmutableList<Multiset.Entry<T>> sortedByCount(Multiset<T> multiset) {
        Ordering<Multiset.Entry<T>> countComp = new Ordering<Multiset.Entry<T>>() {
            public int compare(Multiset.Entry<T> e1, Multiset.Entry<T> e2) {
                return e2.getCount() - e1.getCount();
            }
        };
        return countComp.immutableSortedCopy(multiset.entrySet());
    }

    /**
     * http://stackoverflow.com/questions/3029151/find-top-n-elements-in-a-multiset-from-google-collections
     *
     * @param multiset
     * @param min
     * @param max
     * @param <T>
     * @return
     */
    private static <T> ImmutableList<Multiset.Entry<T>> getEntriesBetweenMinMaxFromListSortedByCount(Multiset<T> multiset,
                                                                                                     int min, int max) {
        ImmutableList<Multiset.Entry<T>> sortedByCount = sortedByCount(multiset);
        if (sortedByCount.size() > max) {
            sortedByCount = sortedByCount.subList(min, max);
        }

        return sortedByCount;
    }

    /**
     * @param statsQuery
     * @param minPos
     * @param rows
     * @param sortedGenesChunk
     * @return overall
     */
    public Integer getSortedGenes(final StatisticsQueryCondition statsQuery, final int minPos, final int rows, List<Long> sortedGenesChunk) {
        long timeStart = System.currentTimeMillis();
        Multiset<Integer> countsForConditions = StatisticsQueryUtils.getExperimentCounts(statsQuery, statisticsStorage);
        log.info("conditions bit index query for " + statsQuery.prettyPrint() + " (genes with counts present: " + countsForConditions.elementSet().size() + ") retrieved in : " + (System.currentTimeMillis() - timeStart) + " ms");
        List<Multiset.Entry<Integer>> sortedCounts = getEntriesBetweenMinMaxFromListSortedByCount(countsForConditions, minPos, minPos + rows);
        for (Multiset.Entry<Integer> entry : sortedCounts) {
            Long geneId = statisticsStorage.getGeneIdForIndex(entry.getElement());
            if (geneId != null) {
                sortedGenesChunk.add(geneId);
            } else {
                log.error("Failed to retrieve gene id for index: " + entry.getElement());
            }
        }

        return countsForConditions.elementSet().size();
    }
}
