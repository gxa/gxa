package ae3.service;


import com.google.common.collect.Multiset;
import org.springframework.beans.factory.DisposableBean;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.IndexBuilderEventHandler;
import uk.ac.ebi.gxa.statistics.*;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: Nov 2, 2010
 * Time: 5:27:03 PM
 * This interface provides gene expression statistics query service API
 */
public interface AtlasStatisticsQueryService extends IndexBuilderEventHandler, DisposableBean {

    public void setAtlasIndex(File atlasIndexDir);

    public void setIndexBuilder(IndexBuilder indexBuilder);

    public void setStatisticsStorage(StatisticsStorage<Long> statisticsStorage);

    public void setEfo(Efo efo);

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
            Long geneId);

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
            HashMap<String, Multiset<Integer>> scoresCacheForStatType);

    /**
     * @param orAttributes
     * @return StatisticsQueryOrConditions, including children of all efo's in orAttributes
     */
    public StatisticsQueryOrConditions<StatisticsQueryCondition> getStatisticsOrQuery(List<Attribute> orAttributes);

    /**
     *
     * @param geneId
     * @return Index of geneId within bit index
     */
    public Integer getIndexForGene(Long geneId);

    /**
     *
     * @param attribute
     * @return Index of Attribute within bit index
     */
    public Integer getIndexForAttribute(Attribute attribute);

    /**
     * @param attributeIndex
     * @return Attribute corresponding to attributeIndex bit index
     */
    public Attribute getAttributeForIndex(Integer attributeIndex);


    /**
     * @param statsQuery
     * @param minPos
     * @param rows
     * @param sortedGenesChunk - a chunk of the overall sorted (by experiment counts - in desc order) list of genes,
     *                         starting from 'minPos' and containing maximums 'rows' genes
     * @return The overall number of genes for which counts exist in statsQuery
     */
    public Integer getSortedGenes(
            final StatisticsQueryCondition statsQuery,
            final int minPos,
            final int rows,
            List<Long> sortedGenesChunk);

 /**
     * @param geneIds
     * @param statType
     * @param autoFactors set of factors of interest
     * @return Serted set of non-zero experiment counts (for at least one of geneIds and statType) per efo/efv attribute
     */
    public List<Multiset.Entry<Integer>> getScoringAttributesForGenes(
            Set<Long> geneIds,
            StatisticsType statType,
            Collection<String> autoFactors);

    /**
     * @param geneId
     * @param statType
     * @param ef
     * @param efv
     * @return Set of Experiments in which geneId-ef-efv have statType expression
     */
    public Set<Experiment> getScoringExperimentsForGeneAndAttribute(
            Long geneId,
            StatisticsType statType,
            String ef,
            @Nullable String efv);


    /**
     * @param efoTerm
     * @return Set of Attributes corresponding to efoTerm. Note that efo's map to ef-efv-experiment triples. However, this method
     *         is used in AtlasStructuredQueryService for populating list view, which for efo queries shows ef-efvs those efos map to and
     *         _all_ experiments in which these ef-efvs have expressions. In other words, we don't restrict experiments shown in the list view
     *         to just those in query efo->ef-efv-experiment mapping.
     */
    public Set<Attribute> getAttributesForEfo(String efoTerm);

    /**
     * @param geneId   Gene of interest
     * @param statType StatisticsType
     * @param ef
     * @param efv
     * @param isEfo    if isEfo == StatisticsQueryUtils.EFO, efv is taken as an efo term
     * @param fromRow  Used for paginating of experiment plots on gene page
     * @param toRow    ditto
     * @return List of Experiments sorted by pVal/tStat ranks from best to worst
     */
    public List<Experiment> getExperimentsSortedByPvalueTRank(
            final Long geneId,
            final StatisticsType statType,
            @Nullable final String ef,
            @Nullable final String efv,
            final boolean isEfo,
            final int fromRow,
            final int toRow);


    /**
     * @param geneId
     * @param statType
     * @param ef
     * @return list all efs for which geneId has statType expression in at least one experiment
     */
    public List<String> getScoringEfsForGene(final Long geneId,
                                             final StatisticsType statType,
                                             @Nullable final String ef);

    /**
     * @param geneId
     * @param statType
     * @return list all efs for which geneId has statType expression in at least one experiment
     */
    public List<Attribute> getScoringEfvsForGene(final Long geneId,
                                                 final StatisticsType statType);

    /**
     * @param ef
     * @param geneId
     * @param statType
     * @return unsorted list of experiments for which geneId has statType expression for ef attr
     */
    public List<Experiment> getExperimentsForGeneAndEf(Long geneId,
                                                       @Nullable String ef,
                                                       StatisticsType statType);

    /**
     * @param geneId
     * @param ef
     * @param efv
     * @param isEfo
     * @param statType
     * @return List of attribute(s) corresponding to ef-efv (isEfo == false), efv (isEfo == true) or all up/down scoring ef-efvs for geneid
     */
    public List<Attribute> getAttributes(Long geneId,
                                         @Nullable String ef,
                                         @Nullable String efv,
                                         boolean isEfo,
                                         StatisticsType statType);

}
