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

	//XML Elements which are the same for AE and DW XML
	public static final String EL_experiment = "experiment";

	//XML Attributes and elements for DW
	public static final String EL_assay_attributes = "assay_attributes";
	public static final String EL_sample_attributes = "sample_attributes";
	
	public static final String EL_type ="type";
	public static final String EL_ba_biometric = "ba_biometric";
	public static final String EL_ba_cellline = "ba_cellline" ;
	public static final String EL_ba_celltype = "ba_celltype";
	public static final String EL_ba_clinhistory = "ba_clinhistory";
	public static final String EL_ba_clininfo = "ba_clininfo";
	public static final String EL_ba_clintreatment = "ba_clintreatment";
	public static final String EL_ba_compound = "ba_compound";
	public static final String EL_ba_cultivar = "ba_cultivar";
	public static final String EL_ba_devstage = "ba_devstage";
	public static final String EL_ba_diseaseloc = "ba_diseaseloc";
	public static final String EL_ba_diseasestaging = "ba_diseasestaging";
	public static final String EL_ba_diseasestate = "ba_diseasestate";
	public static final String EL_ba_dose = "ba_dose";
	public static final String EL_ba_ecotype = "ba_ecotype";
	public static final String EL_ba_envhistory = "ba_envhistory";
	public static final String EL_ba_familyhistory = "ba_familyhistory";
	public static final String EL_ba_genmodif = "ba_genmodif";
	public static final String EL_ba_genotype = "ba_genotype";
	public static final String EL_ba_histology = "ba_histology";
	public static final String EL_ba_indgeneticchar = "ba_indgeneticchar";
	public static final String EL_ba_individual = "ba_individual";
	public static final String EL_ba_light = "ba_light";
	public static final String EL_ba_media = "ba_media";
	public static final String EL_ba_observation = "ba_observation";
	public static final String EL_ba_organism = "ba_organism";
	public static final String EL_ba_organismpart = "ba_organismpart";
	public static final String EL_ba_organismstatus = "ba_organismstatus";
	public static final String EL_ba_protocoltype = "ba_protocoltype ";
	public static final String EL_ba_performer = "ba_performer ";
	public static final String EL_ba_phenotype = "ba_phenotype ";
	public static final String EL_ba_qcdescrtype = "ba_qcdescrtype";
	public static final String EL_ba_sex = "ba_sex";
	public static final String EL_ba_strainorline = "ba_strainorline";
	public static final String EL_ba_targetcelltype = "ba_targetcelltype";
	public static final String EL_ba_temperature = "ba_temperature";
	public static final String EL_ba_testtype = "ba_testtype";
	public static final String EL_ba_testresult = "ba_testresult";
	public static final String EL_ba_test = "ba_test";
	public static final String EL_ba_time = "ba_time";
	public static final String EL_ba_tumorgrading = "ba_tumorgrading";
	public static final String EL_ba_vehicle = "ba_vehicle";
	/* Constants for samples element*/
	public static final String EL_bs_unknown = "bs_unknown";
	public static final String EL_BS_AGE = "bs_AGE";
	public static final String EL_BS_BIOMETRIC = "bs_BIOMETRIC";
	public static final String EL_BS_CELLLINE = "bs_CELLLINE";
	public static final String EL_BS_CELLTYPE = "bs_CELLTYPE";
	public static final String EL_BS_CLINHISTORY = "bs_CLINHISTORY";
	public static final String EL_BS_CLININFO = "bs_CLININFO";
	public static final String EL_BS_CLINTREATMENT = "bs_CLINTREATMENT";
	public static final String EL_BS_CULTIVAR = "bs_CULTIVAR";
	public static final String EL_BS_DEVSTAGE = "bs_DEVSTAGE";
	public static final String EL_BS_DISEASELOC = "bs_DISEASELOC";
	public static final String EL_BS_DISEASESTAGING = "bs_DISEASESTAGING";
	public static final String EL_BS_DISEASESTATE = "bs_DISEASESTATE";
	public static final String EL_BS_ECOTYPE = "bs_ECOTYPE";
	public static final String EL_BS_ENVHISTORY = "bs_ENVHISTORY";
	public static final String EL_BS_FAMILYHISTORY = "bs_FAMILYHISTORY";
	public static final String EL_BS_GENMODIF = "bs_GENMODIF";
	public static final String EL_BS_GENOTYPE = "bs_GENOTYPE";
	public static final String EL_BS_HISTOLOGY = "bs_HISTOLOGY";
	public static final String EL_BS_INDGENETICCHAR = "bs_INDGENETICCHAR";
	public static final String EL_BS_INDIVIDUAL = "bs_INDIVIDUAL";
	public static final String EL_BS_INITIALTIME = "bs_INITIALTIME";
	public static final String EL_BS_OBSERVATION = "bs_OBSERVATION";
	public static final String EL_BS_ORGANISMPART = "bs_ORGANISMPART";
	public static final String EL_BS_ORGANISMSTATUS = "bs_ORGANISMSTATUS";
	public static final String EL_BS_PHENOTYPE = "bs_PHENOTYPE";
	public static final String EL_BS_SEX = "bs_SEX";
	public static final String EL_BS_STRAINORLINE = "bs_STRAINORLINE";
	public static final String EL_BS_TARGETCELLTYPE = "bs_TARGETCELLTYPE";
	public static final String EL_BS_TESTRESULT = "bs_TESTRESULT";
	public static final String EL_BS_TESTTYPE = "bs_TESTTYPE";
	public static final String EL_BS_TEST = "bs_TEST";
	public static final String EL_BS_TUMORGRADING = "bs_TUMORGRADING";	
	public static final String AT_ASSAY_ID = "assay_id";
	public static final String AT_SAMPLE_ID= "sample_id";
	

	public static final String [] ARRAY_ASSAY_ELEMENTS = {
	EL_type, EL_ba_biometric, EL_ba_cellline,EL_ba_celltype, EL_ba_clinhistory, 
    EL_ba_clininfo, EL_ba_clintreatment, EL_ba_compound, EL_ba_cultivar, EL_ba_devstage,
    EL_ba_diseaseloc, EL_ba_diseasestaging, EL_ba_diseasestate, EL_ba_dose,EL_ba_ecotype,
    EL_ba_envhistory, EL_ba_familyhistory, EL_ba_genmodif, EL_ba_genotype, EL_ba_histology,
    EL_ba_indgeneticchar, EL_ba_individual, EL_ba_light, EL_ba_media, EL_ba_observation, 
    EL_ba_organism , EL_ba_organismpart, EL_ba_organismstatus, EL_ba_protocoltype,  
    EL_ba_performer, EL_ba_phenotype ,EL_ba_qcdescrtype ,EL_ba_sex , EL_ba_strainorline ,
    EL_ba_targetcelltype , EL_ba_temperature, EL_ba_testtype, EL_ba_testresult, EL_ba_test, 
    EL_ba_time, EL_ba_tumorgrading, EL_ba_vehicle}; 
	/** Tables contains name of child elements samples**/
	public static final String [] ARRAY_SAMPLE_ELEMENTS = {EL_bs_unknown, EL_BS_AGE, EL_BS_BIOMETRIC, EL_BS_CELLLINE,
	EL_BS_CELLTYPE, EL_BS_CLINHISTORY, EL_BS_CLININFO, EL_BS_CLINTREATMENT, EL_BS_CULTIVAR, EL_BS_DEVSTAGE,
	EL_BS_DISEASELOC, EL_BS_DISEASESTAGING,	EL_BS_DISEASESTATE, EL_BS_ECOTYPE, EL_BS_ENVHISTORY, EL_BS_FAMILYHISTORY,
	EL_BS_GENMODIF,	EL_BS_GENOTYPE,EL_BS_HISTOLOGY, EL_BS_INDGENETICCHAR, EL_BS_INDIVIDUAL, EL_BS_INITIALTIME,
	EL_BS_OBSERVATION, EL_BS_ORGANISMPART, EL_BS_ORGANISMSTATUS, EL_BS_PHENOTYPE, EL_BS_SEX, EL_BS_STRAINORLINE,
	EL_BS_TARGETCELLTYPE, EL_BS_TESTRESULT, EL_BS_TESTTYPE, EL_BS_TEST, EL_BS_TUMORGRADING};
	 
	public static final String PREFIX_AEDW="aew_";
	public static final String SUFFIX_ASSAY_ID = "assay_id";
	public static final String SUFFIX_SAMPLE_ID = "sample_id";
	
	//XML ATTRIBUTES and ELEMENTS for AE
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
	

	public static final String FIELD_XML_DOC_AER = "xml_doc_aer";
	public static final String FIELD_EXP_IN_DW = "exp_in_dw"; 
	/** Constant that represents the index fields for Repository*/
	public static final String FIELD_AER_EXPACCESSION="aer_expaccession";
	public static final String FIELD_AER_EXPID="aer_expid";
	public static final String FIELD_AER_EXPNAME="aer_expname";
	public static final String FIELD_AER_RELEASEDATE="aer_releasedate";
	public static final String FIELD_AER_USER_ID="aer_user_id";
	public static final String FIELD_AER_SAAT_CAT = "aer_saat_cat";
	public static final String FIELD_AER_SAAT_VALUE = "aer_saat_value";
	public static final String FIELD_AER_FV_FACTORNAME = "aer_fv_factorname";
	public static final String FIELD_AER_FV_OE = "aer_fv_oe";
	public static final String FIELD_AER_MIMESCORE_NAME = "aer_mimescore_name";
	public static final String FIELD_AER_MIMESCORE_VALUE = "aer_mimescore_value";
	public static final String FIELD_AER_ARRAYDES_ID = "aer_arraydes_id";
	public static final String FIELD_AER_ARRAYDES_IDENTIFIER = "aer_arraydes_identifier";
	public static final String FIELD_AER_ARRAYDES_NAME = "aer_arraydes_name";
	public static final String FIELD_AER_ARRAYDES_COUNT = "aer_arraydes_count";
	public static final String FIELD_AER_BDG_NAME = "aer_bdg_name";
	public static final String FIELD_AER_BDG_ID = "aer_bdg_id";
	public static final String FIELD_AER_BDG_NUM_BAD_CUBES = "aer_bdg_num_bad_cubes";
	public static final String FIELD_AER_BDG_ARRAYDESIGN = "aer_bdg_arraydesign";
	public static final String FIELD_AER_BDG_DATAFORMAT = "aer_bdg_dataformat";
	public static final String FIELD_AER_BDG_BIOASSAY_COUNT = "aer_bdg_bioassay_count";
	public static final String FIELD_AER_BDG_IS_DERIVED = "aer_bdg_is_derived";
	public static final String FIELD_AER_BI_PUBLICATION = "aer_bi_publication";
	public static final String FIELD_AER_BI_AUTHORS = "aer_bi_authors";
	public static final String FIELD_AER_BI_TITLE = "aer_bi_title";
	public static final String FIELD_AER_BI_YEAR = "aer_bi_year";
	public static final String FIELD_AER_BI_VOLUME = "aer_bi_volume";
	public static final String FIELD_AER_BI_ISSUE = "aer_bi_issue";
	public static final String FIELD_AER_BI_PAGES = "aer_bi_pages";
	public static final String FIELD_AER_PROVIDER_CONTRACT = "aer_provider_contact";
	public static final String FIELD_AER_PROVIDER_ROLE = "aer_provider_role";
	public static final String FIELD_AER_EXPDES_TYPES = "aer_aerdes_type";
	public static final String FIELD_AER_DESC_ID = "aer_desc_id";	
	public static final String FIELD_AER_DESC_TEXT="aer_desc_text";

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
