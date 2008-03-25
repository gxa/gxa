package uk.ac.ebi.ae3.indexbuilder.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The facade class for command line arguments
 * @author mdylag
 *
 */
public class ConfigurationService
{
    /*Constants*/
	public static final String KEY_INDEXDIR="indexdir";
    public static final String KEY_MAGETABDIR="magedir";
    public static final String KEY_INDEXFILE="indexfile";
    public static final String KEY_PROPERTY="property";    
    public static final String VAL_INDEXFILE="multicore.xml";
	public static final String SDRF_EXTENSION=".sdrf.txt";
	public static final String IDF_EXTENSION=".idf.txt";
	public static final String SOLR_CORE_NAME="expt";
	public static final String indexBuilderServiceID="indexBuilderService";



    private String indexDir;
    private String mageDir;

	//CLI library - parse comman line arguments
    //Read configuration from property file 
    private static final Log log = LogFactory.getLog(ConfigurationService.class);

	
	public void parseAndConfigure(String[] args) throws org.apache.commons.configuration.ConfigurationException
	{
		
	}


	public String getIndexDir()
	{
		return indexDir;
	}

	public String getMageDir()
	{
		return mageDir;
	}


	public void setIndexDir(String indexDir)
	{
		this.indexDir = indexDir;
	}


	public void setMageDir(String mageDir)
	{
		this.mageDir = mageDir;
	}
}
