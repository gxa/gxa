/**
 * EBI Microarray Informatics Team (c) 2007-2008
 */
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
    /** **/
	public static enum ExperimentSource { DW, AE};
	
	/*Constants*/
	public static final String KEY_INDEXDIR="indexdir";
    public static final String KEY_MAGETABDIR="magedir";
    public static final String KEY_INDEXFILE="indexfile";
    public static final String KEY_PROPERTY="property";    
    public static final String VAL_INDEXFILE="multicore.xml";
	public static final String SDRF_EXTENSION=".sdrf.txt";
	public static final String IDF_EXTENSION=".idf.txt";
	public static final String SOLR_CORE_NAME_EXPT="expt";
	public static final String indexBuilderServiceID="indexBuilderService";

	//XML ATTRIBUTES and ELEMENTS
	public static final String EL_experiment = "experiment";
	public static final String EL_users="users";
	public static final String EL_user="user";
	public static final String EL_secondaryaccessions="secondaryaccessions"; 
	public static final String EL_secondaryaccession="secondaryaccession";
	public static final String EL_sampleattributes="sampleattributes";
	public static final String EL_sampleattribute="sampleattribute";
	public static final String EL_factorvalues="factorvalues";
	public static final String EL_factorvalue="factorvalue";
	public static final String EL_miamescores="miamescores"; 
	public static final String EL_miamescore="miamescore";
	public static final String EL_arraydesigns="arraydesigns";
	public static final String EL_arraydesign="arraydesign";
	public static final String EL_bioassaydatagroups = "bioassaydatagroups";
	public static final String EL_bioassaydatagroup = "bioassaydatagroup";
	public static final String EL_bibliography = "bibliography";
	public static final String EL_providers ="providers";
	public static final String EL_provider ="provider";
	public static final String EL_experimentdesigns = "experimentdesigns";
	public static final String EL_experimentdesign = "experimentdesign";
	public static final String EL_description = "description";
	
	//exp_accesion fieeld
	public static final String AT_accnum="accnum";
	public static final String AT_id="id";
	public static final String AT_name="name";
	public static final String AT_releasedate="releasedate";
	public static final String AT_CATEGORY="CATEGORY";
	public static final String AT_VALUE="VALUE";
	public static final String AT_FACTORNAME="FACTORNAME";
	public static final String AT_FV_OE="FV_OE";
	public static final String AT_miamescore="miamescore";
	public static final String AT_value="value";
	public static final String AT_identifier="identifier";
	public static final String AT_count="count";
	public static final String AT_num_bad_cubes="num_bad_cubes";
	public static final String AT_arraydesign="arraydesign";
	public static final String AT_dataformat="dataformat";
	public static final String AT_bioassay_count="bioassay_count";
	public static final String AT_is_derived="is_derived";
	public static final String AT_publication="publication";
	public static final String AT_authors="authors";
	public static final String AT_year="year";
	public static final String AT_volume="volume";
	public static final String AT_issue="issue";
	public static final String AT_pages="pages";
	public static final String AT_contact="contact";
	public static final String AT_role="role";
	public static final String AT_type="type";	

	//INDEX FIELDS
	/** Constant that represents the index fields for DW*/
	//public static final String FIELD_AEEXP_ACCESSION="aeexp_accession";
	
	/** Constant that represents the index fields for Repository*/
	public static final String FIELD_AEEXP_ACCESSION="aeexp_accession";
	public static final String FIELD_AEEXP_ID="aeexp_id";
	public static final String FIELD_AEEXP_NAME="aeexp_name";
	public static final String FIELD_EXP_RELEASEDATE="aeexp_releasedate";
	public static final String FIELD_EXP_USER_ID="exp_user_id";
	public static final String FIELD_EXP_SAAT_CAT = "exp_saat_cat";
	public static final String FIELD_EXP_SAAT_VALUE = "exp_saat_value";
	public static final String FIELD_EXP_FV_FACTORNAME = "exp_fv_factorname";
	public static final String FIELD_EXP_FV_OE = "exp_fv_oe";
	public static final String FIELD_EXP_MIMESCORE_NAME = "exp_mimescore_name";
	public static final String FIELD_EXP_MIMESCORE_VALUE = "exp_mimescore_value";
	public static final String FIELD_EXP_ARRAYDES_ID = "exp_arraydes_id";
	public static final String FIELD_EXP_ARRAYDES_IDENTIFIER = "exp_arraydes_identifier";
	public static final String FIELD_EXP_ARRAYDES_NAME = "exp_arraydes_name";
	public static final String FIELD_EXP_ARRAYDES_COUNT = "exp_arraydes_count";
	public static final String FIELD_EXP_BDG_NAME = "exp_bdg_name";
	public static final String FIELD_EXP_BDG_ID = "exp_bdg_id";
	public static final String FIELD_EXP_BDG_NUM_BAD_CUBES = "exp_bdg_num_bad_cubes";
	public static final String FIELD_EXP_BDG_ARRAYDESIGN = "exp_bdg_arraydesign";
	public static final String FIELD_EXP_BDG_DATAFORMAT = "exp_bdg_dataformat";
	public static final String FIELD_EXP_BDG_BIOASSAY_COUNT = "exp_bdg_bioassay_count";
	public static final String FIELD_EXP_BDG_IS_DERIVED = "exp_bdg_is_derived";
	public static final String FIELD_EXP_BI_PUBLICATION = "exp_bi_publication";
	public static final String FIELD_EXP_BI_AUTHORS = "exp_bi_authors";
	public static final String FIELD_EXP_BI_TITLE = "exp_bi_title";
	public static final String FIELD_EXP_BI_YEAR = "exp_bi_year";
	public static final String FIELD_EXP_BI_VOLUME = "exp_bi_volume";
	public static final String FIELD_EXP_BI_ISSUE = "exp_bi_issue";
	public static final String FIELD_EXP_BI_PAGES = "exp_bi_pages";
	public static final String FIELD_EXP_PROVIDER_CONTRACT = "exp_provider_contact";
	public static final String FIELD_EXP_PROVIDER_ROLE = "exp_provider_role";
	public static final String FIELD_EXP_EXPDES_TYPES = "exp_expdes_type";
	public static final String FIELD_EXP_DESC_ID = "exp_desc_id";	
	public static final String FIELD_EXP_DESC_TEXT="aeexp_desc_text";

    /** */
    private String indexDir;
    /** */   
    private String mageDir;

    //Read configuration from property file 
    private static final Log log = LogFactory.getLog(ConfigurationService.class);

	
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
