package ae3.service;

import ae3.service.structuredquery.AtlasEfvService;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
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

import javax.annotation.Nonnull;
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

        private AtlasEfvService efvService;

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

    public AtlasEfvService getEfvService() {
        return efvService;
    }

    public void setEfvService(AtlasEfvService efvService) {
        this.efvService = efvService;
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

        if (!isEfo && statisticsStorage.getIndexForAttribute(attr) == null) {
            // TODO NB. This is currently possible as sample properties are not currently stored in statisticsStorage
            log.debug("Attribute " + attr + " was not found in Attribute Index");
            // return experiment count == 0 if an efv attribute could not be found in AttributeIndex (note
            // that efo attributes are not explicitly stored in AttributeIndex)
            return 0;

        }

        if (geneRestrictionSet == null) { // By default restrict the experiment count query to geneId
            geneRestrictionSet = Collections.singleton(geneId);
        }

        StatisticsQueryCondition statsQuery = new StatisticsQueryCondition(geneRestrictionSet);
        statsQuery.and(getStatisticsOrQuery(Collections.singletonList(attr)));
        Multiset<Integer> scores = StatisticsQueryUtils.getExperimentCounts(statsQuery, statisticsStorage, null);

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
            if (attr.isEfo() == StatisticsQueryUtils.EFO) {
                Collection<String> efoPlusChildren = efo.getTermAndAllChildrenIds(attr.getValue());
                log.debug("Expanded efo: " + attr + " into: " + efoPlusChildren);
                for (String efoTerm : efoPlusChildren) {
                    attrsPlusChildren.add(new Attribute(efoTerm, StatisticsQueryUtils.EFO, attr.getStatType()));
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
        if (min < 0)
            min = 0;
        if (sortedByCount.size() > max) {
            return sortedByCount.subList(min, max);
        }
        return sortedByCount.subList(min, sortedByCount.size());
    }

    /**
     * @param statsQuery
     * @param minPos
     * @param rows
     * @param sortedGenesChunk - a chunk of the overall sorted (by experiment counts - in desc order) list of genes,
     *                         starting from 'minPos' and containing maximums 'rows' genes
     * @return The overall number of genes for which counts exist in statsQuery
     */
    public Integer getSortedGenes(final StatisticsQueryCondition statsQuery, final int minPos, final int rows, List<Long> sortedGenesChunk) {
        long timeStart = System.currentTimeMillis();
        Multiset<Integer> countsForConditions = StatisticsQueryUtils.getExperimentCounts(statsQuery, statisticsStorage, null);
        log.debug("getSortedGenes() bit index query: " + statsQuery.prettyPrint());
        log.info("getSortedGenes() query returned " + countsForConditions.elementSet().size() +
                " genes with counts present in : " + (System.currentTimeMillis() - timeStart) + " ms");
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

    /**
     * @param geneIds
     * @param statType
     * @return Set of efo and efv attributes that have non-zero experiment counts for geneId and statType in bit index
     */
    public Set<Attribute> getScoringAttributesForGenes(Set<Long> geneIds, StatisticsType statType) {
        return StatisticsQueryUtils.getScoringAttributesForGenes(geneIds, statType, statisticsStorage);
    }

    /**
     * @param geneId
     * @param statType
     * @param ef
     * @param efv
     * @return Set of Experiments in which geneId-ef-efv have statType expression
     */
    public Set<Experiment> getScoringExperimentsForGeneAndAttribute(Long geneId, StatisticsType statType, String ef, String efv) {
        return StatisticsQueryUtils.getScoringExperimentsForGeneAndAttribute(geneId, statType, ef, efv, statisticsStorage);
    }


    /**
     * @param efoTerm
     * @return Set of Attributes corresponding to efoTerm. Note that efo's map to ef-efv-experiment triples. However, this method
     *         is used in AtlasStructuredQueryService for populating list view, which for efo queries shows ef-efvs those efos map to and
     *         _all_ experiments in which these ef-efvs have expressions. In other words, we don't restrict experiments shown in the list view
     *         to just those in query efo->ef-efv-experiment mapping.
     */
    public Set<Attribute> getAttributesForEfo(String efoTerm) {
        Set<Attribute> attrsForEfo = new HashSet<Attribute>();
        Map<Integer, Set<Integer>> expToAttrsForEfo = statisticsStorage.getMappingsForEfo(efoTerm);

        if (expToAttrsForEfo != null) {
            for (Collection<Integer> expToAttrIndexes : expToAttrsForEfo.values()) {

                Collection<Attribute> attrsForExp = Collections2.transform(expToAttrIndexes,
                        new Function<Integer, Attribute>() {
                            public Attribute apply(@Nonnull Integer attrIndex) {
                                return statisticsStorage.getAttributeForIndex(attrIndex);
                            }
                        });
                attrsForEfo.addAll(attrsForExp);
            }
        }
        return attrsForEfo;
    }

    /**
     *
     * @param geneId    Gene of interest
     * @param statType  StatisticsType
     * @param ef
     * @param efv
     * @param isEfo     if isEfo == StatisticsQueryUtils.EFO, efv is taken as an efo term
     * @param fromRow   Used for paginating of experiment plots on gene page
     * @param toRow     ditto
     * @return List of Experiments sorted by pVal/tStat ranks from best to worst
     */
    public List<Experiment> getExperimentsSortedByPvalueTRank(
            final Long geneId,
            final StatisticsType statType,
            final String ef,
            final String efv,
            final boolean isEfo,
            final int fromRow,
            final int toRow) {

        Set<String> efs;
        if (ef != null && !"".equals(ef)) {
            efs = Collections.singleton(ef);
        } else {
            efs = efvService.getAllFactors();
        }

        List<Attribute> attrs = new ArrayList<Attribute>();

        for (String expFactor : efs) {
            // Assemble stats query that will be used to extract sorted experiments
            Attribute attr;
            if (efv != null) {
                if (isEfo == StatisticsQueryUtils.EFO) { // efo attribute
                    attr = new Attribute(efv, isEfo, statType);
                } else { // ef-efv Attribute
                    attr = new Attribute(expFactor, efv);
                }
                attr = new Attribute(expFactor, efv);
            } else { // ef only Attribute
                attr = new Attribute(expFactor);
            }
            attr.setStatType(statType);
            attrs.add(attr);
        }

        StatisticsQueryCondition statsQuery = new StatisticsQueryCondition(Collections.singleton(geneId));
        statsQuery.and(getStatisticsOrQuery(attrs));

        // retrieve experiments sorted by pValue/tRank for statsQuery
        List<Experiment> bestExperiments = new ArrayList<Experiment>();
        StatisticsQueryUtils.getBestExperiments(statsQuery, statisticsStorage, bestExperiments);

        // Sort bestExperiments by best pVal/tStat ranks first
        Collections.sort(bestExperiments, new Comparator<Experiment>() {
            public int compare(Experiment e1, Experiment e2) {
                return e1.getpValTStatRank().compareTo(e2.getpValTStatRank());
            }
        });

        List<Experiment> exps = new ArrayList<Experiment>();
        int i = 0;
        for (Experiment experiment : bestExperiments) {
            if (i > toRow)
                break;
            if (i >= fromRow)
                exps.add(experiment);
            i++;
        }
        log.info("Sorted experiments: ");
        for (Experiment exp : exps) {
            log.info(exp.getAccession() + ": pval=" + exp.getpValTStatRank().getPValue() + "; tStat rank: " + exp.getpValTStatRank().getTStatRank());
        }
        return exps;
    }
}
