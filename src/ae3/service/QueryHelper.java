package ae3.service;

import org.apache.commons.lang.StringUtils;

import com.Ostermiller.util.StringTokenizer;

import uk.ac.ebi.ae3.indexbuilder.Constants;

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
	    	//create query keywords
	    	query.append("(").append(keywords).append(")");
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
	
	public static boolean parseParam(String keywords, String species, Long arrayDesId, Integer start, Integer rows)
	{
		if (StringUtils.isEmpty(keywords) & StringUtils.isEmpty(species) & arrayDesId == null)
			return false;
		if (rows != null && rows == 0)
			return false;
		return true;

	}


}
	
	
	

