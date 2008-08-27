package ae3.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.Query;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.QueryResponse;


import com.Ostermiller.util.StringTokenizer;

import uk.ac.ebi.ae3.indexbuilder.Constants;
import uk.ac.ebi.ae3.indexbuilder.utils.XmlUtil;

/**
 * The helper methods for working with lucene query.
 * @author mdylag
 *
 */
public class QueryHelper
{
	/**
	 * Creates query for full text search from keywords parameter.
	 * @param keywords - table of keywords
	 * @return - a lucene query
	 */
	public static final String prepareQueryByKeywords(String[] keywords)
	{
	    StringBuffer buff = new StringBuffer();
	    for (int i=0; i<keywords.length; i++) {
    		String val = keywords[i];
    		buff.append(Constants.FIELD_AER_EXPACCESSION).append(":").append(val);
    		buff.append(" ");
    		buff.append(val).append(" ");
	    }
	    
	    String query = buff.toString().trim();
	    return query;	    
	}
	
	
	public static String prepareQuery(String keywords, String species, Long arrayDesId)
	{
		if (!parseParam(keywords, species, arrayDesId, null, null))
			return null;
		boolean addAnd = false;
		
		StringBuffer query = new StringBuffer();
			
		if (!StringUtils.isEmpty(keywords))
	    {
	    	//create query keywords - change because I need to add highlight	
			query.append(Constants.FIELD_AER_EXPNAME).append("(").append(keywords).append(") ");
			query.append(Constants.FIELD_AER_SAAT_CAT).append("(").append(keywords).append(") ");
			query.append(Constants.FIELD_AER_SAAT_VALUE).append("(").append(keywords).append(") ");
			query.append(Constants.FIELD_AER_FV_FACTORNAME).append("(").append(keywords).append(") ");
			query.append(Constants.FIELD_AER_FV_OE).append("(").append(keywords).append(") ");
			query.append(Constants.FIELD_AER_ARRAYDES_NAME).append("(").append(keywords).append(") ");
			query.append(Constants.FIELD_AER_BI_AUTHORS).append("(").append(keywords).append(") ");
			query.append(Constants.FIELD_AER_BI_TITLE).append("(").append(keywords).append(") ");
			query.append(Constants.FIELD_AER_EXPDES_TYPE).append("(").append(keywords).append(") ");
			query.append(Constants.FIELD_AER_DESC_TEXT).append("(").append(keywords).append(") ");
	    	//query.append("(").append(keywords).append(")");
	    	//Add each field
	    	addAnd = true;
	    }
		if (!StringUtils.isEmpty(species))
		{
			if (addAnd)
			{
				query.append(" AND ");
			}
		
			query.append("(");
			query.append(Constants.FIELD_AER_SAAT_CAT).append(":Organism");
	    	query.append(" AND ").append(Constants.FIELD_AER_SAAT_VALUE).append(":\"").append(species).append("\")");
		}
		if (arrayDesId != null && arrayDesId > 0 )
		{
			if (addAnd)
			{
				query.append(" AND ");
			}
			query.append(Constants.FIELD_AER_ARRAYDES_ID).append(":").append(arrayDesId);
				
		}
		return query.toString();

		}
	/**
	 * The methods checks the input parameters that are correct.
	 * @param keywords 
	 * @param species
	 * @param arrayDesId
	 * @param start
	 * @param rows
	 * @return true if parameters are correct, false if not. 
	 */
	public static boolean parseParam(String keywords, String species, Long arrayDesId, Integer start, Integer rows)
	{
		if (StringUtils.isEmpty(keywords) & StringUtils.isEmpty(species) & arrayDesId == null)
			return false;
		if (rows != null && rows == 0)
			return false;
		return true;
	}
	
	public static String convParamSortToFieldName(String name)
	{
		String _convert = Constants.FIELD_AER_RELEASEDATE;
		if (StringUtils.isEmpty(name))
		    return _convert;
		
		if (name.equalsIgnoreCase(XmlHelper.XML_EL_ACCESSION))
		{
			_convert = Constants.FIELD_AER_EXPACCESSION;
		}
		else if (name.equalsIgnoreCase(XmlHelper.XML_EL_NAME))
		{
			_convert = Constants.FIELD_AER_EXPNAME;
		}
		else if (name.equalsIgnoreCase(XmlHelper.XML_EL_SPECIES))
		{
			_convert = Constants.FIELD_AER_SAAT_VALUE;
		}
		else if (name.equalsIgnoreCase(XmlHelper.XML_EL_FGEM))
		{
			_convert = Constants.FIELD_AER_FGEM_COUNT;
		}
		else if (name.equalsIgnoreCase(XmlHelper.XML_EL_RAW))
		{
			_convert = Constants.FIELD_AER_RAW_COUNT;
		}
		return _convert;
	}

	public static ORDER convParamOrderToOrder(String name)
	{
		ORDER _convert = ORDER.asc;
		if (StringUtils.isEmpty(name))
		{
		    return _convert;
		}
		if (name.equalsIgnoreCase("desc"))
		{
			_convert = ORDER.desc;
		}
		return _convert;
	}
	  /**
	     * Performs pagination and full text SOLR search on experiments.
	     * @param query - A lucene query
	     * @param start - a start record
	     * @param rows - maximum number of Documents
	     * @return {@link QueryResponse} or null
	     */
	  
	public static final SolrQuery createSolrQueryForAer(String query, int start, int rows, String sortField, ORDER sortOrder)
	{
	    if (query == null || query.equals(""))
	            return null;
	    SolrQuery q = new SolrQuery(query);
            q.setHighlight(true);
            q.setHighlightSnippets(500);         
            q.addHighlightField(Constants.FIELD_AER_EXPNAME);
            q.addHighlightField(Constants.FIELD_AER_DESC_TEXT);
            q.addHighlightField(Constants.FIELD_AER_BI_AUTHORS);
            q.addHighlightField(Constants.FIELD_AER_BI_TITLE);
            q.addHighlightField(Constants.FIELD_AER_SAAT_VALUE);
            q.addHighlightField(Constants.FIELD_AER_SAAT_CAT);
            q.addHighlightField(Constants.FIELD_AER_FV_OE);
            if (sortField == null)
            {
                q.addSortField(Constants.FIELD_AER_RELEASEDATE, ORDER.asc);
            }
            else                  
            {
                q.addSortField(sortField, sortOrder);
            }            
            q.setRows(rows);
            q.setStart(start);
            return q;
	}
	
	
	private static Map<String, Map<String,String>> createHighlighting(QueryResponse resp)
	{
	    Map<String,Map<String,List<String>>> mapHg=resp.getHighlighting();
	    Map<String, Map<String,String>> map = new HashMap<String, Map<String,String>>();
	    if (mapHg == null || mapHg.isEmpty())
	    {
		return map;
	    }
	    Iterator<Entry<String,Map<String,List<String>>>> it=mapHg.entrySet().iterator();
	    while (it.hasNext())
	    {
		Entry<String,Map<String,List<String>>> entryField=it.next();
		String indexKey=entryField.getKey();
		Iterator<Entry<String,List<String>>> itValue=entryField.getValue().entrySet().iterator();
		while ( itValue.hasNext())
		{
		    Entry<String,List<String>> entry=itValue.next();
		    //name of 
		    String fieldName=entry.getKey();
		    Iterator<String> hgValueIt=entry.getValue().iterator();
		    while (hgValueIt.hasNext())
		    {
			String str=hgValueIt.next();
			int fromIndex = 0;		
			int endIndex = 0;
			while ( (fromIndex=str.indexOf("<em>", fromIndex)) != -1)
			{
				endIndex = str.indexOf("</em>", fromIndex);
				String match=str.substring(fromIndex, endIndex+5);
				String strSearch = match.replace("<em>", "").replace("</em>", "");
				fromIndex=endIndex;
				Map<String,String> mapPair = new HashMap<String, String>();
				
			}
			//
		    }
		}
	    }
	    return map;
	}
	
	private void createHgString(String value, final QueryResponse response)
	{
	    
	}
}
	
	
	

