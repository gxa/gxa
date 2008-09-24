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

import uk.ac.ebi.ae3.indexbuilder.service.IndexBuilderService;

/**
 * The main class which contains main method. Create expt lucene index.
 * Configuration is stored in app-context.xml file
 * 
 * @author mdylag
 * 
 */
public class App
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
																	   Constants.KEY_PROPERTY)
															   .withRequired(
																	   false)
															   .withArgument(
																	   pathArgument)
															   .withDescription(
																	   "Property file")
															   .create();
	private String					 propertyFile;
	private XmlBeanFactory appContext; 	
	
	private static final Log		   log			 = LogFactory
															   .getLog(App.class);
	
	public static void main(String[] args)
	{
		try
		{
			App app = new App();
			//TODO: Add exception to parse method
			app.parse(args);
			app.startContext();		
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
		conf.setLocation(propertyFile == null ? new ClassPathResource("indexbuilder.properties")
                : new FileSystemResource(propertyFile));
		
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
				.getBean(Constants.indexBuilderServiceID);
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
		if (cl == null)
		{
			helpFormatter.printException();
			return false;
		}
		else 
		{
			propertyFile = cl.hasOption(optionProperty) ? (String) cl.getValue(optionProperty) : null;
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
