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

import sun.util.logging.resources.logging;
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
    private IndexBuilder indexBuilder;
    private boolean runSetUp = false;
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		if (runSetUp)
		{
			
		}
	}
	
	@Override
	protected void tearDown() throws Exception
	{
		if (runSetUp)
		{
			
		}
	}
	/**
	 * 
	 * @return location of the property file
	 */
	public String getPropertyFileLocation()
	{
		return "resource/indexbuilder.properties";
	}

}
