package  uk.ac.ebi.gxa.model;

/**
 * Simple search object, used to retrieve {@link Experiment}.
 * User: Andrey
 * Date: Oct 14, 2009
 * Time: 5:07:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExperimentQuery extends AccessionQuery<ExperimentQuery>  {

    private String solrQuery;
    public String getSolrQuery(){
        return solrQuery;
    }
    /**
     * rudimentary Solr query string
     */
    public void doSolrQuery(String query){
        this.solrQuery = query;
    }

    /**
     * filter genes participating in the experiment
     */
    public ExperimentQuery hasGene(GeneQuery geneQuery){
        return this;
    }

    /**
     * performer
     */
    public ExperimentQuery hasPerformer(String performer){
        return this;
    }

    /**
     * lab
     */
    public ExperimentQuery hasLab(String lab){
        return this;
    }

    /**
     * filter by properties
     */
    public ExperimentQuery hasProperties(PropertyQuery propertyQuery){
        return this;
    }

}