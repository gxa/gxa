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

import ae3.service.structuredquery.AtlasEfoService.EfoTermCount;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
     * Structured query condition expanded by query service
 */
public class ExpFactorResultCondition {
    private ExpFactorQueryCondition condition;
    private boolean ignored;
    private Collection<List<AtlasEfoService.EfoTermCount>> efoPaths;

    /**
     * Constructor for condition
     * @param condition original query condition
     * @param efoPaths EFO paths rendered by condition
     * @param ignored if condition is ignored
     */
    public ExpFactorResultCondition(ExpFactorQueryCondition condition, Collection<List<AtlasEfoService.EfoTermCount>> efoPaths, boolean ignored) {
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

    public Set<String> getEfoIds() {
        Set<String> result = new HashSet<String>();
        for(List<EfoTermCount> l : getEfoPaths())
            for(EfoTermCount tc : l)
                result.add(tc.getId());
        return result;
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

    public int getMinExperiments() {
        return condition.getMinExperiments();
    }

    public boolean isOnlyValues() {
        return condition.isOnlyValues();
    }
}
