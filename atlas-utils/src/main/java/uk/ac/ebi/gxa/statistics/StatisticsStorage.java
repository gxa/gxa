package uk.ac.ebi.gxa.statistics;

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

    private static final long serialVersionUID = 8694787073607623042L;

    Map<StatisticsType, Statistics> stats = new EnumMap<StatisticsType, Statistics>(StatisticsType.class);

    // Index mapping Long gene ids to (ConciseSet-storable) Integer values
    private ObjectIndex<GeneIdType> geneIndex;
    // Index mapping Experiment objects to unique Integer values - to reduce space consumption by each Statistics object
    private ObjectIndex<Experiment> experimentIndex;
    // Index mapping Attributes to unique Integer values - to reduce space consumption by each Statistics object
    private ObjectIndex<Attribute> attributeIndex;
    // Map efo term -> Pair (Attribute index, Experiment index)
    private EfoIndex efoIndex;


    public void setExperimentIndex(ObjectIndex<Experiment> experimentIndex) {
        this.experimentIndex = experimentIndex;
    }

    public ObjectIndex<Experiment> getExperimentIndex() {
        return experimentIndex;
    }

    public void setGeneIndex(ObjectIndex<GeneIdType> objectIndex) {
        this.geneIndex = objectIndex;
    }

    public ObjectIndex<GeneIdType> getGeneIndex() {
        return geneIndex;
    }

    public void setAttributeIndex(ObjectIndex<Attribute> objectIndex) {
        this.attributeIndex = objectIndex;
    }

    public ObjectIndex<Attribute> getAttributeIndex() {
        return attributeIndex;
    }

    public void setEfoIndex(EfoIndex efoIndex) {
        this.efoIndex = efoIndex;
    }

    public EfoIndex getEfoIndex() {
        return efoIndex;
    }

    public void addStatistics(StatisticsType statisticsType, Statistics stats) {
        this.stats.put(statisticsType, stats);
    }

    public Statistics getStatsForType(StatisticsType statisticsType) {
        return stats.get(statisticsType);
    }
}

