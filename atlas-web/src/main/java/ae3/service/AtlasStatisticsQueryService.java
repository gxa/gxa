package ae3.service;

import com.google.common.collect.Multiset;
import org.springframework.beans.factory.DisposableBean;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.IndexBuilderEventHandler;
import uk.ac.ebi.gxa.statistics.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.*;

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
     * @param attribute
     * @param geneId
     * @return Experiment count for statisticsType, attributes and geneId
     */
    public Integer getExperimentCountsForGene(
            Attribute attribute,
            Long geneId);

    /**
     * @param attribute
     * @param geneId
     * @return Experiment count for statisticsType, attributes and geneId
     */
    public Integer getExperimentCountsForGene(
            Attribute attribute,
            Long geneId,
            Set<Long> geneRestrictionSet,
            HashMap<String, Multiset<Integer>> scoresCacheForStatType);

    /**
     * @param orAttributes
     * @return StatisticsQueryOrConditions, including children of all efo's in orAttributes
     */
    public StatisticsQueryOrConditions<StatisticsQueryCondition> getStatisticsOrQuery(List<Attribute> orAttributes);

    /**
     * @param geneId
     * @return Index of geneId within bit index
     */
    public Integer getIndexForGene(Long geneId);

    /**
     * @param attribute
     * @return Index of Attribute within bit index
     */
    public Integer getIndexForAttribute(EfvAttribute attribute);

    /**
     * @param attributeIndex
     * @return Attribute corresponding to attributeIndex bit index
     */
    public EfvAttribute getAttributeForIndex(Integer attributeIndex);


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
     * @param attribute
     * @return Set of Experiments in which geneId-ef-efv have statType expression
     */
    public Set<Experiment> getScoringExperimentsForGeneAndAttribute(
            Long geneId, @Nonnull Attribute attribute);


    /**
     * @param efoTerm
     * @return Set of Attributes corresponding to efoTerm. Note that efo's map to ef-efv-experiment triples. However, this method
     *         is used in AtlasStructuredQueryService for populating list view, which for efo queries shows ef-efvs those efos map to and
     *         _all_ experiments in which these ef-efvs have expressions. In other words, we don't restrict experiments shown in the list view
     *         to just those in query efo->ef-efv-experiment mapping.
     */
    public Set<EfvAttribute> getAttributesForEfo(String efoTerm);

    /**
     * @param geneId    Gene of interest
     * @param attribute Attribute
     * @param fromRow   Used for paginating of experiment plots on gene page
     * @param toRow     ditto
     * @return List of Experiments sorted by pVal/tStat ranks from best to worst
     */
    public List<Experiment> getExperimentsSortedByPvalueTRank(
            final Long geneId,
            final Attribute attribute,
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
    public List<EfvAttribute> getScoringEfvsForGene(final Long geneId,
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
     * @param attribute
     * @param allExpsToAttrs Map: Experiment -> Set<Attribute> to which mappings for an Attribute are to be added.
     */
    public void getEfvExperimentMappings(
            final Attribute attribute,
            Map<Experiment, Set<EfvAttribute>> allExpsToAttrs);

    /**
     * @param statType
     * @return Collection of unique expriments with expressions fro statType
     */
    public Collection<Experiment> getScoringExperiments(StatisticsType statType);

    /**
     * @param attribute
     * @param statType
     * @return the amount of genes with expression statType for efv attribute
     */
    public int getGeneCountForEfvAttribute(EfvAttribute attribute, StatisticsType statType);

    /**
     * @param attribute
     * @param statType
     * @return the amount of genes with expression statType for efo attribute
     */
    public int getGeneCountForEfoAttribute(Attribute attribute, StatisticsType statType);
}
