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

import static uk.ac.ebi.gxa.utils.EscapeUtil.optionalParseList;

import java.util.ArrayList;
import java.util.List;


/**
 * Utility class, allows chained build of Atlas structured queries
 *
 * Typical usage pattern is
 * AtlasStructuredQuery q = new AtlasStructuredQueryBuilder().andGene("aspm").andUpIn("liver").viewAs(ViewType.LIST).query()
 * service.doAtlasStructuredQuery(q);
 *
 * @author pashky
 */
public class AtlasStructuredQueryBuilder {

    private AtlasStructuredQuery q = new AtlasStructuredQuery();

    /**
     * Returns assembled query
     * @return atlas structured query object
     */
    public AtlasStructuredQuery query() {
        return q;
    }

    /**
     * Appends gene condition for any property
     * @param values values, could be either list or single value to be parsed as space separated quoted string
     * @return self
     */
    public AtlasStructuredQueryBuilder andGene(Object values) {
        return andGene("", values);
    }

    /**
     * Appends gene condition for specific property
     * @param property property
     * @param values values, could be either list or single value to be parsed as space separated quoted string
     * @return self
     */
    public AtlasStructuredQueryBuilder andGene(String property, Object values) {
        return andGene(property, true, values);
    }

    /**
     * Appends negated gene condition for any property
     * @param values values, could be either list or single value to be parsed as space separated quoted string
     * @return self
     */
    public AtlasStructuredQueryBuilder andNotGene(Object values) {
        return andNotGene("", values);
    }

    /**
     * Appends negated gene condition for specific property
     * @param property property
     * @param values values, could be either list or single value to be parsed as space separated quoted string
     * @return self
     */
    public AtlasStructuredQueryBuilder andNotGene(String property, Object values) {
        return andGene(property, false, values);
    }

    /**
     * Appends gene condition
     * @param property property
     * @param has true if conditions is "NOT"
     * @param values values, could be either list or single value to be parsed as space separated quoted string
     * @return self
     */
    public AtlasStructuredQueryBuilder andGene(String property, boolean has, Object values) {

        List<String> vlist = optionalParseList(values);
        if (vlist.isEmpty()) {
            return this;
        }

        List<GeneQueryCondition> conds = new ArrayList<GeneQueryCondition>(q.getGeneConditions());
        GeneQueryCondition cond = new GeneQueryCondition();
        cond.setFactor(property);
        cond.setNegated(!has);
        cond.setFactorValues(vlist);
        conds.add(cond);
        q.setGeneConditions(conds);
        return this;
    }

    /**
     * Appends expression condition
     * @param factor specific factor
     * @param expr specific expression
     * @param minExperiments minimum number of experiments
     * @param values values, could be either list or single value to be parsed as space separated quoted string
     * @return self
     */
    public AtlasStructuredQueryBuilder andExprIn(String factor, QueryExpression expr, int minExperiments, Object values) {
        List<String> vlist = optionalParseList(values);
        if (vlist.isEmpty() && "".equals(factor)) {
            return this;
        }

        List<ExpFactorQueryCondition> conds = new ArrayList<ExpFactorQueryCondition>(q.getConditions());
        ExpFactorQueryCondition cond = new ExpFactorQueryCondition();
        cond.setFactor(factor);
        cond.setExpression(expr);
        cond.setFactorValues(vlist);
        cond.setMinExperiments(minExperiments);
        conds.add(cond);
        q.setConditions(conds);
        return this;
    }

    /**
     * Appends expression condition
     * @param factor specific factor
     * @param expr specific expression
     * @param values values, could be either list or single value to be parsed as space separated quoted string
     * @return self
     */
    public AtlasStructuredQueryBuilder andExprIn(String factor, QueryExpression expr, Object values) {
        return andExprIn(factor, expr, 1, values);
    }

    /**
     * Appends up/down expression condition for any factor
     * @param values values, could be either list or single value to be parsed as space separated quoted string
     * @return self
     */
    public AtlasStructuredQueryBuilder andUpdnIn(Object values) {
        return andUpdnIn("", values);
    }

    /**
     * Appends up/down expression condition for specific factor
     * @param factor factor
     * @param values values, could be either list or single value to be parsed as space separated quoted string
     * @return self
     */
    public AtlasStructuredQueryBuilder andUpdnIn(String factor, Object values) {
        return andExprIn(factor, QueryExpression.UP_DOWN, values);
    }

    /**
     * Appends upn expression condition for any factor
     * @param values values, could be either list or single value to be parsed as space separated quoted string
     * @return self
     */
    public AtlasStructuredQueryBuilder andUpIn(Object values) {
        return andUpIn("", values);
    }

    /**
     * Appends up expression condition for specific factor
     * @param factor factor
     * @param values values, could be either list or single value to be parsed as space separated quoted string
     * @return self
     */
    public AtlasStructuredQueryBuilder andUpIn(String factor, Object values) {
        return andExprIn(factor, QueryExpression.UP, values);
    }

    /**
     * Appends down expression condition for any factor
     * @param values values, could be either list or single value to be parsed as space separated quoted string
     * @return self
     */
    public AtlasStructuredQueryBuilder andDnIn(Object values) {
        return andDnIn("", values);
    }

    /**
     * Appends down expression condition for specific factor
     * @param factor factor
     * @param values values, could be either list or single value to be parsed as space separated quoted string
     * @return self
     */
    public AtlasStructuredQueryBuilder andDnIn(String factor, Object values) {
        return andExprIn(factor, QueryExpression.DOWN, values);
    }

    /**
     * Append species restriction condition
     * @param species search string
     * @return self
     */
    public AtlasStructuredQueryBuilder andSpecies(String species) {
        List<String> sl = new ArrayList<String>(q.getSpecies());
        sl.add(species);
        q.setSpecies(sl);
        return this;
    }

    /**
     * Sets result view type
     * @param viewtype view type
     * @return self
     */
    public AtlasStructuredQueryBuilder viewAs(ViewType viewtype) {
        q.setViewType(viewtype);
        return this;
    }

    /**
     * Sets number of rows per page
     * @param rows number of rows per page
     * @return self
     */
    public AtlasStructuredQueryBuilder rowsPerPage(int rows) {
        q.setRowsPerPage(rows);
        return this;
    }

    /**
     * Sets starting position 
     * @param start starting position
     * @return self
     */
    public AtlasStructuredQueryBuilder startFrom(int start) {
        q.setStart(start);
        return this;
    }

    /**
     * Sets maximum number of experiments per gene
     * @param exps maximum experiments per gene
     * @return self
     */
    public AtlasStructuredQueryBuilder expsPerGene(int exps) {
        q.setExpsPerGene(exps);
        return this;
    }
}
