package uk.ac.ebi.gxa.statistics;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import it.uniroma3.mat.extendedset.ConciseSet;

import java.io.Serializable;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: Oct 26, 2010
 * Time: 5:35:06 PM
 * Class encapsulating bit storage of all statistics in StatisticType enum
 */
public class StatisticsStorage<GeneIdType> implements Serializable {
    Map<StatisticsType, Statistics> stats = new EnumMap<StatisticsType, Statistics>(StatisticsType.class);

    // Index mapping Long gene ids to (ConciseSet-storable) Integer values
    private ObjectIndex<GeneIdType> geneIndex;
    // Index mapping Long experiment ids to Experiment objects - to reduce space consumption by each Statistics object
    private ObjectIndex<Experiment> experimentIndex;


    public void setExperimentIndex(ObjectIndex<Experiment> experimentIndex) {
        this.experimentIndex = experimentIndex;
    }

    public void addStatistics(StatisticsType statisticsType, Statistics stats) {
        this.stats.put(statisticsType, stats);
    }

    public void setGeneIndex(ObjectIndex<GeneIdType> objectIndex) {
        this.geneIndex = objectIndex;
    }


    public List<StatisticsResult<GeneIdType>> findForAttribute(final StatisticsType statType, final Attribute attribute) {
        Statistics statistics = stats.get(statType);

        if(statistics == null || statistics.getStatisticsForAttribute(attribute) == null)
            return Collections.emptyList();

        Map<GeneIdType,StatisticsResult<GeneIdType>> results = new HashMap<GeneIdType, StatisticsResult<GeneIdType>>();

        Map<Integer, ConciseSet> stats = statistics.getStatisticsForAttribute(attribute);
        for (Map.Entry<Integer, ConciseSet> stat : stats.entrySet()) {
            List<Integer> positions = new ArrayList<Integer>(stat.getValue());
            Collection<GeneIdType> foundGenes = geneIndex.getObjectsForIndexes(positions);
            Experiment experiment = experimentIndex.getObjectForIndex(stat.getKey());
            for (GeneIdType gene : foundGenes) {
                if(results.containsKey(gene)) {
                    results.get(gene).addExperiment(experiment);
                } else {
                    StatisticsResult<GeneIdType> result = new StatisticsResult<GeneIdType>(gene, attribute);
                    result.addExperiment(experiment);
                    results.put(gene, result);
                }
            }
        }

        List<StatisticsResult<GeneIdType>> foundStats = new ArrayList<StatisticsResult<GeneIdType>>();
        foundStats.addAll(results.values());
        Collections.sort(foundStats);
        return foundStats;
    }

    public Set<Attribute> listAttributes(final StatisticsType statType) {
        Statistics statistics = stats.get(statType);
        if (statistics == null)
            return Collections.emptySet();
        return Collections.unmodifiableSet(statistics.getAttributes());
    }

    public ConciseSet bitsForAttribute(final StatisticsType statType, final Attribute attribute) {
        Statistics statistics = stats.get(statType);

        ConciseSet bits = new ConciseSet();

        if (statistics == null || statistics.getStatisticsForAttribute(attribute) == null)
            return bits;

        Collection<ConciseSet> stats = statistics.getStatisticsForAttribute(attribute).values();
        for (ConciseSet stat : stats)
            bits = bits.union(stat);

        return bits;
    }


    public Multiset<GeneIdType> scoresForAttribute(final StatisticsType statType, final Attribute attribute) {

        Multiset<GeneIdType> set = HashMultiset.create();
        Statistics statistics = stats.get(statType);
        if (statistics == null)
            return set;

        Collection<ConciseSet> stats = statistics.getStatisticsForAttribute(attribute).values();

        for (ConciseSet stat : stats)
            set.addAll(geneIndex.getObjectsForIndexes(stat));
        return set;
    }

    public Multiset<GeneIdType> scoreForAttributes(final StatisticsType statType, final Set<Attribute> attributes) {

        Multiset<GeneIdType> set = HashMultiset.create();
        Statistics statistics = stats.get(statType);
        if (statistics == null)
            return set;

        for (Attribute attribute : attributes)
            set.addAll(scoresForAttribute(statType, attribute));

        return set;
    }

    public ConciseSet bitsForAttributes(final StatisticsType statType, final Set<Attribute> attributes) {
        ConciseSet bits = new ConciseSet();

        for (Attribute attribute : attributes)
            bits = bits.union(bitsForAttribute(statType, attribute));

        return bits;
    }

    public Set<Experiment> getExperimentsForAttribute(final StatisticsType statType, final Attribute attribute) {

        Statistics statistics = stats.get(statType);
        if (statistics == null)
            return Collections.unmodifiableSet(new HashSet<Experiment>());

        Set<Integer> experimentIndexes = statistics.getStatisticsForAttribute(attribute).keySet();
        Set<Experiment> set = new HashSet<Experiment>(experimentIndex.getObjectsForIndexes(experimentIndexes));
        return Collections.unmodifiableSet(set);
    }

    public Map<Experiment, ConciseSet> getStatisticsForAttribute(final StatisticsType statType, final Attribute attribute) {
        Map<Experiment, ConciseSet> expsToBits = new HashMap<Experiment, ConciseSet>();
        Statistics statistics = stats.get(statType);
        if (statistics != null) {


            Map<Integer, ConciseSet> expIndexToBits = statistics.getStatisticsForAttribute(attribute);
            for (Integer expIndex : expIndexToBits.keySet()) {
                expsToBits.put(experimentIndex.getObjectForIndex(expIndex), expIndexToBits.get(expIndex));
            }
        }
        return Collections.unmodifiableMap(expsToBits);
    }

}

