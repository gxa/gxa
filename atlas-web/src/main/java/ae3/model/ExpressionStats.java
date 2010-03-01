/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://ostolop.github.com/gxa/
 */

package ae3.model;

import ae3.service.structuredquery.EfvTree;

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
