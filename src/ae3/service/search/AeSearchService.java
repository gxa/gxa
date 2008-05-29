package ae3.service.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import uk.ac.ebi.ae3.indexbuilder.Constants;

import ae3.model.AtlasExperiment;
import ae3.service.ArrayExpressSearchService;
import ae3.service.QueryHelper;

import com.Ostermiller.util.StringTokenizer;

//import com.Ostermiller.util.StringTokenizer;

/**
 * The class searches the experiment index by keywords, species and id array design.
 * Affords API which creates a XML output file.
 * and return the XML document or number of documents. 
 * @author mdylag
 *
 */
public class AeSearchService
{

	
	
	/**
	 * The method return the XML document contains experiment(s).
	 * if keywords, species and arrayDesId is null returns null.
	 * if rows is equal 0 returns null too. 
	 * @param keywords - 
	 * @param species - 
	 * @param arrayDesId - 
	 * @param start - 
	 * @param rows -
	 * @param sortField - a sort field 
	 * @param sortOrder - a   
	 * @return the XML document, null if (keywords, species and arrayDesId is null) or rows is 0 or searching returbs 0.   
	 */
	public static String searchIdxAer(String keywords, String species, Long arrayDesId, int start, int rows, String sortField, String sortOrder) throws SolrServerException
	{
		if (!QueryHelper.parseParam(keywords, species, arrayDesId, null, null))
			return null;
		String query = QueryHelper.prepareQuery(keywords, species, arrayDesId);
		//get total
		SolrDocumentList list=getNumOfDocAndFacet(query);
		long total = list.getNumFound();
		String _sortField = QueryHelper.convParamSortToFieldName(sortField);
		ORDER _sortOrder = QueryHelper.convParamOrderToOrder(sortOrder); 
		QueryResponse resp=ArrayExpressSearchService.instance().fullTextQueryExptsAer(query, start, rows, _sortField, _sortOrder);
		SolrDocumentList docList=resp.getResults();
		Map<String, Map<String, List<String>>>hgl=resp.getHighlighting();
		List<FacetField> facetFields=resp.getFacetFields();
		Document doc=XmlHelper.createXmlDoc(docList, total, start, rows);		
		return doc.asXML();
	}
	//
	//TODO: Add special Exception
	//	
	public static long getNumOfDoc(String keywords, String species, Long arrayDesId) throws SolrServerException
	{
		if (!QueryHelper.parseParam(keywords, species, arrayDesId, null, null))
			return 0;
		String query = QueryHelper.prepareQuery(keywords, species, arrayDesId);
		return getNumOfDoc(query);
	}
	
	
	public static long getNumOfDoc(String query) throws SolrServerException
	{	
	    long count = 0;
	    SolrDocumentList list =ArrayExpressSearchService.instance().getNumDocAer(query, false);
	    if (list != null)
		count=list.getNumFound();
	    return count;
	}
	
	protected static SolrDocumentList getNumOfDocAndFacet(String query) throws SolrServerException
	{
	    if (StringUtils.isEmpty(query))
		return null;
	    SolrDocumentList docList=ArrayExpressSearchService.instance().getNumDocAer(query, true);
	    return docList;
	}


}
