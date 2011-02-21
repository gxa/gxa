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

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import uk.ac.ebi.gxa.utils.EscapeUtil;

import java.util.List;

/**
 * Class representing one experiment or gene condition
 */
public abstract class QueryCondition {
    private String factor;
    private List<String> factorValues;

    /**
     * Returns factor
     * @return factor name
     */
    @JsonProperty("factor")
    public String getFactor() {
        return factor;
    }

    /**
     * Sets factor name
     * @param factor factor name
     */
    public void setFactor(String factor) {
        this.factor = factor;
    }

    /**
     * Returns factor values
     * @return iterable factor values
     */
    @JsonIgnore
    public List<String> getFactorValues() {
        return factorValues;
    }

    /**
     * Returns string with space-separated factor values, quoted if necessary
     * @return string of all factor values
     */
    @JsonProperty("jointFactorValues")
    public String getJointFactorValues() {
        return EscapeUtil.joinQuotedValues(factorValues);
    }

    /**
     * Returns string with space-separated factor values, quoted if necessary
     * @return string of all factor values
     */
    @JsonIgnore
    public String getSolrEscapedFactorValues() {
        return EscapeUtil.escapeSolrValueList(factorValues);
    }

    /**
     * Sets factor values
     * @param factorValues list of factor values
     */
    public void setFactorValues(List<String> factorValues) {
        this.factorValues = factorValues;
    }

    /**
     * Convenience method to check whether conditions is for any factor
     * @return true if any factor
     */
    @JsonIgnore
    public boolean isAnyFactor() {
        return getFactor().length() == 0;
    }

    /**
     * Convenience method to check whether conditions is for any value
     * @return true if any value contains '*' or all values are empty
     */
    @JsonIgnore
    public boolean isAnyValue() {
        for(String v : getFactorValues())
            if(v.equals("*"))
                return true;
        for(String v : getFactorValues())
            if(!v.equals(""))
                return false;
        return true;
    }

    /**
     * Convenience method to check whether condition is for anything (any value and any factor)
     * @return
     */
    @JsonIgnore
    public boolean isAnything() {
        return isAnyValue() && isAnyFactor();
    }

}
