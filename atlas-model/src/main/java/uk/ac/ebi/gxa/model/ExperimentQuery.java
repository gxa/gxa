package  uk.ac.ebi.gxa.model;

/**
 * Simple search object, used to retrieve {@link Experiment}.
 * User: Andrey
 * Date: Oct 14, 2009
 * Time: 5:07:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExperimentQuery extends AccessionQuery<ExperimentQuery>  {

    private PropertyQuery propertyQuery;
    private GeneQuery geneQuery;
    private String lab;
    private String performer;

    private String solrQuery;
    public String getSolrQuery(){
        return solrQuery;
    }

    public ExperimentQuery(){};

    public ExperimentQuery(AccessionQuery accessionQuery){
        super(accessionQuery);
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
        this.geneQuery = geneQuery;
        return this;
    }

    /**
     * performer
     */
    public ExperimentQuery hasPerformer(String performer){
        this.performer = performer;
        return this;
    }

    /**
     * lab
     */
    public ExperimentQuery hasLab(String lab){
        this.lab = lab;
        return this;
    }

    /**
     * filter by properties
     */
    public ExperimentQuery hasProperties(PropertyQuery propertyQuery){
        this.propertyQuery = propertyQuery;
        return this;
    }

    public String getPerformer(){
        return this.performer;
    }

    public String getLab(){
        return this.lab;
    }

    public PropertyQuery getPropertyQuery(){
        return this.propertyQuery;
    }

    public GeneQuery getGeneQuery(){
        return this.geneQuery;
    }

}