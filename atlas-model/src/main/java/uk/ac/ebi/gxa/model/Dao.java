package  uk.ac.ebi.gxa.model;

/**
 *  All, absolutely all data retrieval functions used by Atlas app, web services and JSON api. 
 * User: Andrey
 * Date: Oct 20, 2009
 * Time: 5:53:00 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Dao {

    public QueryResultSet<ArrayDesign> getArrayDesign(ArrayDesignQuery atlasArrayDesignQuery, PageSortParams pageSortParams);
    public QueryResultSet<ArrayDesign> getArrayDesign(ArrayDesign atlasArrayDesignQuery);
    public ArrayDesign                 getArrayDesignByAccession(AccessionQuery accession);

    public QueryResultSet<Assay> getAssay(AssayQuery atlasAssayQuery, PageSortParams pageSortParams);
    public QueryResultSet<Assay> getAssay(AssayQuery atlasAssayQuery);
    public Assay                 getAssayByAccession(AccessionQuery accession);

    public QueryResultSet<Sample> getSample(SampleQuery atlasSampleQuery, PageSortParams pageSortParams);
    public QueryResultSet<Sample> getSample(SampleQuery atlasSampleQuery);
    public Sample                 getSampleByAccession(AccessionQuery accession);

    public QueryResultSet<Experiment> getExperiment(ExperimentQuery atlasExperimentQuery, PageSortParams pageSortParams);
    public QueryResultSet<Experiment> getExperiment(ExperimentQuery atlasExperimentQuery);
    public Experiment                 getExperimentByAccession(AccessionQuery accession);

    public QueryResultSet<Property> getProperty(PropertyQuery atlasPropertyQuery, PageSortParams pageSortParams);
    public QueryResultSet<Property> getProperty(PropertyQuery atlasPropertyQuery);
    public Property                 getPropertyByAccession(AccessionQuery accession);

    public QueryResultSet<Gene> getGene(GeneQuery atlasGeneQuery, PageSortParams pageSortParams);
    public QueryResultSet<Gene> getGene(GeneQuery atlasGeneQuery);
    public Gene                 getGeneByAccession(AccessionQuery accession);

    public QueryResultSet<ExpressionStat> getExpressionStat(ExpressionStatQuery atlasExpressionStatQuery, PageSortParams pageSortParams);
    public QueryResultSet<ExpressionStat> getExpressionStat(ExpressionStatQuery atlasExpressionStatQuery);

}