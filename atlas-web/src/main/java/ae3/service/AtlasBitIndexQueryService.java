package ae3.service;

import com.google.common.collect.*;
import it.uniroma3.mat.extendedset.ConciseSet;
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
     * @param statsQuery
     * @return A map containing
     *  - scoring experiments as keys
     *  - Collections of scoring statistics type-attribute pairs in experiment key as values
     */
    public Multimap<ExperimentInfo, Pair<StatisticsType, EfAttribute>> getScoringExpsAttrs(
            final StatisticsQueryCondition statsQuery) {
        return StatisticsQueryUtils.getScoringExpsAttrs(statsQuery, statisticsStorage);
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
            final Collection<? extends Attribute> orAttributes,
            final StatisticsType statType,
            int minExperiments) {
        List<Attribute> efoPlusChildren = includeEfoChildren(orAttributes);
        return StatisticsQueryUtils.getStatisticsOrQuery(efoPlusChildren, statType, minExperiments, statisticsStorage);
    }

    /**
     * @param orAttributes
     * @return Collection containing all (afv and efo) attributes in orAttributes, plus the children of all efo's in orAttributes
     */
    private List<Attribute> includeEfoChildren(Collection<? extends Attribute> orAttributes) {
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
     * @param geneIds
     * @return A subset of geneIds that is scoring according to the statsQuery result
     */
    public Set<Integer> restrictGenesByStatsQuery(final StatisticsQueryCondition statsQuery,
                                                  final Set<Integer> geneIds) {
        Multiset<Integer> experimentCounts = StatisticsQueryUtils.getExperimentCounts(statsQuery, statisticsStorage, null);
        if (!geneIds.isEmpty()) {
            experimentCounts = StatisticsQueryUtils.intersect(experimentCounts, geneIds);
        }
        return experimentCounts.elementSet();
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
     * @param bioEntityId
     * @param attributes
     * @param statType    any of StatisticsType's apart from UP_DOWN
     * @return List of ExperimentResult's corresponding to bioEntityId, attributes and statType, sorted by tStatRank/pval in which each experiment occurs only once
     */
    private List<ExperimentResult> getBestExperiments(final Integer bioEntityId,
                                                      final List<Attribute> attributes,
                                                      final StatisticsType statType) {

        List<ExperimentResult> bestExperiments = Lists.newArrayList();
        if (statType == StatisticsType.UP_DOWN) {
            bestExperiments = Lists.newArrayList();
            bestExperiments.addAll(getBestExperiments(bioEntityId, attributes, StatisticsType.UP));
            bestExperiments.addAll(getBestExperiments(bioEntityId, attributes, StatisticsType.DOWN));

            // Sort bestExperiments by best pVal/tStat ranks first - across UP and DOWN
            Collections.sort(bestExperiments, new Comparator<ExperimentResult>() {
                public int compare(ExperimentResult e1, ExperimentResult e2) {
                    return e1.getPValTStatRank().compareTo(e2.getPValTStatRank());
                }
            });
            // Now retain only one ExperimentResult per experiment - the one with the best pVal/tStat rank
            // e.g. if experiment was present twice in bestExperiments (once for UP and once for DOWN)
            // we want to choose the one occurrence with the better best pVal/tStat rank
            Set<ExperimentResult> uniqueBestExperiments = new LinkedHashSet<ExperimentResult>(bestExperiments);

            return new ArrayList<ExperimentResult>(uniqueBestExperiments);

        } else {
            StatisticsQueryCondition statsQuery = new StatisticsQueryCondition(Collections.singleton(bioEntityId), statType);
            statsQuery.and(getStatisticsOrQuery(attributes, statType, 1));
            bestExperiments.addAll(getBestExperiments(statsQuery).values());
            return bestExperiments;
        }
    }

    /**
     * @param bioEntityId BioEntity of interest
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
        if (attribute.isEmpty()) {
            attrs = new ArrayList<Attribute>();
            attrs.addAll(getScoringEfsForBioEntity(bioEntityId, statType, null));
        } else {
            attrs = Collections.singletonList(attribute);
        }

        // Get best experiments, sorted by pval/tStat rank in which each experiment occurs only once
        List<ExperimentResult> bestExperiments = getBestExperiments(bioEntityId, attrs, statType);

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
    public List<EfAttribute> getScoringEfsForBioEntity(final Integer bioEntityId,
                                                       final StatisticsType statType,
                                                       @Nullable final String ef) {

        long timeStart = System.currentTimeMillis();
        List<EfAttribute> scoringEfs = new ArrayList<EfAttribute>();
        if (bioEntityId != null) {
            Set<EfAttribute> scoringEfAttrs = statisticsStorage.getScoringEfAttributesForBioEntity(bioEntityId, statType);
            for (EfAttribute efAttr : scoringEfAttrs) {
                if (efAttr != null && (ef == null || "".equals(ef) || ef.equals(efAttr.getEf()))) {
                    scoringEfs.add(new EfAttribute(efAttr.getEf()));
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
    public List<ExperimentInfo> getExperimentsForBioEntityAndAttribute(Integer bioEntityId, @Nullable EfAttribute attribute, StatisticsType statType) {
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
        for (EfvAttribute efvAttr : statisticsStorage.getAllEfvAttributes(statType)) {
            if ((autoFactors != null && !autoFactors.contains(efvAttr.getEf())) || efvAttr.getEfv() == null) {
                continue; // skip attribute if its factor is not of interest or it's an ef-only attribute
            }
            StatisticsQueryCondition statsQuery = new StatisticsQueryCondition(bioEntityIds, statType);
            statsQuery.and(getStatisticsOrQuery(Collections.<Attribute>singletonList(efvAttr), statType, 1));
            ArrayListMultimap<ExperimentInfo, Pair<StatisticsType, EfAttribute>> scoringExpsAttrs = ArrayListMultimap.create();
            StatisticsQueryUtils.getExperimentCounts(statsQuery, statisticsStorage, scoringExpsAttrs);
            Set<ExperimentInfo> scoringExps = scoringExpsAttrs.asMap().keySet();
            if (scoringExps.size() > 0) { // at least one bioEntityId in bioEntityIds had an experiment count > 0 for attr
                if (attrCounts != null)
                    attrCounts.add(efvAttr, scoringExps.size());
                for (ExperimentInfo exp : scoringExps) {
                    Set<String> efoTerms = statisticsStorage.getEfoTerms(efvAttr, exp);
                    if (scoringEfos != null)
                        scoringEfos.addAll(efoTerms);
                    else
                        log.debug("Skipping efo: {} for attr: {} and exp: {}", new Object[]{efoTerms, efvAttr, exp});
                }
            }
        }
    }

    /**
     * @param bioEntityIds
     * @param statType
     * @param autoFactors  set of factors of interest; if null, all factors are included
     * @return Unsorted set of non-zero experiment counts (for at least one of bioEntityIds and statType) per efv (note: not efo) attribute
     */
    public Multiset<EfvAttribute> getUnsortedScoringAttributesForBioEntities(
            Set<Integer> bioEntityIds,
            StatisticsType statType,
            @Nullable Collection<String> autoFactors) {
        Multiset<EfvAttribute> attrCounts = create();
        collectScoringAttributes(bioEntityIds, statType, autoFactors, attrCounts, null);
        return attrCounts;
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
     * @param bioEntityIds
     * @param statType
     * @param autoFactors  set of factors of interest; if null, all factors are included
     * @return Sorted set of non-zero experiment counts (for at least one of bioEntityIds and statType) per efv (note: not efo) attribute
     */
    public List<Multiset.Entry<EfvAttribute>> getSortedScoringAttributesForBioEntities(
            Set<Integer> bioEntityIds, StatisticsType statType, @Nullable Collection<String> autoFactors) {
        long timeStart = System.currentTimeMillis();

        Multiset<EfvAttribute> attrCounts =  getUnsortedScoringAttributesForBioEntities(bioEntityIds, statType, autoFactors);
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
        ArrayListMultimap<ExperimentInfo, Pair<StatisticsType, EfAttribute>> scoringExpsAttrs = ArrayListMultimap.create();
        StatisticsQueryUtils.getExperimentCounts(statsQuery, statisticsStorage, scoringExpsAttrs);
        return scoringExpsAttrs.asMap().keySet();
    }

    /**
     * @param attribute
     * @param allExpsToAttrs Map: ExperimentInfo -> Set<Attribute> to which mappings for an Attribute are to be added.
     */
    public void getAttributeToExperimentMappings(
            final Attribute attribute,
            Map<ExperimentInfo, Set<EfAttribute>> allExpsToAttrs) {
        attribute.getAttributeToExperimentMappings(statisticsStorage, allExpsToAttrs);
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

    /**
      * Populate bestExperimentsSoFar with an (unsorted) list of experiments with best pval/tstat rank, for statisticsQuery
      *
      * @param statisticsQuery
      * @return (unordered) Map of experiment id-> best pval/tstat rank for that experiment as a result of
      *         statisticsQuery against statisticsStorage
      */
     private Map<Long, ExperimentResult> getBestExperiments(StatisticsQueryCondition statisticsQuery) {
         // Use LinkedHashMap to preserve the order to experiments - as they are populated from bit index
         // in best-first order
         Map<Long, ExperimentResult> bestExperimentsMap = new LinkedHashMap<Long, ExperimentResult>();
         getBestExperimentsRecursive(statisticsQuery, bestExperimentsMap);
         return bestExperimentsMap;
     }

     /**
      * Populate bestExperimentsSoFar with an (unordered) Map of experiment id-> best pval/tstat rank for that
      * experiment as a result of statisticsQuery against statisticsStorage
      *
      * @param statisticsQuery
      * @param bestExperimentsSoFar
      */
     private void getBestExperimentsRecursive(
             StatisticsQueryCondition statisticsQuery,
             Map<Long, ExperimentResult> bestExperimentsSoFar) {
         Set<StatisticsQueryOrConditions<StatisticsQueryCondition>> andStatisticsQueryConditions = statisticsQuery.getConditions();


         if (andStatisticsQueryConditions.isEmpty()) { // End of recursion
             Set<Integer> bioEntityIdRestrictionSet = statisticsQuery.getBioEntityIdRestrictionSet();

            Set<EfAttribute> attributes = statisticsQuery.getAttributes();
             Set<ExperimentInfo> experiments = statisticsQuery.getExperiments();

             for (EfAttribute attr : attributes) {

                 SortedMap<PTRank, Map<ExperimentInfo, ConciseSet>> pValToExpToGenes =
                         statisticsStorage.getPvalsTStatRanksForAttribute(attr, statisticsQuery.getStatisticsType());

                 if (pValToExpToGenes != null) {
                     for (Map.Entry<PTRank, Map<ExperimentInfo, ConciseSet>> pValToExpToGenesEntry : pValToExpToGenes.entrySet()) {
                         Map<ExperimentInfo, ConciseSet> expToGenes = pValToExpToGenesEntry.getValue();
                         if (expToGenes != null) {
                             for (Map.Entry<ExperimentInfo, ConciseSet> expToGenesEntry : expToGenes.entrySet()) {
                                 if (experiments.isEmpty() || experiments.contains(expToGenesEntry.getKey())) {
                                     if (StatisticsQueryUtils.containsAtLeastOne(expToGenesEntry.getValue(), bioEntityIdRestrictionSet)) {
                                         // If best experiments are collected for an (OR) group of genes, pVal/tStat
                                         // for any of these genes will be considered here
                                         ExperimentResult expCandidate = new ExperimentResult(expToGenesEntry.getKey(), attr, pValToExpToGenesEntry.getKey());
                                         tryAddOrReplaceExperiment(expCandidate, bestExperimentsSoFar);
                                     }
                                 }
                             }
                         }
                     }
                 }
             }
         } else {
             // We only expect one 'AND' condition with set of orConditions inside
             StatisticsQueryOrConditions<StatisticsQueryCondition> orConditions = andStatisticsQueryConditions.iterator().next();
             if (orConditions != null) {
                 for (StatisticsQueryCondition orCondition : orConditions.getConditions()) {
                     // Pass gene restriction set down to orCondition
                     orCondition.setBioEntityIdRestrictionSet(orConditions.getBioEntityIdRestrictionSet());
                     getBestExperimentsRecursive(orCondition, bestExperimentsSoFar);
                 }
             }
         }
     }

        /**
     * If exp cannot be found in exps, add it to exps
     * If it can be found and its pVal/tStat ranks are worse the one is exps, replace it in exps
     *
     * @param exp
     * @param exps
     */
    private static void tryAddOrReplaceExperiment(ExperimentResult exp, Map<Long, ExperimentResult> exps) {
        long expId = exp.getExperimentId();
        ExperimentResult existingExp = exps.get(expId);
        if (existingExp != null) {
            if (exp.getPValTStatRank().compareTo(existingExp.getPValTStatRank()) < 0) {
                exps.put(expId, exp);
            }
        } else {
            exps.put(expId, exp);
        }
    }

}
