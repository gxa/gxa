package ae3.service.search;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServerException;

import uk.ac.ebi.ae3.indexbuilder.Constants;

import ae3.service.ArrayExpressSearchService;

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
	
	private static boolean validateParam(String keywords, String species, Long arrayDesId, Integer start, Integer rows)
	{
		if (StringUtils.isEmpty(keywords) & StringUtils.isEmpty(species) & arrayDesId == null)
			return false;
		if (rows != null && rows == 0)
			return false;
		return true;

	}
	
	private static String parseQuery(String keywords, String species, Long arrayDesId)
	{
		StringTokenizer tok;
		String tabKeywords[];
		String tabSpecies[]; 
		boolean addAnd = false;
		
		
		StringBuffer query = new StringBuffer();
		
		if (!StringUtils.isEmpty(keywords))
    	{
    		tok = new StringTokenizer(keywords," ");
    		tabKeywords=tok.toArray();
    		//create query keywords
    		query.append("(").append(keywords).append(")");
    		boolean addEnd = true;
    	}
		if (!StringUtils.isEmpty(species))
		{
	    	//tok = new StringTokenizer(species," ");
	    	
	    	//tabSpecies = tok.toArray();
	    	//
			if (addAnd)
			{
				query.append(" AND ");
			}
			query.append("(");
			query.append(Constants.FIELD_AER_SAAT_CAT).append(":Organism");
    		query.append(" AND ").append(Constants.FIELD_AER_SAAT_VALUE).append(":\"").append(species).append("\")");
		}
		return query.toString();

	}
	/**
	 * The method return the XML document contains experiment(s).
	 * if keywords, species and arrayDesId is null returns null.
	 * if rows is equal 0 returns null too. 
	 * @param keywords - 
	 * @param species - 
	 * @param arrayDesId - 
	 * @param start - 
	 * @param rows - 
	 * @return the XML document, null if (keywords, species and arrayDesId is null) or rows is 0.   
	 */
	public static String searchIndexAer(String keywords, String species, Long arrayDesId, int start, int rows)
	{
		String query;
		return null;
	}
	//
	//TODO: Add special Exception
	//	
	public static long getNumberOfDoc(String keywords, String species, Long arrayDesId) throws SolrServerException
	{
		if (!validateParam(keywords, species, arrayDesId, null, null))
			return 0;
		String query = parseQuery(keywords, species, arrayDesId);
		return getNumberOfDoc(query);
	}
	
	private static long getNumberOfDoc(String query) throws SolrServerException
	{		
		return ArrayExpressSearchService.instance().getExperimentsCount(query);
	}
}
