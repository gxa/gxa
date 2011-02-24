package uk.ac.ebi.gxa.statistics;

import uk.ac.ebi.gxa.efo.Efo;

import java.util.Map;
import java.util.Set;

/**
 * Abstract representation for ef-efv/efo Attributes used in querying bit index
 */
public abstract class Attribute {

    protected String value;
    protected StatisticsType statType;

    /**
     *
     * @param efo
     * @return Set containing this Attribute (and all its children if applicable - c.f. EfoAttribute)
     */
    public abstract Set<Attribute> getAttributeAndChildren(Efo efo);

    /**
     * @param statisticsStorage - used to obtain indexes of attributes and experiments, needed finding experiment counts in bit index
     * @param allExpsToAttrs    Map: Experiment -> Set<Attribute> to which mappings for an Attribute are to be added.
     */
    public abstract void getEfvExperimentMappings(
            final StatisticsStorage<Long> statisticsStorage,
            Map<Experiment, Set<EfvAttribute>> allExpsToAttrs
    );

    public String getValue() {
        return value;
    }

    public void setStatType(StatisticsType statType) {
        this.statType = statType;
    }

    public StatisticsType getStatType() {
        return statType;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Attribute attribute = (Attribute) o;

        if (value != null ? !value.equals(attribute.value) : attribute.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }


}
