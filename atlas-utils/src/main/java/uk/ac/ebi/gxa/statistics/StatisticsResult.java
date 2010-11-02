package uk.ac.ebi.gxa.statistics;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: Oct 26, 2010
 * Time: 5:34:29 PM
 * Class representing Statistics result retrieved from StatisticsStorage
 */
public class StatisticsResult<GeneIdType> implements Comparable {
    private GeneIdType geneid;
    private Set<Experiment> experiments = new HashSet<Experiment>();
    private Attribute attribute;

    public StatisticsResult(GeneIdType geneid, Attribute attribute) {
        this.geneid = geneid;
        this.attribute = attribute;
    }

    public void addExperiment(Experiment experiment) {
        experiments.add(experiment);
    }

    public int compareTo(Object o) {
        if (this == o)
            return 0;

        StatisticsResult<GeneIdType> that = (StatisticsResult<GeneIdType>) o;

        if(this.experiments.size() < that.experiments.size())
            return 1;

        if(this.experiments.size() > that.experiments.size())
            return -1;

        return 0;
    }

    public GeneIdType getGene() {
        return geneid;
    }

    public Set<Experiment> getExperiments() {
        return experiments;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public void setAttribute(Attribute attribute) {
        this.attribute = attribute;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StatisticsResult that = (StatisticsResult) o;

        //if (attribute != null ? !attribute.equals(that.attribute) : that.attribute != null) return false;
        //if (experiments != null ? !experiments.equals(that.experiments) : that.experiments != null) return false;
        if (geneid != null ? !geneid.equals(that.geneid) : that.geneid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = geneid != null ? geneid.hashCode() : 0;
        //result = 31 * result + (experiments != null ? experiments.hashCode() : 0);
        //result = 31 * result + (attribute != null ? attribute.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "StatisticsResult{" +
                "geneid=" + geneid +
                ", experiments=" + experiments +
                ", attribute=" + attribute +
                '}';
    }
}
