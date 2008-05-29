package ae3.service.search;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.highlight.DefaultSolrHighlighter;
import org.dom4j.Document;

import com.sun.jmx.mbeanserver.NamedObject;

import uk.ac.ebi.ae3.indexbuilder.Constants;

import ae3.service.ArrayExpressSearchService;
import ae3.service.QueryHelper;


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
	 * The method return a XML document contains experiment(s).
	 * if keywords, species and arrayDesId is null returns null.
	 * if rows is equal 0 returns null too. 
	 * @param keywords - is a keyword for full text search.
	 * @param species - is a keyword for searching species.
	 * @param arrayDesId - is a parameter for search array design id
	 * @param start - define start row
	 * @param rows - define number of row
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
		SolrQuery solrQuery=QueryHelper.createSolrQueryForAer(query, start, rows, _sortField, _sortOrder);
		QueryResponse resp=ArrayExpressSearchService.instance().query(solrQuery);
		 DefaultSolrHighlighter sol = new DefaultSolrHighlighter();
		 String hgl[] = {Constants.FIELD_AER_DESC_TEXT};
		 //NamedObject ob=sol.doHighlighting(resp.getResults()., solrQuery, resp, hgl);
		Document doc=XmlHelper.createXmlDoc(resp, total, start, rows);		
		return doc.asXML();
	}
	
	/**
         * 
         * @param keywords
         * @param species
         * @param arrayDesId
         * @return
         * @throws SolrServerException
         */	
	public static long getNumOfDoc(String keywords, String species, Long arrayDesId) throws SolrServerException
	{
		if (!QueryHelper.parseParam(keywords, species, arrayDesId, null, null))
			return 0;
		String query = QueryHelper.prepareQuery(keywords, species, arrayDesId);
		return getNumOfDoc(query);
	}
	
	/**
	 * 
	 * @param query
	 * @return
	 * @throws SolrServerException
	 */
	public static long getNumOfDoc(String query) throws SolrServerException
	{	
	    long count = 0;
	    SolrDocumentList list =ArrayExpressSearchService.instance().getNumDocAer(query, false);
	    if (list != null)
		count=list.getNumFound();
	    return count;
	}
	
	/**
	 * 
	 * @param query
	 * @return
	 * @throws SolrServerException
	 */
	protected static SolrDocumentList getNumOfDocAndFacet(String query) throws SolrServerException
	{
	    if (StringUtils.isEmpty(query))
		return null;
	    SolrDocumentList docList=ArrayExpressSearchService.instance().getNumDocAer(query, true);
	    return docList;
	}


}
