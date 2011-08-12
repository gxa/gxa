package uk.ac.ebi.gxa.statistics;

import uk.ac.ebi.gxa.efo.Efo;

import java.util.Map;
import java.util.Set;

/**
 * Abstract representation for ef-efv/efo Attributes used in querying bit index
 */
public abstract class Attribute {

    private transient final StatisticsType statType;

    /**
     * Note: Default constructor is required for serializable descendants of the abstract class.
     */
    protected Attribute() {
        this.statType = null;
    }

    protected Attribute(StatisticsType statType) {
        this.statType = statType;
    }

    /**
     * @param efo
     * @return Set containing this Attribute (and all its children if applicable - c.f. EfoAttribute)
     */
    public abstract Set<Attribute> getAttributeAndChildren(Efo efo);

    /**
     * @param statisticsStorage - used to obtain indexes of attributes and experiments, needed finding experiment counts in bit index
     * @param allExpsToAttrs    Map: ExperimentInfo -> Set<Attribute> to which mappings for an Attribute are to be added.
     */
    public abstract void getEfvExperimentMappings(
            final StatisticsStorage statisticsStorage,
            Map<ExperimentInfo, Set<EfvAttribute>> allExpsToAttrs
    );

    public abstract String getValue();

    /**
     * TODO: Temporary solution to make Attribute immutable. See ticket #3048
     * @param statType new statistics type
     * @return a copy of current object but with new statistics type
     */
    public abstract Attribute withStatType(StatisticsType statType);

    public StatisticsType getStatType() {
        return statType;
    }

    @Override
    public String toString() {
        return getValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Attribute attribute = (Attribute) o;

        return getValue() == null ? attribute.getValue() == null : getValue().equals(attribute.getValue());
    }

    @Override
    public int hashCode() {
        return getValue() != null ? getValue().hashCode() : 0;
    }


    public boolean isEmpty() {
        return getValue() == null;
    }
}
