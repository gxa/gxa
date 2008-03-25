/**
 * 
 */
package uk.ac.ebi.ae3.indexbuilder;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli2.Argument;
import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.commons.cli2.option.DefaultOption;
import org.apache.commons.cli2.util.HelpFormatter;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServerException;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.xml.sax.SAXException;

import uk.ac.ebi.ae3.indexbuilder.service.ConfigurationService;
import uk.ac.ebi.ae3.indexbuilder.service.IndexBuilderService;

/**
 * @author mdylag
 *
 */
public class IndexBuilder
{
    private final HelpFormatter helpFormatter = new HelpFormatter();
    private final DefaultOptionBuilder optionBuilder = new DefaultOptionBuilder();
    private final ArgumentBuilder argumentBuilder = new ArgumentBuilder();
    private final GroupBuilder groupBuilder = new GroupBuilder();
    
    private final Argument pathArgument = argumentBuilder.withName("path").withMaximum(1).withMaximum(1).create();
    
    private final DefaultOption optionProperty = optionBuilder.withLongName(ConfigurationService.KEY_PROPERTY).withRequired(true).
	 											 withArgument(pathArgument).withRequired(true).
	 											 withDescription("Property file").create();
    private String propertyFile;
    
    private static final Log log = LogFactory.getLog(IndexBuilder.class);

    
	public static void main(String[] args)
	{
		try
		{
			IndexBuilder app = new IndexBuilder();
			app.parse(args);
			app.run();		
     	} catch (java.io.IOException e) {
            log.error(e);
            System.exit(-1);
        } catch (SolrServerException e) {
            log.error(e);
            System.exit(-1);            
        } catch (ParserConfigurationException e) {
            log.error(e);
            System.exit(-1);            
        } catch (SAXException e) {
            log.error(e);
            System.exit(-1);            
        }
        catch (IndexBuilderException e) {
        	log.error(e);
            System.exit(-1);        	
        }
        
	}
	
	public void run() throws IOException, SolrServerException, ParserConfigurationException, SAXException, IndexBuilderException
	{
		PropertyPlaceholderConfigurer conf = new PropertyPlaceholderConfigurer();
		conf.setLocation(new FileSystemResource(propertyFile));	
		
	    XmlBeanFactory appContext = new XmlBeanFactory(new ClassPathResource("app-context.xml"));
    	conf.postProcessBeanFactory(appContext);
    	IndexBuilderService indexBuilderService = (IndexBuilderService) appContext.getBean(ConfigurationService.indexBuilderServiceID);
    	indexBuilderService.buildIndex();
	    
		
	}
	
	public void parse(String[] args)
	{
    	Group groupOptions=groupBuilder.withOption(optionProperty).create();
    	Parser parser = new Parser();
    	
    	parser.setGroup(groupOptions);
    	parser.setHelpFormatter(helpFormatter);
    	parser.setHelpTrigger("--help");
    	CommandLine cl = parser.parseAndHelp(args);
    	if (cl==null)
    	{
    		helpFormatter.printException();
    		System.exit(-1);
    	}
    	if (cl.hasOption(optionProperty))
    	{
    		propertyFile = (String)cl.getValue(optionProperty);
    		
    	}    	
	
	}
	
}
