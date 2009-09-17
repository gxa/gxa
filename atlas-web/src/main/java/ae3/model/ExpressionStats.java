package ae3.model;

import ae3.service.structuredquery.EfvTree;
import uk.ac.ebi.ae3.indexbuilder.Expression;

/**
 * Expression statistics interface
 * @author pashky
 */
public interface ExpressionStats {
    /**
     * Gets {@link ae3.service.structuredquery.EfvTree} of expression statistics structures
     * @param designElementId design element id
     * @return efv tree of stats
     */
    EfvTree<Stat> getExpressionStats(int designElementId);

    /**
     * Expression statistics for ef/efv pair for one design element
     */
    public static class Stat implements Comparable<Stat> {
        private final double pvalue;
        private final double tstat;

        /**
         * Constructor
         * @param tstat t-statistics
         * @param pvalue p-value
         */
        public Stat(double tstat, double pvalue) {
            this.pvalue = pvalue;
            this.tstat = tstat;
        }

        /**
         * Gets p-value
         * @return p-value
         */
        public double getPvalue() {
            return pvalue;
        }

        /**
         * Gets t-statistics
         * @return t-statistics value
         */
        public double getTstat() {
            return tstat;
        }

        /**
         * Returns whether gene is over-expressed or under-expressed
         * @return gene expression
         */
        public Expression getExpression() {
            return tstat > 0 ? Expression.UP : Expression.DOWN;
        }

        /**
         * Useful, as {@link ae3.service.structuredquery.EfvTree} can return elements sorted by value.
         * P-value of statistics, in this case.
         * @param o other object
         * @return 1, 0 or -1
         */
        public int compareTo(Stat o) {
            return Double.valueOf(getPvalue()).compareTo(o.getPvalue());
        }
    }
}
