/**
 * 
 */
package uk.ac.ebi.ae3.web;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.core.MultiCore;
import org.xml.sax.SAXException;

import uk.ac.ebi.ae3.indexbuilder.service.ConfigurationService;

/**
 * 
 * @author mdylag
 *
 */
public class IndexQueryService
{

	private ConfigurationService conf = new ConfigurationService();
	protected org.apache.solr.client.solrj.SolrServer solr;

	/**
	 * @throws SAXException 
	 * @throws IOException 
	 * @throws ParserConfigurationException 
	 * 
	 */
	public IndexQueryService() throws ParserConfigurationException, IOException, SAXException
	{
		conf.setIndexDir("D:\\tools\\ebi_env\\multicore\\");
		startupSolr();
	}
	
	private void startupSolr() throws ParserConfigurationException, IOException, SAXException
	{
        MultiCore.getRegistry().load(conf.getIndexDir(), new File(conf.getIndexDir(), ConfigurationService.VAL_INDEXFILE));
        this.solr = new EmbeddedSolrServer(ConfigurationService.SOLR_CORE_NAME);

	}
	
	private void shutdownSolr()
	{
		MultiCore.getRegistry().shutdown();
	}
	
	public void dispose()
	{
		shutdownSolr();
	}
	
	public void getExperiments() throws SolrServerException
	{
		  String query = ConfigurationService.FIELD_EXP_ACCESSION + ":A* or E*";
		  SolrQuery q = new SolrQuery();
		  q.setQuery(query);
		  q.setShowDebugInfo(true);
          QueryResponse resp=solr.query(q);
          SolrDocumentList sList=resp.getResults();          
          Iterator<SolrDocument> it =sList.iterator();
          System.out.println("Size is " + sList.size() + " another ");
          while (it.hasNext())
          {
        	  SolrDocument doc=it.next();
        	  System.out.println(doc.getFieldValue(ConfigurationService.FIELD_EXP_ACCESSION ));
          }
          

	}
	
	public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, SolrServerException
	{
		IndexQueryService idx = new IndexQueryService();
		idx.getExperiments();
		idx.dispose();
	}
	
	
}
