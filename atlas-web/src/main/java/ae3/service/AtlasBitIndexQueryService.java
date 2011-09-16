package ae3.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.index.StatisticsStorageFactory;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.statistics.*;
import uk.ac.ebi.gxa.utils.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.google.common.collect.HashMultiset.create;
import static com.google.common.collect.Maps.newHashMap;
import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

/**
 * This class provides bioentity expression statistics query service:
 * - manages the index storage management and interaction with IndexBuider service
 * - delegates statistics queries to StatisticsQueryUtils
 */
public class AtlasBitIndexQueryService implements AtlasStatisticsQueryService {

    final private Logger log = LoggerFactory.getLogger(getClass());

    // Handler for the BitIndex builder
    private IndexBuilder indexBuilder;
    // Bitindex Object which is de-serialized from indexFileName in atlasIndexDir
    private StatisticsStorage statisticsStorage;
    private File atlasIndexDir;
    private String indexFileName;

    // Used for finding children for query efo's
    private Efo efo;

    public AtlasBitIndexQueryService(String indexFileName) {
        this.indexFileName = indexFileName;
    }

    public void setAtlasIndex(File atlasIndexDir) {
        this.atlasIndexDir = atlasIndexDir;
    }

    public void setIndexBuilder(IndexBuilder indexBuilder) {
        this.indexBuilder = indexBuilder;
        indexBuilder.registerIndexBuildEventHandler(this);
    }

    public void setStatisticsStorage(StatisticsStorage statisticsStorage) {
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
            String errMsg = "Failed to create statisticsStorage from " + new File(atlasIndexDir.getAbsolutePath(), indexFileName);
            log.error(errMsg, ioe);
            throw createUnexpected(errMsg, ioe);
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
     * @param attribute
     * @param bioEntityId
     * @param statType
     * @return Experiment count for statisticsType, attributes and bioEntityId
     */
    public Integer getExperimentCountsForBioEntity(
            final Attribute attribute,
            final Integer bioEntityId,
            final StatisticsType statType) {
        return getExperimentCountsForBioEntity(attribute, bioEntityId, statType, null, null);
    }

    /**
     * @param attribute
     * @param bioEntityId
     * @param statType
     * @param bioEntityIdRestrictionSet
     * @param scoresCache
     * @return Experiment count for statisticsType, attributes and bioEntityId
     */
    public Integer getExperimentCountsForBioEntity(
            final Attribute attribute,
            final Integer bioEntityId,
            final StatisticsType statType,
            Set<Integer> bioEntityIdRestrictionSet,
            Map<StatisticsType, HashMap<String, Multiset<Integer>>> scoresCache) {

        if (bioEntityIdRestrictionSet == null) { // By default restrict the experiment count query to bioEntityId
            bioEntityIdRestrictionSet = Collections.singleton(bioEntityId);
        }

        StatisticsQueryCondition statsQuery = new StatisticsQueryCondition(bioEntityIdRestrictionSet, statType);
        statsQuery.and(getStatisticsOrQuery(Collections.singletonList(attribute), statType, 1));
        Multiset<Integer> scores = StatisticsQueryUtils.getExperimentCounts(statsQuery, statisticsStorage, null);

        // Cache bioEntityIdRestrictionSet's scores for efvOrEfo - this cache will be re-used in heatmaps for rows other than the first one
        if (scoresCache != null) {
            scoresCache.get(statType).put(attribute.getValue(), scores);
        }

        if (scores != null) {
            long time = System.currentTimeMillis();
            int expCountForBioEntity = scores.count(bioEntityId);
            if (expCountForBioEntity > 0) {
                log.debug(statType + " " + attribute.getValue() + " expCountForBioEntity: " + bioEntityId + " = " + expCountForBioEntity + " got in:  " + (System.currentTimeMillis() - time) + " ms");
            }
            return expCountForBioEntity;
        }
        return 0;
    }

    /**
     * @param orAttributes
     * @param statType
     * @param minExperiments minimum number of experiments restriction for this clause
     * @return StatisticsQueryOrConditions, including children of all efo's in orAttributes
     */
    public StatisticsQueryOrConditions<StatisticsQueryCondition> getStatisticsOrQuery(
            final List<Attribute> orAttributes,
            final StatisticsType statType,
            int minExperiments) {
        List<Attribute> efoPlusChildren = includeEfoChildren(orAttributes);
        return StatisticsQueryUtils.getStatisticsOrQuery(efoPlusChildren, statType, minExperiments, statisticsStorage);
    }

    /**
     * @param orAttributes
     * @return List containing all (afv and efo) attributes in orAttributes, plus the children of all efo's in orAttributes
     */
    private List<Attribute> includeEfoChildren(List<Attribute> orAttributes) {
        // LinkedHashSet for maintaining order of entry - order of processing attributes may be important
        // in multi-Attribute queries for sorted lists of experiments for the gene page
        Set<Attribute> attrsPlusChildren = new LinkedHashSet<Attribute>();
        for (Attribute attr : orAttributes)
            attrsPlusChildren.addAll(attr.getAttributeAndChildren(efo));
        return new ArrayList<Attribute>(attrsPlusChildren);
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
        int totalResults = sortedByCount.size();
        if (min < 0 || min > totalResults)
            min = 0;

        if (totalResults > max) {
            return sortedByCount.subList(min, max);
        }
        return sortedByCount.subList(min, totalResults);
    }

    /**
     * @param statsQuery
     * @param minPos
     * @param rows
     * @param bioEntityIdRestrictionSet Set of BioEntity ids to restrict the query before sorting
     * @param sortedBioEntitiesChunk    - a chunk of the overall sorted (by experiment counts - in desc order) list of bioentities,
     *                                  starting from 'minPos' and containing maximums 'rows' bioentities
     * @return Pair<The overall number of bioentities for which counts exist in statsQuery, total experiemnt count for returned genes>
     */
    public Pair<Integer, Integer> getSortedBioEntities(
            final StatisticsQueryCondition statsQuery,
            final int minPos,
            final int rows,
            final Set<Integer> bioEntityIdRestrictionSet,
            List<Integer> sortedBioEntitiesChunk) {
        long timeStart = System.currentTimeMillis();

        Multiset<Integer> countsForConditions =
                StatisticsQueryUtils.intersect(StatisticsQueryUtils.getExperimentCounts(statsQuery, statisticsStorage, null),
                        bioEntityIdRestrictionSet);
        log.debug("Intersected " + countsForConditions.entrySet().size() + " bioentities' experiment counts with " + bioEntityIdRestrictionSet.size() +
                " restriction bioeentities in " + (System.currentTimeMillis() - timeStart) + " ms");

        log.debug("getSortedBioEntities() bit index query: " + statsQuery.prettyPrint());
        log.info("getSortedBioEntities() query returned " + countsForConditions.elementSet().size() +
                " bioentities with counts present in : " + (System.currentTimeMillis() - timeStart) + " ms");
        List<Multiset.Entry<Integer>> sortedCounts = getEntriesBetweenMinMaxFromListSortedByCount(countsForConditions, minPos, minPos + rows);
        int totalExpCount = 0;
        for (Multiset.Entry<Integer> entry : sortedCounts) {
            sortedBioEntitiesChunk.add(entry.getElement());
            totalExpCount += entry.getCount();
        }
        log.debug("Total experiment count: " + totalExpCount);

        return Pair.create(countsForConditions.elementSet().size(), totalExpCount);
    }

    /**
     * @param efoTerm
     * @return Set of EfvAttributes corresponding to efoTerm. Note that efo's map to ef-efv-experiment triples. However, this method
     *         is used in AtlasStructuredQueryService for populating list view, which for efo queries shows ef-efvs those efos map to and
     *         _all_ experiments in which these ef-efvs have expressions. In other words, we don't restrict experiments shown in the list view
     *         to just those in query efo->ef-efv-experiment mapping.
     */
    public Set<EfvAttribute> getAttributesForEfo(String efoTerm) {
        Set<EfvAttribute> attrsForEfo = new HashSet<EfvAttribute>();
        Map<ExperimentInfo, Set<EfvAttribute>> expToAttrsForEfo = statisticsStorage.getMappingsForEfo(efoTerm);
        for (Collection<EfvAttribute> expToAttrIndexes : expToAttrsForEfo.values()) {
            attrsForEfo.addAll(expToAttrIndexes);
        }
        return attrsForEfo;
    }

    /**
     * @param efoTerm
     * @return the total count of experiment-attribute mappings for efoTerm. A measure of how expensive a given efoTerm will be
     *         to search against bit index. Currently used just for logging.
     */
    public int getMappingsCountForEfo(String efoTerm) {
        int count = 0;
        Map<ExperimentInfo, Set<EfvAttribute>> expToAttrsForEfo = statisticsStorage.getMappingsForEfo(efoTerm);
        for (Collection<EfvAttribute> expToAttrIndexes : expToAttrsForEfo.values()) {
            count += expToAttrIndexes.size();
        }
        return count;
    }

    /**
     *
     * @param bioEntityId Bioentity of interest
     * @param attribute   Attribute
     * @param fromRow     Used for paginating of experiment plots on gene page
     * @param toRow       ditto
     * @param statType    StatisticsType
     * @return List of Experiments sorted by pVal/tStat ranks from best to worst
     */
    public List<ExperimentResult> getExperimentsSortedByPvalueTRank(
            final Integer bioEntityId,
            final Attribute attribute,
            int fromRow,
            int toRow,
            final StatisticsType statType) {

        List<Attribute> attrs;
        if (attribute.isEmpty()) { // Empty attribute
            List<String> efs = getScoringEfsForBioEntity(bioEntityId, statType, null);
            attrs = new ArrayList<Attribute>();
            for (String expFactor : efs) {
                EfvAttribute attr = new EfvAttribute(expFactor);
                attrs.add(attr);
            }
        } else {
            attrs = Collections.singletonList(attribute);
        }


        StatisticsQueryCondition statsQuery = new StatisticsQueryCondition(Collections.singleton(bioEntityId), statType);
        statsQuery.and(getStatisticsOrQuery(attrs, statType, 1));

        // retrieve experiments sorted by pValue/tRank for statsQuery
        // Map: experiment id -> ExperimentInfo (used in getBestExperiments() for better than List efficiency of access)
        Map<Long, ExperimentResult> bestExperimentsMap = newHashMap();
        StatisticsQueryUtils.getBestExperiments(statsQuery, statisticsStorage, bestExperimentsMap);

        List<ExperimentResult> bestExperiments = new ArrayList<ExperimentResult>(bestExperimentsMap.values());
        // Sort bestExperiments by best pVal/tStat ranks first
        Collections.sort(bestExperiments, new Comparator<ExperimentResult>() {
            public int compare(ExperimentResult e1, ExperimentResult e2) {
                return e1.getPValTStatRank().compareTo(e2.getPValTStatRank());
            }
        });

        // Extract the correct chunk (Note that if toRow == fromRow == -1, the whole of bestExperiments is returned)
        int maxSize = bestExperiments.size();
        if (fromRow == -1)
            fromRow = 0;
        if (toRow == -1 || toRow > maxSize)
            toRow = maxSize;
        List<ExperimentResult> exps = bestExperiments.subList(fromRow, toRow);

        log.debug("Sorted experiments: ");
        for (ExperimentResult exp : exps) {
            log.debug(exp.getAccession() + ": pval=" + exp.getPValTStatRank().getPValue() +
                    "; tStat rank: " + exp.getPValTStatRank().getTStatRank() + "; highest ranking ef: " + exp.getHighestRankAttribute());
        }
        return exps;
    }


    /**
     * @param bioEntityId
     * @param statType
     * @param ef
     * @return list all efs for which bioEntityId has statType expression in at least one experiment
     */
    public List<String> getScoringEfsForBioEntity(final Integer bioEntityId,
                                                  final StatisticsType statType,
                                                  @Nullable final String ef) {

        long timeStart = System.currentTimeMillis();
        List<String> scoringEfs = new ArrayList<String>();
        if (bioEntityId != null) {
            Set<EfvAttribute> scoringEfAttrs = statisticsStorage.getScoringEfAttributesForBioEntity(bioEntityId, statType);
            for (EfvAttribute efAttr : scoringEfAttrs) {
                if (efAttr != null && (ef == null || "".equals(ef) || ef.equals(efAttr.getEf()))) {
                    scoringEfs.add(efAttr.getEf());
                }
            }
        }
        log.debug("getScoringEfsForBioEntity() returned " + scoringEfs.size() + " efs for bioEntityId: " + bioEntityId + " in: " + (System.currentTimeMillis() - timeStart) + " ms");

        return scoringEfs;
    }

    /**
     * @param bioEntityId
     * @param statType
     * @return list all efs for which bioEntityId has statType expression in at least one experiment
     */
    public List<EfvAttribute> getScoringEfvsForBioEntity(final Integer bioEntityId,
                                                         final StatisticsType statType) {

        long timeStart = System.currentTimeMillis();
        List<EfvAttribute> scoringEfvs = new ArrayList<EfvAttribute>();
        if (bioEntityId != null) {
            Set<EfvAttribute> scoringEfvIndexes = statisticsStorage.getScoringEfvAttributesForBioEntity(bioEntityId, statType);
            for (EfvAttribute efv : scoringEfvIndexes) {
                if (efv.getEfv() != null && !efv.getEfv().isEmpty()) {
                    scoringEfvs.add(efv);
                }
            }
        }
        log.debug("getScoringEfsForBioEntity()  returned " + scoringEfvs.size() + " efs for bioEntityId: " + bioEntityId + " in: " + (System.currentTimeMillis() - timeStart) + " ms");

        return scoringEfvs;
    }

    /**
     * @param attribute
     * @param bioEntityId
     * @param statType
     * @return unsorted list of experiments for which bioEntityId has statType expression for ef attr
     */
    public List<ExperimentInfo> getExperimentsForBioEntityAndAttribute(Integer bioEntityId, @Nullable EfvAttribute attribute, StatisticsType statType) {
        List<ExperimentInfo> exps = new ArrayList<ExperimentInfo>();
        // Note that if ef == null, this method returns list of experiments across all efs for which this bioentity has up/down exp counts
        if (bioEntityId != null) {
            return new ArrayList<ExperimentInfo>(statisticsStorage.getExperimentsForBioEntityAndAttribute(attribute, bioEntityId, statType));
        }
        return exps;
    }

    /**
     * @param bioEntityIds
     * @param statType
     * @param autoFactors  set of factors of interest
     * @param attrCounts   if not null, populated by this method. Map: attribute Index -> (non-zero) experiment counts
     * @param scoringEfos  if not null, populated by this method. Set of Efo terms with non-zero experiment counts
     */
    private void collectScoringAttributes(Set<Integer> bioEntityIds, StatisticsType statType, Collection<String> autoFactors,
                                          @Nullable Multiset<EfvAttribute> attrCounts, @Nullable Set<String> scoringEfos) {
        for (EfvAttribute efvAttr : statisticsStorage.getAllAttributes(statType)) {
            if ((autoFactors != null && !autoFactors.contains(efvAttr.getEf())) || efvAttr.getEfv() == null) {
                continue; // skip attribute if its factor is not of interest or it's an ef-only attribute
            }
            StatisticsQueryCondition statsQuery = new StatisticsQueryCondition(bioEntityIds, statType);
            statsQuery.and(getStatisticsOrQuery(Collections.<Attribute>singletonList(efvAttr), statType, 1));
            Set<ExperimentInfo> scoringExps = new HashSet<ExperimentInfo>();
            StatisticsQueryUtils.getExperimentCounts(statsQuery, statisticsStorage, scoringExps);
            if (scoringExps.size() > 0) { // at least one bioEntityId in bioEntityIds had an experiment count > 0 for attr
                if (attrCounts != null)
                    attrCounts.add(efvAttr, scoringExps.size());
                for (ExperimentInfo exp : scoringExps) {
                    String efoTerm = statisticsStorage.getEfoTerm(efvAttr, exp);
                    if (efoTerm != null) {
                        if (scoringEfos != null)
                            scoringEfos.add(efoTerm);
                        else
                            log.debug("Skipping efo: " + efoTerm + " for attr: " + efvAttr + " and exp: " + exp);
                    }
                }
            }
        }
    }

    /**
     * @param bioEntityIds
     * @param statType
     * @return Set of efo's with non-zero statType experiment counts for bioEntityIds
     */
    public Set<String> getScoringEfosForBioEntities(Set<Integer> bioEntityIds, StatisticsType statType) {
        Set<String> scoringEfos = new HashSet<String>();
        collectScoringAttributes(bioEntityIds, statType, null, null, scoringEfos);
        return scoringEfos;
    }

    /**
     *
     * @param bioEntityIds
     * @param statType
     * @param autoFactors  set of factors of interest
     * @return Serted set of non-zero experiment counts (for at least one of bioEntityIds and statType) per efv (note: not efo) attribute
     */
    public List<Multiset.Entry<EfvAttribute>> getScoringAttributesForBioEntities(Set<Integer> bioEntityIds, StatisticsType statType, Collection<String> autoFactors) {
        long timeStart = System.currentTimeMillis();

        Multiset<EfvAttribute> attrCounts = create();
        collectScoringAttributes(bioEntityIds, statType, autoFactors, attrCounts, null);

        List<Multiset.Entry<EfvAttribute>> sortedAttrCounts = getEntriesBetweenMinMaxFromListSortedByCount(attrCounts, 0, attrCounts.entrySet().size());

        log.debug("Retrieved " + sortedAttrCounts.size() + " sorted scoring attributes for statType: " + statType + " and bioentity ids: (" + bioEntityIds + ") in " + (System.currentTimeMillis() - timeStart) + "ms");
        return sortedAttrCounts;
    }

    /**
     * @param bioEntityId
     * @param attribute
     * @param statType
     * @return Set of Experiments in which bioEntityId-ef-efv have statType expression
     */
    public Set<ExperimentInfo> getScoringExperimentsForBioEntityAndAttribute(
            final Integer bioEntityId,
            @Nonnull Attribute attribute,
            final StatisticsType statType) {
        StatisticsQueryCondition statsQuery = new StatisticsQueryCondition(Collections.singleton(bioEntityId), statType);
        statsQuery.and(getStatisticsOrQuery(Collections.<Attribute>singletonList(attribute), statType, 1));
        Set<ExperimentInfo> scoringExps = new HashSet<ExperimentInfo>();
        StatisticsQueryUtils.getExperimentCounts(statsQuery, statisticsStorage, scoringExps);
        return scoringExps;
    }

    /**
     * @param attribute
     * @param allExpsToAttrs Map: ExperimentInfo -> Set<Attribute> to which mappings for an Attribute are to be added.
     */
    public void getEfvExperimentMappings(
            final Attribute attribute,
            Map<ExperimentInfo, Set<EfvAttribute>> allExpsToAttrs) {
        attribute.getEfvExperimentMappings(statisticsStorage, allExpsToAttrs);
    }

    /**
     * @param statType
     * @return Collection of unique experiments with expressions for statType
     */
    public Collection<ExperimentInfo> getScoringExperiments(StatisticsType statType) {
        return statisticsStorage.getScoringExperiments(statType);
    }

    /**
     * @param attribute
     * @param statType
     * @return the amount of bioentities with expression statType for efv attribute
     */
    public int getBioEntityCountForEfvAttribute(EfvAttribute attribute, StatisticsType statType) {
        return statisticsStorage.getBioEntityCountForAttribute(attribute, statType);
    }


    /**
     * @param attribute
     * @param statType
     * @return the amount of bioentities with expression statType for efo attribute
     */
    public int getBioEntityCountForEfoAttribute(Attribute attribute, StatisticsType statType) {
        StatisticsQueryCondition statsQuery = new StatisticsQueryCondition(statType);
        statsQuery.and(getStatisticsOrQuery(Collections.singletonList(attribute), statType, 1));
        return StatisticsQueryUtils.getExperimentCounts(statsQuery, statisticsStorage, null).entrySet().size();
    }
}
