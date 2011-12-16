package ae3.service;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import org.springframework.beans.factory.DisposableBean;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.IndexBuilderEventHandler;
import uk.ac.ebi.gxa.statistics.*;
import uk.ac.ebi.gxa.utils.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.*;

/**
 * This interface provides bioentity expression statistics query service API
 */
public interface AtlasStatisticsQueryService extends IndexBuilderEventHandler, DisposableBean {

    public void setAtlasIndex(File atlasIndexDir);

    public void setIndexBuilder(IndexBuilder indexBuilder);

    public void setStatisticsStorage(StatisticsStorage statisticsStorage);

    public void setEfo(Efo efo);

    /**
     * @param attribute
     * @param bioEntityId
     * @param statType
     * @return ExperimentInfo count for statisticsType, attributes and bioEntityId
     */
    public Integer getExperimentCountsForBioEntity(
            final Attribute attribute,
            final Integer bioEntityId,
            final StatisticsType statType);

    /**
     * @param attribute
     * @param bioEntityId
     * @param statType
     * @param bioEntityIdRestrictionSet
     * @param scoresCache
     * @return ExperimentInfo count for statisticsType, attributes and geneId
     */
    public Integer getExperimentCountsForBioEntity(
            final Attribute attribute,
            final Integer bioEntityId,
            final StatisticsType statType,
            Set<Integer> bioEntityIdRestrictionSet,
            Map<StatisticsType, HashMap<String, Multiset<Integer>>> scoresCache);

    /**
     * @param statsQuery
     * @return A map containing
     *         - scoring experiments as keys
     *         - Collections of scoring statistics type-attribute pairs in experiment key as values
     */
    public Multimap<ExperimentInfo, Pair<StatisticsType, EfAttribute>> getScoringExpsAttrs(
            final StatisticsQueryCondition statsQuery);

    /**
     * @param orAttributes
     * @param statType
     * @param minExperiments
     * @return StatisticsQueryOrConditions, including children of all efo's in orAttributes
     */
    public StatisticsQueryOrConditions<StatisticsQueryCondition> getStatisticsOrQuery(
            final Collection<? extends Attribute> orAttributes,
            final StatisticsType statType,
            int minExperiments);


    /**
     * @param statsQuery
     * @param geneIds
     * @return A subset of geneIds that is scoring according to the statsQuery result
     */
    public Set<Integer> restrictGenesByStatsQuery(final StatisticsQueryCondition statsQuery,
                                                  final Set<Integer> geneIds);

    /**
     * @param statsQuery
     * @param minPos
     * @param rows
     * @param bioEntityIdRestrictionSet Set of BioEntity ids to restrict the query before sorting
     * @param sortedBioEntitiesChunk    - a chunk of the overall sorted (by experiment counts - in desc order) list of bioentities,
     *                                  starting from 'minPos' and containing maximums 'rows' bioentities
     * @return Pair<The overall number of bioentities for which counts exist in statsQuery, total experiment count for returned genes>
     */
    public Pair<Integer, Integer> getSortedBioEntities(
            final StatisticsQueryCondition statsQuery,
            final int minPos,
            final int rows,
            final Set<Integer> bioEntityIdRestrictionSet,
            List<Integer> sortedBioEntitiesChunk);

 /**
     * @param bioEntityIds
     * @param statType
     * @param autoFactors  set of factors of interest; if null, all factors are included
     * @return Set of efv's with non-zero statType experiment counts for bioEntityIds
     */
    public  Multiset<EfvAttribute> getUnsortedScoringAttributesForBioEntities(
            Set<Integer> bioEntityIds,
            StatisticsType statType,
            @Nullable Collection<String> autoFactors);

    /**
     * @param bioEntityIds
     * @param statType
     * @param autoFactors  set of factors of interest
     * @return Sorted set of non-zero experiment counts (for at least one of bioEntityIds and statType) per efo/efv attribute
     */
    public List<Multiset.Entry<EfvAttribute>> getSortedScoringAttributesForBioEntities(
            Set<Integer> bioEntityIds,
            StatisticsType statType,
            Collection<String> autoFactors);

    /**
     * @param bioEntityIds
     * @param statType
     * @return Set of efo's with non-zero experiment counts for bioEntityIds and statType
     */
    public Set<String> getScoringEfosForBioEntities(Set<Integer> bioEntityIds, StatisticsType statType);

    /**
     * @param bioEntityId
     * @param attribute
     * @param statType
     * @return Set of Experiments in which bioEntityId-ef-efv have statType expression
     */
    public Set<ExperimentInfo> getScoringExperimentsForBioEntityAndAttribute(
            final Integer bioEntityId,
            @Nonnull Attribute attribute,
            final StatisticsType statType);

    /**
     * @param efoTerm
     * @return Set of Attributes corresponding to efoTerm. Note that efo's map to ef-efv-experiment triples. However, this method
     *         is used in AtlasStructuredQueryService for populating list view, which for efo queries shows ef-efvs those efos map to and
     *         _all_ experiments in which these ef-efvs have expressions. In other words, we don't restrict experiments shown in the list view
     *         to just those in query efo->ef-efv-experiment mapping.
     */
    public Set<EfvAttribute> getAttributesForEfo(String efoTerm);

    /**
     *
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
            final StatisticsType statType);


    /**
     * @param bioEntityId
     * @param statType
     * @param ef
     * @return list all efs for which bioEntityId has statType expression in at least one experiment
     */
    public List<EfAttribute> getScoringEfsForBioEntity(final Integer bioEntityId,
                                                  final StatisticsType statType,
                                                  @Nullable final String ef);

    /**
     * @param bioEntityId
     * @param statType
     * @return list all efs for which bioEntityId has statType expression in at least one experiment
     */
    public List<EfvAttribute> getScoringEfvsForBioEntity(final Integer bioEntityId,
                                                         final StatisticsType statType);

    /**
     * @param attribute
     * @param bioEntityId
     * @param statType
     * @return unsorted list of experiments for which bioEntityId has statType expression for attribute
     */
    public List<ExperimentInfo> getExperimentsForBioEntityAndAttribute(Integer bioEntityId,
                                                                       @Nullable EfAttribute attribute,
                                                                       StatisticsType statType);

    /**
     * @param attribute
     * @param allExpsToAttrs Map: ExperimentInfo -> Set<Attribute> to which mappings for an Attribute are to be added.
     */
    public void getAttributeToExperimentMappings(
            final Attribute attribute,
            Map<ExperimentInfo, Set<EfAttribute>> allExpsToAttrs);

    /**
     * @param attribute
     * @param statType
     * @return the amount of bioentities with expression statType for efv attribute
     */
    public int getBioEntityCountForEfvAttribute(EfvAttribute attribute, StatisticsType statType);

    /**
     * @param attribute
     * @param statType
     * @return the amount of bioentities with expression statType for efo attribute
     */
    public int getBioEntityCountForEfoAttribute(Attribute attribute, StatisticsType statType);

    /**
     * @param efoTerm
     * @return the total count of experiment-attribute mappings for efoTerm. A measure of how expensive a given efoTerm will be
     *         to search against bit index. Currently used just for logging.
     */
    public int getMappingsCountForEfo(String efoTerm);

}
