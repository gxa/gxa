package ae3.service.structuredquery;

/**
 * @author pashky
 */
public class ExpFactorQueryCondition extends QueryCondition {
    private Expression expression;
    
    /**
     * Returns gene expression type
     * @return gene expression type
     */
    public Expression getExpression() {
        return expression;
    }

    /**
     * Sets gene expression type
     * @param expression gene expression type
     */
    public void setExpression(Expression expression) {
        this.expression = expression;
    }
}
