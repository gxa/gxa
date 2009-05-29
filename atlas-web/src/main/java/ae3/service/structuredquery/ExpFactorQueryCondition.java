package ae3.service.structuredquery;

/**
 * @author pashky
 */
public class ExpFactorQueryCondition extends QueryCondition {
    private QueryExpression expression;
    
    /**
     * Returns gene expression type
     * @return gene expression type
     */
    public QueryExpression getExpression() {
        return expression;
    }

    /**
     * Sets gene expression type
     * @param expression gene expression type
     */
    public void setExpression(QueryExpression expression) {
        this.expression = expression;
    }
}
