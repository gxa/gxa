package ae3.model;

/**
 * Expression matrxi interface
 * @author pashky
 */
public interface ExpressionMatrix {
    /**
     * Returns expression for design element in assay position
     * @param designElementId design element id
     * @param assayPos assay's position in matrix
     * @return expression value
     */
    double getExpression(int designElementId, int assayPos);
}
