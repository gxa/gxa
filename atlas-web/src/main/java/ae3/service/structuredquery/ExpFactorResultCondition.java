package ae3.service.structuredquery;

import ae3.service.structuredquery.EfoValueListHelper.EfoTermCount;
import java.util.Collection;
import java.util.List;

/**
     * Structured query condition expanded by query service
 */
public class ExpFactorResultCondition {
    private ExpFactorQueryCondition condition;
    private boolean ignored;
    private Collection<List<EfoValueListHelper.EfoTermCount>> efoPaths;

    /**
     * Constructor for condition
     * @param condition original query condition
     * @param expansion EFV expansion tree
     */
    public ExpFactorResultCondition(ExpFactorQueryCondition condition, Collection<List<EfoValueListHelper.EfoTermCount>> efoPaths, boolean ignored) {
        this.condition = condition;
        this.efoPaths = efoPaths;
        this.ignored = ignored;
    }

    /**
     * Returns gene expression type
     * @return gene expression type
     */
    public QueryExpression getExpression() {
        return condition.getExpression();
    }

    /**
     * Returns factor
     * @return factor name
     */
    public String getFactor() {
        return condition.getFactor();
    }

    /**
     * Returns factor values
     * @return iterable factor values
     */
    public Iterable<String> getFactorValues() {
        return condition.getFactorValues();
    }

    /**
     * Returns concatenated quoted factor values
     * @return string factor values
     */
    public String getJointFactorValues() {
        return condition.getJointFactorValues();
    }

    /**
     * Get EFO paths for condition
     * @return
     */
    public Collection<List<EfoTermCount>> getEfoPaths() {
        return efoPaths;
    }


    /**
     * Convenience method to check whether conditions is for any factor
     * @return true if any factor
     */
    public boolean isAnyFactor() {
        return condition.isAnyFactor();
    }

    /**
     * Convenience method to check whether conditions is for any value
     * @return true if any value contains '*' or all values are empty
     */
    public boolean isAnyValue() {
        return condition.isAnyValue();
    }

    /**
     * Convenience method to check whether condition is for anything (any value and any factor)
     * @return true or false
     */
    public boolean isAnything() {
        return condition.isAnything();
    }

    /**
     * Returns if this condition was ignored in query
     * @return true or false
     */
    public boolean isIgnored() {
        return ignored;
    }
}
