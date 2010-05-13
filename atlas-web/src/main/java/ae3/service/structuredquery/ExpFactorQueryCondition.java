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
 * http://gxa.github.com/gxa
 */

package ae3.service.structuredquery;

/**
 * @author pashky
 */
public class ExpFactorQueryCondition extends QueryCondition {
    private QueryExpression expression;
    private int minExperiments = 1;

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

    /**
     * Gets minimum number of experiments required
     * @return number
     */
    public int getMinExperiments() {
        return minExperiments;
    }

    /**
     * Sets the minimum number experiments required
     * @param minExperiments number
     */
    public void setMinExperiments(int minExperiments) {
        if(minExperiments > 0)
            this.minExperiments = minExperiments;
    }

    public boolean isOnly() {
        return expression == QueryExpression.UP_ONLY || expression == QueryExpression.DOWN_ONLY;
    }
}
