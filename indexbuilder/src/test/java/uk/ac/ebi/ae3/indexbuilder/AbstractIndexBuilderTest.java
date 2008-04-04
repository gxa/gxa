/**
 * 
 */
package uk.ac.ebi.ae3.indexbuilder;

import java.io.File;

import junit.framework.TestCase;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.MultiCore;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

import uk.ac.ebi.ae3.indexbuilder.service.ConfigurationService;
import uk.ac.ebi.ae3.indexbuilder.service.IndexBuilderService;
/**
 * An Abstract base class that makes JUnit tests easier. 
 * @author mdylag
 * <p>
 * 
 * </p>
 * @see #setUp
 * @see #tearDown
 */
public class AbstractIndexBuilderTest extends TestCase
{
    private SolrServer solr_gene;
    private SolrServer solrExpt;
    private XmlBeanFactory appContext;
    private MultiCore multiCore;

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();

		//read application context
		PropertyPlaceholderConfigurer conf = new PropertyPlaceholderConfigurer();
		conf.setLocation(new FileSystemResource(getPropertyFileLocation()));	

		
	    appContext = new XmlBeanFactory(new ClassPathResource("app-context.xml"));
    	conf.postProcessBeanFactory(appContext);
    	IndexBuilderService indexBuilderService = (IndexBuilderService) appContext.getBean(ConfigurationService.indexBuilderServiceID);
    	try {
	    indexBuilderService.buildIndex();
	} catch (IndexException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

    	ConfigurationService configurationService=(ConfigurationService)appContext.getBean("configurationService");
        multiCore = new MultiCore(configurationService.getIndexDir(), new File(configurationService.getIndexDir(), ConfigurationService.VAL_INDEXFILE));
        solr_gene = new EmbeddedSolrServer(multiCore, "gene");
        solrExpt = new EmbeddedSolrServer(multiCore, "expt");
        

		//initialize solr
	}
	
	@Override
	protected void tearDown() throws Exception
	{
		super.tearDown();
		//shutdown solr
		multiCore.shutdown();		
	}
	/**
	 * 
	 * @return location of the property file
	 */
	public String getPropertyFileLocation()
	{
		return "resource/indexbuilder.properties";
	}

	public SolrServer getSolr_gene()
	{
		return solr_gene;
	}

	public void setSolr_gene(SolrServer solr_gene)
	{
		this.solr_gene = solr_gene;
	}

	public SolrServer getSolrExpt()
	{
		return solrExpt;
	}

	public void setSolrExpt(SolrServer solr_expt)
	{
		this.solrExpt = solr_expt;
	}

	public XmlBeanFactory getAppContext()
	{
		return appContext;
	}

	public void setAppContext(XmlBeanFactory appContext)
	{
		this.appContext = appContext;
	}
}
