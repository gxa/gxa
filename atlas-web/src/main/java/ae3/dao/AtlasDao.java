package ae3.dao;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.commons.lang.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.ae3.indexbuilder.Constants;
import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import ae3.service.ArrayExpressSearchService;
import ae3.util.QueryHelper;
import ae3.util.EscapeUtil;

/**
 * @author: ostolop, mdylag
 */
public class AtlasDao {
    final static protected Logger log = LoggerFactory.getLogger(AtlasDao.class);

    /**
	 * Returns an AtlasExperiment that contains all information from index.
	 * @param experiment_id_key
	 * @return the AtlasExperiment at the specified experiment_id_key. 
	 * @throws AtlasObjectNotFoundException
	 */
    @Deprecated
	public static AtlasExperiment getExperimentByIdAER(String experiment_id_key) throws AtlasObjectNotFoundException {
    	String query = Constants.FIELD_AER_FV_OE+":(" + Constants.FIELD_AER_EXPID + ":" + experiment_id_key + ")";
		
        QueryResponse queryResponse = ArrayExpressSearchService.instance().fullTextQueryExpts(query);

        SolrDocumentList documentList = queryResponse.getResults();

        if (documentList == null || documentList.size() == 0)
            throw new AtlasObjectNotFoundException(experiment_id_key);

        SolrDocument exptDoc = documentList.get(0);
        
        return AtlasExperiment.load(exptDoc, true, true);
    }
    
	/**
	 * Returns an AtlasExperiment that contains all information from index.
	 * @param exptDoc
	 * @param exptHitsResponse
	 * @return
	 */
	@Deprecated
    public static AtlasExperiment getExperimentByIdAER(SolrDocument exptDoc, QueryResponse exptHitsResponse) {
        AtlasExperiment expt = AtlasExperiment.load(exptDoc, true, true);
        expt.setExperimentHighlights(exptHitsResponse.getHighlighting().get(expt.getAerExpId()));
        return expt;
    }

    /**
     * 
     * @param experiment_id_key
     * @return
     * @throws AtlasObjectNotFoundException
     */
	public static AtlasExperiment getExperimentByIdDw(String experiment_id_key) {
    	String query = Constants.FIELD_DWEXP_ID + ":" + experiment_id_key;

        QueryResponse queryResponse = ArrayExpressSearchService.instance().fullTextQueryExpts(query);

        SolrDocumentList documentList = queryResponse.getResults();

        if (documentList == null || documentList.size() == 0)
        	return null;

        SolrDocument exptDoc = documentList.get(0);

        AtlasExperiment expt = AtlasExperiment.load(exptDoc, true, true);

        return expt;
    }
    
	/**
	 * @param exptDoc
	 * @param exptHitsResponse
	 * @return
	 */
    public static AtlasExperiment getExperimentByIdDw(SolrDocument exptDoc, QueryResponse exptHitsResponse) {
        AtlasExperiment expt = AtlasExperiment.load(exptDoc, true, true);;
        expt.setExperimentHighlights(exptHitsResponse.getHighlighting().get(expt.getDwExpAccession()));
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
    	String query = Constants.FIELD_DWEXP_ACCESSION + ":" + accessionId;
        QueryResponse queryResponse = ArrayExpressSearchService.instance().fullTextQueryExpts(query);

        SolrDocumentList documentList = queryResponse.getResults();

        if (documentList == null || documentList.size() == 0)
            throw new AtlasObjectNotFoundException(accessionId);
        
    	SolrDocument exptDoc = documentList.get(0);
    	return AtlasExperiment.load(exptDoc, true, true);
    }

    
    
    /**
     * Return a list of AtlasExperiment objects for specify keywords. 
     *  
     * @param keywords - the keywords parse to query 
     * @param start - the start record
     * @param rows  - number of records in result set
     * @return {@link List}<AtlasExperiment>
     */
    public static List<AtlasExperiment> getExperimentsAer(String[] keywords, int start, int rows) 
    {
    	String query = QueryHelper.prepareQueryByKeywords(keywords);
    	QueryResponse queryResponse = ArrayExpressSearchService.instance().fullTextQueryExpts(query, start, rows);

        SolrDocumentList documentList = queryResponse.getResults();

        ArrayList<AtlasExperiment> list = new ArrayList<AtlasExperiment>();
       
        Iterator<SolrDocument> itDoc=documentList.iterator();
        while (itDoc.hasNext())
        {
        	SolrDocument exptDoc = itDoc.next();
        	AtlasExperiment atlasExp = new AtlasExperiment(true, false);
        	atlasExp.load(exptDoc);
        	list.add(atlasExp);
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
    
    /**
     * TODO
     * 
     * @param solrGeneDoc
     * @param geneHitsResponse
     * @return
     */
    public static AtlasGene getGene(SolrDocument solrGeneDoc, QueryResponse geneHitsResponse) {
        AtlasGene gene = new AtlasGene(solrGeneDoc);
        gene.setGeneHighlights(geneHitsResponse.getHighlighting().get(gene.getGeneId()));
        
        return gene;
    }

    /**
     * Returns a list of SOLR gene documents for the gene specified
     * @param gene_identifier
     * @return
     * @throws AtlasObjectNotFoundException
     */
    public static SolrDocumentList getDocListForGene(String gene_identifier) throws AtlasObjectNotFoundException {
    	QueryResponse queryResponse = ArrayExpressSearchService.instance().fullTextQueryGenes("gene_ids:" + EscapeUtil.escapeSolr(gene_identifier));
    	
        SolrDocumentList documentList = queryResponse.getResults();
        if (documentList == null || documentList.size() == 0)
            throw new AtlasObjectNotFoundException(gene_identifier);
       
        return documentList;
    }
    
    /**
     * Returns the AtlasGene corresponding to the specified gene identifier, i.e. matching one of the terms in the
     * "gene_ids" field in Solr schema.
     *
     * @param gene_identifier
     * @return AtlasGene
     * @throws AtlasObjectNotFoundException
     * @throws MultipleGeneException
     */
    public static AtlasGene getGeneByIdentifier(String gene_identifier) throws AtlasObjectNotFoundException, MultipleGeneException {
        QueryResponse queryResponse = ArrayExpressSearchService.instance().fullTextQueryGenes("gene_ids:" + EscapeUtil.escapeSolr(gene_identifier));

        SolrDocumentList documentList = queryResponse.getResults();

        if (documentList == null || documentList.size() == 0)
            throw new AtlasObjectNotFoundException(gene_identifier);

        if (documentList.size() > 1)
            throw new MultipleGeneException(gene_identifier);
            

        SolrDocument geneDoc = documentList.get(0);

        return new AtlasGene(geneDoc);
    }
}
