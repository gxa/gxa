package ae3.service;

import uk.ac.ebi.ae3.indexbuilder.Constants;

/**
 * The helper methods for working with lucene query.
 * @author mdylag
 *
 */
public class QueryHelper
{
	/**
	 * Creates query for full text search.
	 * @param keywords - table of keywords
	 * @return
	 */
	public static final String createQuery(String[] keywords)
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
	
}
