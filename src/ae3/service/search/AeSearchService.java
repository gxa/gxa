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
 * The class searches the experiment index 
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
	 * @param sortField - 
	 * @param sortOrder -   
	 * @return the XML document, null if (keywords, species and arrayDesId is null) or rows is 0.   
	 */
	public static String searchIdxAer(String keywords, String species, Long arrayDesId, int start, int rows, String sortField, String sortOrder) throws SolrServerException
	{
		if (!QueryHelper.parseParam(keywords, species, arrayDesId, null, null))
			return null;
		String query = QueryHelper.prepareQuery(keywords, species, arrayDesId);
		//get total
		long total=getNumberOfDoc(query);
		String _sortField = QueryHelper.convParamSortToFieldName(sortField);
		ORDER _sortOrder = QueryHelper.convParamOrderToOrder(sortOrder); 
		QueryResponse resp=ArrayExpressSearchService.instance().fullTextQueryExpts(query, start, rows, false ,true);
		SolrDocumentList docList=resp.getResults();
		Map<String, Map<String, List<String>>>hgl=resp.getHighlighting();
		List<FacetField> facetFields=resp.getFacetFields();
		Document doc=XmlHelper.createXmlDoc(docList, total, start, rows);		
		return doc.asXML();
	}
	//
	//TODO: Add special Exception
	//	
	public static long getNumberOfDoc(String keywords, String species, Long arrayDesId) throws SolrServerException
	{
		if (!QueryHelper.parseParam(keywords, species, arrayDesId, null, null))
			return 0;
		String query = QueryHelper.prepareQuery(keywords, species, arrayDesId);
		return getNumberOfDoc(query);
	}
	
	
	private static long getNumberOfDoc(String query) throws SolrServerException
	{		
		return ArrayExpressSearchService.instance().getNumDoc(query);
	}
	
	private static SolrDocumentList getNumOfDocAndFacet(String query) throws SolrServerException
	{
		SolrDocumentList docList=ArrayExpressSearchService.instance().getNumDoc(query, true, true);
		return docList;
		
		//docList.
	}


}
