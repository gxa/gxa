package ae3.service;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery.ORDER;

import ae3.service.search.XmlHelper;

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


}
	
	
	

