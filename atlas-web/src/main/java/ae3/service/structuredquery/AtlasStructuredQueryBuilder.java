package ae3.service.structuredquery;

import ae3.util.EscapeUtil;

import java.util.List;
import java.util.ArrayList;


/**
 * Utility class mainly for tests use, allows chained build of Atlas structured queries
 * @author pashky
 */
public class AtlasStructuredQueryBuilder {

    private AtlasStructuredQuery q = new AtlasStructuredQuery();

    public AtlasStructuredQuery toQuery() {
        return q;
    }

    public AtlasStructuredQueryBuilder andGene(String values) {
        return andGene("", values);
    }

    public AtlasStructuredQueryBuilder andGene(String property, String values) {
        List<GeneQueryCondition> conds = new ArrayList<GeneQueryCondition>(q.getGeneConditions());
        GeneQueryCondition cond = new GeneQueryCondition();
        cond.setFactor(property);
        cond.setNegated(false);
        cond.setFactorValues(EscapeUtil.parseQuotedList(values));
        conds.add(cond);
        q.setGeneConditions(conds);
        return this;
    }

    public AtlasStructuredQueryBuilder andNotGene(String values) {
        return andNotGene("", values);
    }

    public AtlasStructuredQueryBuilder andNotGene(String property, String values) {
        List<GeneQueryCondition> conds = new ArrayList<GeneQueryCondition>(q.getGeneConditions());
        GeneQueryCondition cond = new GeneQueryCondition();
        cond.setFactor(property);
        cond.setNegated(true);
        cond.setFactorValues(EscapeUtil.parseQuotedList(values));
        conds.add(cond);
        q.setGeneConditions(conds);
        return this;
    }

    public AtlasStructuredQueryBuilder andUpdnIn(String values) {
        return andUpdnIn("", values);
    }

    public AtlasStructuredQueryBuilder andUpdnIn(String factor, String values) {
        List<ExpFactorQueryCondition> conds = new ArrayList<ExpFactorQueryCondition>(q.getConditions());
        ExpFactorQueryCondition cond = new ExpFactorQueryCondition();
        cond.setFactor(factor);
        cond.setExpression(QueryExpression.UP_DOWN);
        cond.setFactorValues(EscapeUtil.parseQuotedList(values));
        conds.add(cond);
        q.setConditions(conds);
        return this;
    }

    public AtlasStructuredQueryBuilder andUpIn(String values) {
        return andUpIn("", values);
    }

    public AtlasStructuredQueryBuilder andUpIn(String factor, String values) {
        List<ExpFactorQueryCondition> conds = new ArrayList<ExpFactorQueryCondition>(q.getConditions());
        ExpFactorQueryCondition cond = new ExpFactorQueryCondition();
        cond.setFactor(factor);
        cond.setExpression(QueryExpression.UP_DOWN);
        cond.setFactorValues(EscapeUtil.parseQuotedList(values));
        conds.add(cond);
        q.setConditions(conds);
        return this;
    }

    public AtlasStructuredQueryBuilder andDnIn(String values) {
        return andDnIn("", values);
    }

    public AtlasStructuredQueryBuilder andDnIn(String factor, String values) {
        List<ExpFactorQueryCondition> conds = new ArrayList<ExpFactorQueryCondition>(q.getConditions());
        ExpFactorQueryCondition cond = new ExpFactorQueryCondition();
        cond.setFactor(factor);
        cond.setExpression(QueryExpression.UP_DOWN);
        cond.setFactorValues(EscapeUtil.parseQuotedList(values));
        conds.add(cond);
        q.setConditions(conds);
        return this;
    }

    public AtlasStructuredQueryBuilder andSpecies(String specie) {
        List<String> species = new ArrayList<String>(q.getSpecies());
        species.add(specie);
        q.setSpecies(species);
        return this;
    }
}
