package uk.ac.ebi.ae3.indexbuilder.service;

import org.apache.commons.cli2.Argument;
import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.commons.cli2.option.DefaultOption;
import org.apache.commons.cli2.util.HelpFormatter;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.ebi.ae3.indexbuilder.IndexBuilder;

/**
 * The facade class for command line arguments
 * @author mdylag
 *
 */
public class ConfigurationService
{
    /*Constants*/
    private static final String KEY_INDEXDIR="indexdir";
    private static final String KEY_MAGETABDIR="magedir";
    private static final String KEY_INDEXFILE="indexfile";
    private static final String KEY_PROPERTY="property";    
    private static final String VAL_INDEXFILE="multicore.xml";


    private String indexDir;
    private String mageDir;
    private String propertyFile;

	//CLI library - parse comman line arguments
    private final HelpFormatter helpFormatter = new HelpFormatter();
    private final DefaultOptionBuilder optionBuilder = new DefaultOptionBuilder();
    private final ArgumentBuilder argumentBuilder = new ArgumentBuilder();
    private final GroupBuilder groupBuilder = new GroupBuilder();
    
    private final Argument pathArgument = argumentBuilder.withName("path").withMaximum(1).withMaximum(1).create();
    private final DefaultOption optionIndexDir = optionBuilder.withLongName(ConfigurationService.KEY_INDEXDIR).
    											 withArgument(pathArgument).withDescription("an index directory").
    											 create();
    
    private final DefaultOption optionMageTabDir = optionBuilder.withLongName(ConfigurationService.KEY_MAGETABDIR).
	 											   withArgument(pathArgument).    
	 											   withDescription("an files directory").create();

    private final DefaultOption optionProperty = optionBuilder.withLongName(ConfigurationService.KEY_PROPERTY).withRequired(true).
	 											 withArgument(pathArgument).
	 											 withDescription("Property file").create();
    //Read configuration from property file 
    private final CompositeConfiguration config= new CompositeConfiguration();

    private static final Log log = LogFactory.getLog(IndexBuilder.class);

	
	public void parseAndConfigure(String[] args) throws org.apache.commons.configuration.ConfigurationException
	{
    	Group groupOptions=groupBuilder.withOption(optionIndexDir).withOption(optionMageTabDir).withOption(optionProperty).create();
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
    	PropertiesConfiguration propConf = new PropertiesConfiguration(propertyFile);
        config.addConfiguration(propConf);
    	
    	if (cl.hasOption(optionIndexDir))
    	{
    		indexDir = (String)cl.getValue(optionIndexDir);
    	}
    	else
    	{
            indexDir = config.getString(ConfigurationService.KEY_INDEXDIR);    		
    	}
    	
    	if (cl.hasOption(optionMageTabDir))
    	{
    		mageDir = (String)cl.getValue(optionMageTabDir);
    	}
    	else {
            mageDir = config.getString(ConfigurationService.KEY_MAGETABDIR);    		
    	}
    	
        log.info("The IndexBuilder reads properies from " + propConf.getBasePath());
		
	}


	public String getIndexDir()
	{
		return indexDir;
	}

	public String getMageDir()
	{
		return mageDir;
	}
}
