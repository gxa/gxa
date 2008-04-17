/**
 * EBI Microarray Informatics Team (c) 2007-2008
 */
package uk.ac.ebi.ae3.indexbuilder;

import org.apache.commons.cli2.Argument;
import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.commons.cli2.option.DefaultOption;
import org.apache.commons.cli2.util.HelpFormatter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

import uk.ac.ebi.ae3.indexbuilder.service.ConfigurationService;
import uk.ac.ebi.ae3.indexbuilder.service.IndexBuilderService;

/**
 * The main class which contains main method. Create expt lucene index.
 * Configuration is stored in app-context.xml file
 * 
 * @author mdylag
 * 
 */
public class IndexBuilder
{
	/** */
	private final HelpFormatter		helpFormatter   = new HelpFormatter();
	
	private final DefaultOptionBuilder optionBuilder   = new DefaultOptionBuilder();
	private final ArgumentBuilder	  argumentBuilder = new ArgumentBuilder();
	private final GroupBuilder		 groupBuilder	= new GroupBuilder();
	
	private final Argument			 pathArgument	= argumentBuilder
															   .withName("path")
															   .withMaximum(1)
															   .withMaximum(1)
															   .create();
	
	private final DefaultOption		optionProperty  = optionBuilder
															   .withLongName(
																	   ConfigurationService.KEY_PROPERTY)
															   .withRequired(
																	   true)
															   .withArgument(
																	   pathArgument)
															   .withRequired(
																	   true)
															   .withDescription(
																	   "Property file")
															   .create();
	private String					 propertyFile;
	private XmlBeanFactory appContext; 	
	
	private static final Log		   log			 = LogFactory
															   .getLog(IndexBuilder.class);
	
	public static void main(String[] args)
	{
		try
		{
			IndexBuilder app = new IndexBuilder();
			app.parse(args);
			app.run();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			log.error(e);
			System.exit(-1);
		}
		catch (IndexException e)
		{
			e.printStackTrace();
			log.error(e);
			System.exit(-1);
		}
		
	}
	protected void startContext()
	{
		PropertyPlaceholderConfigurer conf = new PropertyPlaceholderConfigurer();
		conf.setLocation(new FileSystemResource(propertyFile));
		
		appContext = new XmlBeanFactory(new ClassPathResource(
				"app-context.xml"));
		conf.postProcessBeanFactory(appContext);
		
	}
	/**
	 * DOCUMENT ME
	 * @throws Exception
	 * @throws IndexException
	 */
	
	protected void run() throws Exception, IndexException
	{
		IndexBuilderService indexBuilderService = (IndexBuilderService) appContext
				.getBean(ConfigurationService.indexBuilderServiceID);
		indexBuilderService.buildIndex();
		
	}
	
	protected boolean parse(String[] args)
	{
		Group groupOptions = groupBuilder.withOption(optionProperty).create();
		Parser parser = new Parser();
		
		parser.setGroup(groupOptions);
		parser.setHelpFormatter(helpFormatter);
		parser.setHelpTrigger("--help");
		CommandLine cl = parser.parseAndHelp(args);
		if (cl == null || !cl.hasOption(optionProperty))
		{
			helpFormatter.printException();
			return false;
		}
		else 
		{
			propertyFile = (String) cl.getValue(optionProperty);
			return true;
			
		}
		
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
