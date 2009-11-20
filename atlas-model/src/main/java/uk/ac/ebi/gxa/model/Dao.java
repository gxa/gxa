package  uk.ac.ebi.gxa.model;

/**
 *  All, absolutely all data retrieval functions used by Atlas app, web services and JSON api. 
 * User: Andrey
 * Date: Oct 20, 2009
 * Time: 5:53:00 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Dao {

    public QueryResultSet<ArrayDesign> getArrayDesign(ArrayDesignQuery atlasArrayDesignQuery, PageSortParams pageSortParams) throws GxaException;
    public QueryResultSet<ArrayDesign> getArrayDesign(ArrayDesignQuery atlasArrayDesignQuery) throws GxaException;
    public ArrayDesign                 getArrayDesignByAccession(AccessionQuery accession) throws GxaException;
//    public Integer[]                   getArrayDesignIDs(ArrayDesignQuery atlasArrayDesignQuery) throws GxaException;

    public QueryResultSet<Assay> getAssay(AssayQuery atlasAssayQuery, PageSortParams pageSortParams) throws GxaException;
    public QueryResultSet<Assay> getAssay(AssayQuery atlasAssayQuery) throws GxaException;
    public Assay                 getAssayByAccession(AccessionQuery accession) throws GxaException;
//    public Integer[]             getAssayIDs(AssayQuery atlasAssayQuery) throws GxaException;

    public QueryResultSet<Sample> getSample(SampleQuery atlasSampleQuery, PageSortParams pageSortParams) throws GxaException;
    public QueryResultSet<Sample> getSample(SampleQuery atlasSampleQuery) throws GxaException;
    public Sample                 getSampleByAccession(AccessionQuery accession) throws GxaException;
//    public Integer[]              getSampleIDs(SampleQuery atlasSampleQuery) throws GxaException;

    public QueryResultSet<Experiment> getExperiment(ExperimentQuery atlasExperimentQuery, PageSortParams pageSortParams) throws GxaException;
    public QueryResultSet<Experiment> getExperiment(ExperimentQuery atlasExperimentQuery) throws GxaException;
    public Experiment                 getExperimentByAccession(AccessionQuery accession) throws GxaException;
//    public Integer[]                  getExperimentIDs(ExperimentQuery atlasExperimentQuery) throws GxaException;

    public QueryResultSet<Property> getProperty(PropertyQuery atlasPropertyQuery, PageSortParams pageSortParams) throws GxaException;
    public QueryResultSet<Property> getProperty(PropertyQuery atlasPropertyQuery) throws GxaException;
    public Property                 getPropertyByAccession(AccessionQuery accession) throws GxaException;
//    public Integer[]                getPropertyIDs(PropertyQuery atlasPropertyQuery) throws GxaException;

    public QueryResultSet<Gene> getGene(GeneQuery atlasGeneQuery, PageSortParams pageSortParams) throws GxaException;
    public QueryResultSet<Gene> getGene(GeneQuery atlasGeneQuery) throws GxaException;
    public Gene                 getGeneByAccession(AccessionQuery accession) throws GxaException;
//    public Integer[]            getGeneIDs(GeneQuery atlasGeneQuery) throws GxaException;

    public <T extends ExpressionStat> FacetQueryResultSet<T, ExpressionStatFacet> getExpressionStat(ExpressionStatQuery atlasExpressionStatQuery, PageSortParams pageSortParams) throws GxaException;
    public <T extends ExpressionStat> FacetQueryResultSet<T, ExpressionStatFacet> getExpressionStat(ExpressionStatQuery atlasExpressionStatQuery) throws GxaException;

}