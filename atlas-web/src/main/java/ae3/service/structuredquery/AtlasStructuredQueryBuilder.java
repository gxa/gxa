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
 * Utility class mainly for tests use, allows chained build of Atlas structured queries
 * @author pashky
 */
public class AtlasStructuredQueryBuilder {

    private AtlasStructuredQuery q = new AtlasStructuredQuery();

    public AtlasStructuredQuery query() {
        return q;
    }

    public AtlasStructuredQueryBuilder andGene(Object values) {
        return andGene("", values);
    }

    public AtlasStructuredQueryBuilder andGene(String property, Object values) {
        return andGene(property, true, values);
    }

    public AtlasStructuredQueryBuilder andNotGene(Object values) {
        return andNotGene("", values);
    }

    public AtlasStructuredQueryBuilder andNotGene(String property, Object values) {
        return andGene(property, false, values);
    }

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

    public AtlasStructuredQueryBuilder andExprIn(String factor, QueryExpression expr, Object values) {
        return andExprIn(factor, expr, 1, values);
    }


    public AtlasStructuredQueryBuilder andUpdnIn(Object values) {
        return andUpdnIn("", values);
    }

    public AtlasStructuredQueryBuilder andUpdnIn(String factor, Object values) {
        return andExprIn(factor, QueryExpression.UP_DOWN, values);
    }

    public AtlasStructuredQueryBuilder andUpIn(Object values) {
        return andUpIn("", values);
    }

    public AtlasStructuredQueryBuilder andUpIn(String factor, Object values) {
        return andExprIn(factor, QueryExpression.UP, values);
    }

    public AtlasStructuredQueryBuilder andDnIn(Object values) {
        return andDnIn("", values);
    }

    public AtlasStructuredQueryBuilder andDnIn(String factor, Object values) {
        return andExprIn(factor, QueryExpression.DOWN, values);
    }

    public AtlasStructuredQueryBuilder andSpecies(String specie) {
        List<String> species = new ArrayList<String>(q.getSpecies());
        species.add(specie);
        q.setSpecies(species);
        return this;
    }

    public AtlasStructuredQueryBuilder viewAs(ViewType viewtype) {
        q.setViewType(viewtype);
        return this;
    }

    public AtlasStructuredQueryBuilder rowsPerPage(int rows) {
        q.setRowsPerPage(rows);
        return this;
    }

    public AtlasStructuredQueryBuilder startFrom(int start) {
        q.setStart(start);
        return this;
    }

    public AtlasStructuredQueryBuilder expsPerGene(int exps) {
        q.setExpsPerGene(exps);
        return this;
    }
}
