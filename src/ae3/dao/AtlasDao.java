package ae3.dao;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import uk.ac.ebi.ae3.indexbuilder.Constants;
import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import ae3.service.ArrayExpressSearchService;
import ae3.service.QueryHelper;

/**
 * Created by IntelliJ IDEA.
 * User: ostolop, mdylag
 * Date: Apr 17, 2008
 * Time: 9:32:22 AM
 * To change this template use File | Settings | File Templates.
 */
public class AtlasDao {
    
	/**
	 * Returns an AtlasExperiment that contains all information from index.
	 * @param experiment_id_key
	 * @return the AtlasExperiment at the specified experiment_id_key. 
	 * @throws AtlasObjectNotFoundException
	 */
	public static AtlasExperiment getExperimentByIdAER(String experiment_id_key) throws AtlasObjectNotFoundException {
    	String query = Constants.FIELD_AER_FV_OE+":(" + Constants.FIELD_AER_EXPID + ":" + experiment_id_key + ")";
		
        QueryResponse queryResponse = ArrayExpressSearchService.instance().fullTextQueryExpts(query);

        SolrDocumentList documentList = queryResponse.getResults();

        if (documentList == null || documentList.size() == 0)
            throw new AtlasObjectNotFoundException(experiment_id_key);

        SolrDocument exptDoc = documentList.get(0);

        return new AtlasExperiment(exptDoc);
    }
    
	/**
	 * Returns an AtlasExperiment that contains all information from index.
	 * @param solrExptDoc
	 * @param exptHitsResponse
	 * @return
	 */
    public static AtlasExperiment getExperimentByIdAER(SolrDocument solrExptDoc, QueryResponse exptHitsResponse) {
        AtlasExperiment expt = new AtlasExperiment(solrExptDoc);
        expt.setExperimentHighlights(exptHitsResponse.getHighlighting().get(expt.getAerExpId()));
        return expt;
    }

    /**
     * 
     * @param experiment_id_key
     * @return
     * @throws AtlasObjectNotFoundException
     */
	public static AtlasExperiment getExperimentByIdDw(String experiment_id_key) throws AtlasObjectNotFoundException {
    	//String query = Constants.FIELD_AER_FV_OE+":(" + Constants.FIELD_DWEXP_ID + ":" + experiment_id_key + ")";
    	String query = Constants.FIELD_DWEXP_ID + ":" + experiment_id_key;
		
        QueryResponse queryResponse = ArrayExpressSearchService.instance().fullTextQueryExpts(query);

        SolrDocumentList documentList = queryResponse.getResults();

        if (documentList == null || documentList.size() == 0)
            throw new AtlasObjectNotFoundException(experiment_id_key);

        SolrDocument exptDoc = documentList.get(0);

        return new AtlasExperiment(exptDoc);
    }
    
	/**
	 * @param solrExptDoc
	 * @param exptHitsResponse
	 * @return
	 */
    public static AtlasExperiment getExperimentByIdDw(SolrDocument solrExptDoc, QueryResponse exptHitsResponse) {
        AtlasExperiment expt = new AtlasExperiment(solrExptDoc);
        expt.setExperimentHighlights(exptHitsResponse.getHighlighting().get(expt.getDwExpId()));
        return expt;
    }

    
    /**
	 * Returns an AtlasExperiment that contains all information from index.
     * @return
     * @param accessionId - an experiment accession/identifier.
     * @throws AtlasObjectNotFoundException 
     */
    public static AtlasExperiment getExperimentByAccession(String accessionId) throws AtlasObjectNotFoundException 
    {
    	String query = Constants.FIELD_AER_FV_OE+":(" + Constants.FIELD_AER_EXPACCESSION + ":" + accessionId + ")";
        QueryResponse queryResponse = ArrayExpressSearchService.instance().fullTextQueryExpts(query);

        SolrDocumentList documentList = queryResponse.getResults();

        if (documentList == null || documentList.size() == 0)
            throw new AtlasObjectNotFoundException(accessionId);
        
    	SolrDocument exptDoc = documentList.get(0);
    	return new AtlasExperiment(exptDoc);
       

    }

    /**
     * Returns number of experiments. 
     * @param keywords - search keywords.
     * @return number of experiments.
     * @throws SolrServerException
     */
    public static long getExperimentsCount(String keywords[]) throws SolrServerException  
    {
    	String query = QueryHelper.createQuery(keywords);
    	long count= ArrayExpressSearchService.instance().getExperimentsCount(query);
    	return count;
    }
    
    /**
     * Return a list of AtlasExperiment objects for specify keywords. 
     *  
     * @param keywords - the keywords parse to query 
     * @param start - the start record
     * @param rows  - number of records in result set
     * @return {@link List}<AtlasExperiment>
     */
    public static List<AtlasExperiment> getExperiments(String[] keywords, int start, int rows) 
    {
    	String query = QueryHelper.createQuery(keywords);
    	QueryResponse queryResponse = ArrayExpressSearchService.instance().fullTextQueryExpts(query, start, rows);

        SolrDocumentList documentList = queryResponse.getResults();

        //if (documentList == null || documentList.size() == 0)
          //  throw new AtlasObjectNotFoundException(keywords[0]);
        ArrayList<AtlasExperiment> list = new ArrayList<AtlasExperiment>();
       
        Iterator<SolrDocument> itDoc=documentList.iterator();
        while (itDoc.hasNext())
        {
        	SolrDocument exptDoc = itDoc.next();
        	list.add(new AtlasExperiment(exptDoc));
        }
        return list;

    }    
    

    public static AtlasGene getGene(String gene_id_key) throws AtlasObjectNotFoundException {
        QueryResponse queryResponse = ArrayExpressSearchService.instance().fullTextQueryGenes("gene_id:" + gene_id_key);

        SolrDocumentList documentList = queryResponse.getResults();

        if (documentList == null || documentList.size() == 0)
            throw new AtlasObjectNotFoundException(gene_id_key);

        SolrDocument geneDoc = documentList.get(0);

        return new AtlasGene(geneDoc);
    }

    public static AtlasGene getGene(SolrDocument solrGeneDoc, QueryResponse geneHitsResponse) {
        AtlasGene gene = new AtlasGene(solrGeneDoc);
        gene.setGeneHighlights(geneHitsResponse.getHighlighting().get(gene.getGeneId()));
        
        return gene;
    }
}
