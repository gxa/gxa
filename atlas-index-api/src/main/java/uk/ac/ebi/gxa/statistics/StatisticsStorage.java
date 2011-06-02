package uk.ac.ebi.gxa.statistics;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import it.uniroma3.mat.extendedset.ConciseSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.*;

import static uk.ac.ebi.gxa.statistics.StatisticsType.*;

/**
 * Class encapsulating bit storage of all statistics in StatisticType enum
 */
public class StatisticsStorage implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(StatisticsStorage.class);
    private static final long serialVersionUID = 4119074256514570379L;

    // Map: StatisticsType -> Statistics (Statistics class contains experiment counts for bioEntityIds, in experiments in experimentIndex
    // and attributes in attributeIndex (see below))
    private Map<StatisticsType, Statistics> stats = new EnumMap<StatisticsType, Statistics>(StatisticsType.class);

    // Index mapping Experiment objects to unique Integer values - to reduce space consumption by each Statistics object
    private ObjectIndex<ExperimentInfo> experimentIndex;
    // Index mapping Attributes to unique Integer values - to reduce space consumption by each Statistics object
    private ObjectIndex<EfvAttribute> attributeIndex;
    // Map efo term -> ExperimentInfo index -> Set<Attribute Index>
    // Map Attribute index -> ExperimentInfo Index -> efo term
    private EfoIndex efoIndex;


    // Setter methods

    public void addStatistics(StatisticsType statisticsType, Statistics stats) {
        this.stats.put(statisticsType, stats);
    }

    public void setExperimentIndex(ObjectIndex<ExperimentInfo> experimentIndex) {
        this.experimentIndex = experimentIndex;
    }

    public void setAttributeIndex(ObjectIndex<EfvAttribute> objectIndex) {
        this.attributeIndex = objectIndex;
    }

    public void setEfoIndex(EfoIndex efoIndex) {
        this.efoIndex = efoIndex;
    }

    // Experiment-related getter methods

    /**
     * @param index
     * @return A clone of ExperimentInfo object stored in experimentIndex
     */
    public ExperimentInfo getExperimentForIndex(Integer index) {
        ExperimentInfo experiment = experimentIndex.getObjectForIndex(index);
        if (experiment != null) {
            return new ExperimentInfo(experiment.getAccession(), experiment.getExperimentId());
        }
        return null;
    }

    Collection<ExperimentInfo> getExperimentsForIndexes(Collection<Integer> indexes) {
        List<ExperimentInfo> result = new ArrayList<ExperimentInfo>();
        for (Integer expIndex : indexes) {
            ExperimentInfo exp = getExperimentForIndex(expIndex);
            if (exp != null)
                result.add(exp);
        }
        return result;
    }

    public Integer getIndexForExperiment(ExperimentInfo experiment) {
        return experimentIndex.getIndexForObject(experiment);
    }

    // Attribute-related getter methods

    /**
     * @param index
     * @return A clone of EfvAttribute object stored in attributeIndex
     */
    public EfvAttribute getAttributeForIndex(Integer index) {
        EfvAttribute attribute = attributeIndex.getObjectForIndex(index);
        if (attribute != null) {
            return new EfvAttribute(attribute.getEf(), attribute.getEfv(), attribute.getStatType());
        }
        return null;
    }

    public Integer getIndexForAttribute(EfvAttribute attribute) {
        return attributeIndex.getIndexForObject(attribute);
    }

    public Map<Integer, ConciseSet> getStatisticsForAttribute(Integer attributeIndex, StatisticsType statType) {
        return stats.get(statType).getStatisticsForAttribute(attributeIndex);
    }

    /**
     * Delegates call to Statistics object corresponding to statType
     *
     * @param attributeIndex
     * @param bioEntityId
     * @param statType
     * @return Set of indexes of experiments with non-zero statType counts for attributeIndex-bioEntityId tuple
     */
    public Set<Integer> getExperimentsForBioEntityAndAttribute(Integer attributeIndex, Integer bioEntityId, StatisticsType statType) {
        return stats.get(statType).getExperimentsForBioEntityAndAttribute(attributeIndex, bioEntityId);
    }

    /**
     * Delegates call to Statistics object corresponding to statType
     *
     * @param bioEntityId
     * @param statType
     * @return Set of Ef-only attribute indexes that have statType up/down experiment counts for bioEntityId
     */
    public Set<Integer> getScoringEfAttributesForBioEntity(final Integer bioEntityId,
                                                           final StatisticsType statType) {
        return stats.get(statType).getScoringEfAttributesForBioEntity(bioEntityId);
    }

    /**
     * Delegates call to Statistics object corresponding to statType
     *
     * @param bioEntityId
     * @param statType
     * @return Set of Ef-only attribute indexes that have statType up/down experiment counts for bioEntityId
     */
    public Set<Integer> getScoringEfvAttributesForBioEntity(final Integer bioEntityId,
                                                            final StatisticsType statType) {
        return stats.get(statType).getScoringEfvAttributesForBioEntity(bioEntityId);
    }


    // Efo-related getter methods

    /**
     * @param efoTerm
     * @return Map: ExperimentInfo -> Set<EfvAttribute>, corresponding to efoterm
     */
    public Map<ExperimentInfo, Set<EfvAttribute>> getMappingsForEfo(String efoTerm) {
        Map<ExperimentInfo, Set<EfvAttribute>> result = new HashMap<ExperimentInfo, Set<EfvAttribute>>();
        Map<Integer, Set<Integer>> mappings = efoIndex.getMappingsForEfo(efoTerm);
        if (mappings != null) {
            for (Map.Entry<Integer, Set<Integer>> mapping : mappings.entrySet()) {
                ExperimentInfo exp = getExperimentForIndex(mapping.getKey());
                Set<EfvAttribute> attrs = new HashSet<EfvAttribute>();
                for (Integer attrIdx : mapping.getValue()) {
                    attrs.add(getAttributeForIndex(attrIdx));
                }
                result.put(exp, attrs);
            }
        }
        return result;
    }

    public Set<String> getEfos() {
        return efoIndex.getEfos();
    }

    public Multiset<Integer> getScoresAcrossAllEfos(StatisticsType statType) {
        return stats.get(statType).getScoresAcrossAllEfos();
    }

    /**
     * @param statType
     * @return Set of attributes or which experiment counts exist for statType
     */
    public Set<EfvAttribute> getAllAttributes(StatisticsType statType) {
        Set<EfvAttribute> attributes = new HashSet<EfvAttribute>();
        Set<Integer> attrIndexes = stats.get(statType).getAttributes();
        for (Integer attrIndex : attrIndexes) {
            attributes.add(getAttributeForIndex(attrIndex));
        }
        return attributes;
    }

    /**
     * @param attr
     * @param exp
     * @return efo term which maps to attr and exp
     */
    public String getEfoTerm(EfvAttribute attr, ExperimentInfo exp) {
        return efoIndex.getEfoTerm(getIndexForAttribute(attr), getIndexForExperiment(exp));
    }

    /**
     * @param attributeIndex
     * @param statType
     * @return pValue/tStat rank -> Experiment index -> ConciseSet of BioEntity ids, corresponding to attributeIndex and statType
     */
    public SortedMap<PvalTstatRank, Map<Integer, ConciseSet>> getPvalsTStatRanksForAttribute(Integer attributeIndex, StatisticsType statType) {
        return stats.get(statType).getPvalsTStatRanksForAttribute(attributeIndex);
    }

    /**
     * @param statType
     * @return Collection of unique expriments with expressions fro statType
     */
    public Collection<ExperimentInfo> getScoringExperiments(StatisticsType statType) {
        return Collections2.transform(stats.get(statType).getScoringExperiments(),
                new Function<Integer, ExperimentInfo>() {
                    public ExperimentInfo apply(@Nonnull Integer expIdx) {
                        return experimentIndex.getObjectForIndex(expIdx);
                    }
                });
    }

    /**
     * @param attribute
     * @param statType
     * @return the amount of BioEntities with expression represented by this object for attribute
     */
    public int getBioEntityCountForAttribute(EfvAttribute attribute, StatisticsType statType) {
        int bioEntityCount = 0;
        Integer attrIndex = attributeIndex.getIndexForObject(attribute);
        if (attrIndex != null)
            return stats.get(statType).getBioEntityCountForAttribute(attrIndex);
        return bioEntityCount;
    }

    /**
     * Populated all statistics in statisticsStorage with pre-computed scores for all genes across all efo's. These scores
     * are used in user queries containing no efv/efo conditions.
     */
    public void computeScoresAcrossAllEfos() {
        // Pre-computing UP stats scores for all genes across all efo's
        log.info("Pre-computing scores across all efo mappings for statistics: " + UP + "...");
        long start = System.currentTimeMillis();

        Multiset<Integer> upCounts = StatisticsQueryUtils.getScoresAcrossAllEfos(UP, this);
        setScoresAcrossAllEfos(upCounts, UP);
        log.info("Pre-computed scores across all efo mappings for statistics: " + UP + " in " + (System.currentTimeMillis() - start) + " ms");

        // Pre-computing DOWN stats scores for all genes across all efo's
        log.info("Pre-computing scores across all efo mappings for statistics: " + DOWN + "...");
        start = System.currentTimeMillis();
        Multiset<Integer> dnCounts = StatisticsQueryUtils.getScoresAcrossAllEfos(DOWN, this);
        setScoresAcrossAllEfos(dnCounts, DOWN);
        log.info("Pre-computed scores across all efo mappings for statistics: " + DOWN + " in " + (System.currentTimeMillis() - start) + " ms");

        // Pre-computing UP_DOWN stats scores for all genes across all efo's
        log.info("Pre-computing scores across all efo mappings for statistics: " + UP_DOWN + "...");
        start = System.currentTimeMillis();
        Multiset<Integer> upDnCounts = HashMultiset.create();
        upDnCounts.addAll(upCounts);
        upDnCounts.addAll(dnCounts);
        setScoresAcrossAllEfos(upDnCounts, UP_DOWN);
        log.info("Pre-computed scores across all efo mappings for statistics: " + UP_DOWN + " in " + (System.currentTimeMillis() - start) + " ms");

        // Pre-computing NON_D_E stats scores for all genes across all efo's
        log.info("Pre-computing scores across all efo mappings for statistics: " + NON_D_E + "...");
        start = System.currentTimeMillis();
        Multiset<Integer> nonDECounts = StatisticsQueryUtils.getScoresAcrossAllEfos(NON_D_E, this);
        setScoresAcrossAllEfos(nonDECounts, NON_D_E);
        log.info("Pre-computed scores across all efo mappings for statistics: " + NON_D_E + " in " + (System.currentTimeMillis() - start) + " ms");
    }

    private void setScoresAcrossAllEfos(Multiset<Integer> scores, StatisticsType statType) {
        stats.get(statType).setScoresAcrossAllEfos(scores);
    }
}

