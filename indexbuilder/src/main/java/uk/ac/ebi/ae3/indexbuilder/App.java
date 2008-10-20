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
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

import uk.ac.ebi.ae3.indexbuilder.service.IndexBuilderService;
import uk.ac.ebi.ae3.indexbuilder.service.GeneAtlasIndexBuilder;

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

    private final Argument			 modeArgument	= argumentBuilder
            .withName("mode")
            .withMinimum(1)
            .withMaximum(2)
            .withDefault("expt")
            .withSubsequentSeparator(',')
            .create();


    private final DefaultOption     optionBuild = optionBuilder
            .withLongName("build")
            .withRequired(false)
            .withArgument(modeArgument)
            .withDescription("Indexes to build")
            .create();

    private final DefaultOption     optionUpdate = optionBuilder
            .withLongName("update")
            .withRequired(false)
            .withDescription("Update mode")
            .create();

    private String					 propertyFile;
	private XmlBeanFactory appContext; 	
	
	private static final Log		   log			 = LogFactory
															   .getLog(App.class);

    private boolean buildExpt = false;
    private boolean buildGene = false;

    private boolean updateMode = false;

    public static void main(String[] args)
	{
		try
		{
			App app = new App();
			//TODO: Add exception to parse method
            if(app.parse(args)) {
                app.startContext();
                app.run();
            }
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
        IndexBuilderService indexBuilderService;

        log.info("Will build indexes: " + (buildExpt ? "experiments " : "") + (buildGene ? "gene" : "")
                + (updateMode ? " (update mode)" : ""));

        if(buildExpt) {
            log.info("Building experiments index");
            indexBuilderService = (IndexBuilderService) appContext
                    .getBean(Constants.exptIndexBuilderServiceID);
            indexBuilderService.setUpdateMode(updateMode);
            indexBuilderService.buildIndex();
        }

        if(buildGene) {
            log.info("Building atlas gene index");
            indexBuilderService = (IndexBuilderService) appContext
                    .getBean(Constants.geneIndexBuilderServiceID);
            indexBuilderService.setUpdateMode(updateMode);
            indexBuilderService.buildIndex();
        }
	}
	
	protected boolean parse(String[] args)
	{
		Group groupOptions = groupBuilder.withOption(optionProperty)
                .withOption(optionBuild)
                .withOption(optionUpdate)
                .create();
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
            updateMode = cl.hasOption(optionUpdate);

            if(cl.hasOption(optionBuild)) {
                for(Object s: cl.getValues(optionBuild)) {
                    if("expt".equals(s))
                        buildExpt = true;
                    else if("gene".equals(s))
                        buildGene = true;
                }
            }

            if(!buildGene && !buildExpt)
                buildExpt = true;

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
