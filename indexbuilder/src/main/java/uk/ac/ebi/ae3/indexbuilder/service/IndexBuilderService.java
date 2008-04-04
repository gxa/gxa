/**
 * EBI Microarray Informatics Team (c) 2007-2008
 */
package uk.ac.ebi.ae3.indexbuilder.service;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.core.MultiCore;
import org.apache.solr.core.SolrCore;
import org.xml.sax.SAXException;

import uk.ac.ebi.ae3.indexbuilder.IndexBuilderException;
import uk.ac.ebi.ae3.indexbuilder.IndexException;
/**
 * 
 * @author mdylag
 *
 */
public abstract class IndexBuilderService
{
	protected UpdateResponse response;
	//protected SolrServer solr;
	private ConfigurationService confService;
	//private SolrCore exptCore;
	//private MultiCore multiCore;
	private SolrEmbededIndex solrEmbededIndex;
	/** */
	protected static final Log log = LogFactory.getLog(IndexBuilderService.class);

	
	
	/**
	 * 
	 * @param confService
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 */
	public IndexBuilderService(ConfigurationService confService) throws ParserConfigurationException, IOException, SAXException

	{
		this.confService = confService;
	}
	
	
	

	/**
	 * 
	 * @throws SolrServerException
	 * @throws IOException
	 */
	protected void dispose() throws SolrServerException, IOException
	{
	   solrEmbededIndex.commit();
	   solrEmbededIndex.dispose();
	}

	/**
	 * 
	 * @throws Exception
	 * @throws IndexException 
	 */
	public void buildIndex() throws Exception, IndexException
	{
		try
		{
		    solrEmbededIndex.init();
		    createIndexDocs();		
		}
		catch (Exception e)
		{
			throw new IndexBuilderException(e);
		}		
		finally
		{
		    dispose();			
		}
	}
	
	protected abstract void createIndexDocs() throws Exception;

	public ConfigurationService getConfService()
	{
		return confService;
	}

	public SolrEmbededIndex getSolrEmbededIndex() {
	    return solrEmbededIndex;
	}

	public void setSolrEmbededIndex(SolrEmbededIndex solrEmbededIndex) {
	    this.solrEmbededIndex = solrEmbededIndex;
	}

}
