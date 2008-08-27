package ae3.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ae3.model.AtlasGene;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author pashky
 */
public class AtlasStructuredQueryResult {
    protected final Log log = LogFactory.getLog(getClass());

    public static class Condition {
        private AtlasStructuredQuery.Condition queryCondition;
        private List<String> expandedFactorValues;

        public AtlasStructuredQuery.Expression getExpression() {
            return queryCondition.getExpression();
        }

        public String getFactor() {
            return queryCondition.getFactor();
        }

        public Condition(AtlasStructuredQuery.Condition queryCondition, Collection<String> expandedFactorValues) {
            this.queryCondition = queryCondition;
            this.expandedFactorValues = new ArrayList<String>(expandedFactorValues);
        }

        public List<String> getFactorValues() {
            return expandedFactorValues;
        }
    }

    static public class UpdownCounter {
        private int ups;
        private int downs;
        private double mpvup;
        private double mpvdn;
        private String factor;
        private String factorValue;

        public UpdownCounter(int ups, int downs, double mpvup, double mpvdn, final String factor, final String factorValue) {
            this.ups = ups;
            this.downs = downs;
            this.mpvup = mpvup;
            this.mpvdn = mpvdn;
            this.factor = factor;
            this.factorValue = factorValue;
        }

        public int getUps() {
            return ups;
        }

        public int getDowns() {
            return downs;
        }

        public double getMpvUp() {
            return mpvup;
        }

        public double getMpvDn() {
            return mpvdn;
        }

        public String getFactor() {
            return factor;
        }

        public String getFactorValue() {
            return factorValue;
        }
    }

    static public class GeneResult {
        private AtlasGene gene;
        private List<UpdownCounter> updownCounters;

        public GeneResult(AtlasGene gene, List<UpdownCounter> updownCounters) {
            this.gene = gene;
            this.updownCounters = updownCounters;
        }

        public AtlasGene getGene() {
            return gene;
        }

        public void setGene(AtlasGene gene) {
            this.gene = gene;
        }

        public List<UpdownCounter> getCounters() {
            return updownCounters;
        }

        public void setCounters(List<UpdownCounter> updownCounters) {
            this.updownCounters = updownCounters;
        }
    }

    List<Condition> conditions;
    List<GeneResult> results;

    public AtlasStructuredQueryResult(List<Condition> conditions) {
        this.conditions = conditions;
        this.results = new ArrayList<GeneResult>();
    }

    public void addResult(GeneResult result) {
        results.add(result);
    }

    public int getSize() {
        return results.size();
    }

    public List<GeneResult> getResults() {
        return results;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

}
