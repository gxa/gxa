/**
 * EBI Microarray Informatics Team (c) 2007-2008
 */
package uk.ac.ebi.ae3.indexbuilder.service;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.core.MultiCore;
import org.xml.sax.SAXException;

import uk.ac.ebi.ae3.indexbuilder.IndexBuilderException;
/**
 * 
 * @author mdylag
 *
 */
public abstract class IndexBuilderService
{
	protected UpdateResponse response;
	protected org.apache.solr.client.solrj.SolrServer solr;

	private ConfigurationService confService;
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
		startupSolr();
	}
	
	/**
	 * 
	 * @throws ParserConfigurationException
	 * @throws IOException
	 * @throws SAXException
	 */
	private void startupSolr() 	throws ParserConfigurationException, IOException, SAXException
	{
        MultiCore.getRegistry().load(getConfService().getIndexDir(), new File(getConfService().getIndexDir(), ConfigurationService.VAL_INDEXFILE));
        this.solr = new EmbeddedSolrServer(ConfigurationService.SOLR_CORE_NAME);

	}
	
	/**
	 * 
	 * @throws SolrServerException
	 * @throws IOException
	 */
	private void shutdownSolr() throws SolrServerException, IOException
	{
       response = solr.commit();
       response = solr.optimize();
       MultiCore.getRegistry().shutdown();		
		
	}

	/**
	 * 
	 * @throws SolrServerException
	 * @throws IOException
	 */
	protected void dispose() throws SolrServerException, IOException
	{
		shutdownSolr();
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void buildIndex() throws Exception
	{
		try
		{
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

}
