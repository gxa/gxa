package  uk.ac.ebi.gxa.model;


import uk.ac.ebi.gxa.utils.Pair;

import java.util.List;
import java.util.ArrayList;


/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Oct 22, 2009
 * Time: 11:47:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class ExpressionStatQuery {

    private List<GeneQuery> geneQueries = new ArrayList<GeneQuery>();
    private List<Pair<ExpressionQuery,PropertyQuery>> activityQueries = new ArrayList<Pair<ExpressionQuery, PropertyQuery>>();
    private boolean facets = false;

    public ExpressionStatQuery hasGene(GeneQuery geneQuery) {
        geneQueries.add(geneQuery);
        return this;
    }

    public ExpressionStatQuery activeIn(ExpressionQuery expression, PropertyQuery property) {
        activityQueries.add(new Pair<ExpressionQuery, PropertyQuery>(expression, property));
        return this;
    }

    public List<GeneQuery> getGeneQueries() {
        return geneQueries;
    }

    public List<Pair<ExpressionQuery, PropertyQuery>> getActivityQueries() {
        return activityQueries;
    }

    public boolean isFacets() {
        return facets;
    }

    public ExpressionStatQuery facets(boolean facets) {
        this.facets = facets;
        return this;
    }
}