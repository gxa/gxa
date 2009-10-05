package ae3.service.structuredquery;

/**
 * @author pashky
 */
public class GeneQueryCondition extends QueryCondition {
     private boolean negated;

    public boolean isNegated() {
        return negated;
    }

    public void setNegated(boolean negated) {
        this.negated = negated;
    }
}
