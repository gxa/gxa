package uk.ac.ebi.ae3.indexbuilder;


/**
 * The class contains constants members.
 * @author mdylag
 *
 */
public class Constants
{
	/** **/
	public static enum ExperimentSource { DW, AE}
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
	public static final String AT_SAMPLE_ID= "sample_id";
	public static final String AT_ASSAY_ID = "assay_id";
	public static final String EL_BS_TUMORGRADING = "bs_TUMORGRADING";
	public static final String EL_ba_diseasestate = "ba_diseasestate";
	public static final String [] ARRAY_ASSAY_ELEMENTS = {
	EL_type, EL_ba_biometric, EL_ba_cellline,EL_ba_celltype, EL_ba_clinhistory, 
	EL_ba_clininfo, EL_ba_clintreatment, EL_ba_compound, EL_ba_cultivar, EL_ba_devstage,
	EL_ba_diseaseloc, EL_ba_diseasestaging, EL_ba_diseasestate, Constants.EL_ba_dose,Constants.EL_ba_ecotype,
	Constants.EL_ba_envhistory, Constants.EL_ba_familyhistory, Constants.EL_ba_genmodif, Constants.EL_ba_genotype, Constants.EL_ba_histology,
	Constants.EL_ba_indgeneticchar, Constants.EL_ba_individual, Constants.EL_ba_light, Constants.EL_ba_media, Constants.EL_ba_observation, 
	Constants.EL_ba_organism , Constants.EL_ba_organismpart, Constants.EL_ba_organismstatus, Constants.EL_ba_protocoltype,  
	Constants.EL_ba_performer, Constants.EL_ba_phenotype ,Constants.EL_ba_qcdescrtype ,Constants.EL_ba_sex , Constants.EL_ba_strainorline ,
	Constants.EL_ba_targetcelltype , Constants.EL_ba_temperature, Constants.EL_ba_testtype, Constants.EL_ba_testresult, Constants.EL_ba_test, 
	Constants.EL_ba_time, Constants.EL_ba_tumorgrading, Constants.EL_ba_vehicle};
	/** Tables contains name of child elements samples**/
	public static final String [] ARRAY_SAMPLE_ELEMENTS = {Constants.EL_bs_unknown, Constants.EL_BS_AGE, Constants.EL_BS_BIOMETRIC, Constants.EL_BS_CELLLINE,
	Constants.EL_BS_CELLTYPE, Constants.EL_BS_CLINHISTORY, Constants.EL_BS_CLININFO, Constants.EL_BS_CLINTREATMENT, Constants.EL_BS_CULTIVAR, Constants.EL_BS_DEVSTAGE,
	Constants.EL_BS_DISEASELOC, Constants.EL_BS_DISEASESTAGING,	Constants.EL_BS_DISEASESTATE, Constants.EL_BS_ECOTYPE, Constants.EL_BS_ENVHISTORY, Constants.EL_BS_FAMILYHISTORY,
	Constants.EL_BS_GENMODIF,	Constants.EL_BS_GENOTYPE,Constants.EL_BS_HISTOLOGY, Constants.EL_BS_INDGENETICCHAR, Constants.EL_BS_INDIVIDUAL, Constants.EL_BS_INITIALTIME,
	Constants.EL_BS_OBSERVATION, Constants.EL_BS_ORGANISMPART, Constants.EL_BS_ORGANISMSTATUS, Constants.EL_BS_PHENOTYPE, Constants.EL_BS_SEX, Constants.EL_BS_STRAINORLINE,
	Constants.EL_BS_TARGETCELLTYPE, Constants.EL_BS_TESTRESULT, Constants.EL_BS_TESTTYPE, Constants.EL_BS_TEST, EL_BS_TUMORGRADING};
	//exp_accesion fieeld
	public static final String AT_accession="accession";
	public static final String AT_CATEGORY="CATEGORY";
	public static final String AT_count="count";
	public static final String AT_arraydesign="arraydesign";
	public static final String AT_bioassay_count="bioassay_count";
	public static final String AT_authors="authors";
	public static final String AT_contact="contact";
	public static final String AT_id="id";
	public static final String AT_name="name";
	public static final String AT_FACTORNAME="FACTORNAME";
	public static final String AT_FV_OE="FV_OE";
	public static final String AT_miamescore="miamescore";
	public static final String AT_identifier="identifier";
	public static final String AT_num_bad_cubes="num_bad_cubes";
	public static final String AT_dataformat="dataformat";
	public static final String AT_is_derived="is_derived";
	public static final String AT_issue="issue";
	public static final String AT_pages="pages";
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
	public static final String EL_arraydesign="arraydesign";
	public static final String AT_releasedate="releasedate";
						   

	public static final String AT_value="value";
	public static final String AT_publication="publication";
	public static final String AT_year="year";
	public static final String AT_volume="volume";
	public static final String AT_role="role";
	public static final String AT_type="type";
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
	public static final String EL_BS_AGE = "bs_AGE";
	public static final String EL_BS_BIOMETRIC = "bs_BIOMETRIC";
	public static final String EL_BS_CELLLINE = "bs_CELLLINE";
	public static final String EL_BS_CELLTYPE = "bs_CELLTYPE";
	public static final String EL_BS_CLINHISTORY = "bs_CLINHISTORY";
	public static final String EL_bioassaydatagroup = "bioassaydatagroup";
	public static final String EL_bibliography = "bibliography";
	/* Constants for samples element*/
	public static final String EL_bs_unknown = "bs_unknown";
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
	public static final String EL_factorvalue="experimentalfactor";
	//public static final String EL_factorvalue="factorvalue";
	public static final String EL_experimentdesign = "experimentdesign";
	public static final String EL_description = "description";
	//XML ATTRIBUTES and ELEMENTS for AE
	public static final String EL_users="users";
	public static final String EL_user="user";
	public static final String EL_secondaryaccessions="secondaryaccessions";
	public static final String EL_secondaryaccession="secondaryaccession";
	//public static final String EL_sampleattributes="sampleattributes";
	public static final String EL_sampleattribute="sampleattribute";
	public static final String EL_score="scotre";
	public static final String EL_miamescore="miamescore";
	public static final String EL_provider ="provider";

	/*XML attribute for DW*/
	public static final String AT_EXPERIMENT_ID_KEY = "EXPERIMENT_ID_KEY";
	public static final String AT_EXPERIMENT_IDENTIFIER="EXPERIMENT_IDENTIFIER";
	public static final String AT_EXPERIMENT_DESCRIPTION="EXPERIMENT_DESCRIPTION";

	/** Constant that represents the index fields for Repository*/
	public static final String FIELD_AER_ARRAYDES_ID = "aer_arraydes_id";
	public static final String FIELD_AER_ARRAYDES_IDENTIFIER = "aer_txt_arraydes_identifier";
	public static final String FIELD_AER_ARRAYDES_NAME = "aer_txt_arraydes_name";
	public static final String FIELD_AER_ARRAYDES_COUNT = "aer_arraydes_count";
	public static final String FIELD_AER_BDG_NAME = "aer_txt_bdg_name";
	public static final String FIELD_AER_BDG_ID = "aer_bdg_id";
	public static final String FIELD_AER_BDG_NUM_BAD_CUBES = "aer_bdg_num_bad_cubes";
	public static final String FIELD_AER_BDG_ARRAYDESIGN = "aer_txt_bdg_arraydesign";
	public static final String FIELD_AER_BDG_DATAFORMAT = "aer_bdg_dataformat";
	public static final String FIELD_AER_BDG_BIOASSAY_COUNT = "aer_bdg_bioassay_count";
	public static final String FIELD_AER_BDG_IS_DERIVED = "aer_bdg_is_derived";
	public static final String FIELD_AER_EXPACCESSION="aer_txt_expaccession";
	public static final String FIELD_AER_EXPID="aer_expid";
	public static final String FIELD_AER_EXPNAME="aer_txt_expname";
	public static final String FIELD_AER_RELEASEDATE="aer_releasedate";
	public static final String FIELD_AER_USER_ID="aer_user_id";
	public static final String FIELD_AER_SAAT_CAT = "aer_txt_saat_cat";
	public static final String FIELD_AER_SAAT_VALUE = "aer_txt_saat_value";
	public static final String FIELD_AER_FV_FACTORNAME = "aer_txt_fv_factorname";
	public static final String FIELD_AER_FV_OE = "aer_txt_fv_oe";
	public static final String FIELD_AER_MIMESCORE_NAME = "aer_txt_mimescore_name";
	public static final String FIELD_AER_MIMESCORE_VALUE = "aer_txt_mimescore_value";

	public static final String FIELD_AER_BI_ACCESSION = "aer_bi_accession";
	public static final String FIELD_AER_BI_PUBLICATION = "aer_txt_bi_publication";
	public static final String FIELD_AER_BI_AUTHORS = "aer_txt_bi_authors";
	public static final String FIELD_AER_BI_TITLE = "aer_txt_bi_title";
	public static final String FIELD_AER_BI_YEAR = "aer_bi_year";
	public static final String FIELD_AER_BI_VOLUME = "aer_bi_volume";
	public static final String FIELD_AER_BI_ISSUE = "aer_bi_issue";
	public static final String FIELD_AER_BI_PAGES = "aer_bi_pages";
	public static final String FIELD_AER_BI_URI = "aer_bi_uri";
	
	public static final String FIELD_AER_PROVIDER_CONTRACT = "aer_txt_provider_contact";
	public static final String FIELD_AER_PROVIDER_ROLE = "aer_txt_provider_role";
	public static final String FIELD_AER_PROVIDER_EMAIL = "aer_txt_provider_email";
	public static final String FIELD_AER_EXPDES_TYPE = "aer_txt_aerdes_type";
	public static final String FIELD_AER_DESC_ID = "aer_desc_id";
	public static final String FIELD_AER_DESC_TEXT="aer_txt_desc_text";
	public static final String FIELD_AER_TOTAL_HYBS = "aer_total_hybs";
	public static final String FIELD_AER_TOTAL_SAMPL = "aer_total_samples";
	public static final String FIELD_AER_FILE_SDRF = "aer_file_sdrf";
	public static final String FIELD_AER_FILE_FGEM = "aer_file_fgem";
	public static final String FIELD_AER_FILE_BIOSAMPLEPNG = "aer_file_biosamplepng";
	public static final String FIELD_AER_FILE_BIOSAMPLESVG = "aer_file_biosamplesvg";
	public static final String FIELD_AER_FILE_RAW = "aer_file_raw";
	public static final String FIELD_AER_FILE_TWOCOLUMNS = "aer_file_twocolumns";
	public static final String FIELD_AER_FGEM_COUNT="aer_fgem_count";
	public static final String FIELD_AER_RAW_CELCOUNT="aer_raw_celcount";
	public static final String FIELD_AER_RAW_COUNT="aer_raw_count";

	//DW
	public static final String FIELD_DWEXP_ID ="dwe_id";
	public static final String FIELD_DWEXP_ACCESSION = "dwe_txt_accession";
	public static final String FIELD_DWEXP_EXPDESC= "dwe_txt_expdescription";
	public static final String FIELD_DWEXP_EXPTYPE = "dwe_txt_exptype";

	//INDEX FIELDS
	/** Constant that represents the index fields for DW*/
	//public static final String FIELD_AEEXP_ACCESSION="aeexp_accession";
	
	
	public static final String FIELD_XML_DOC_AER = "xml_doc_aer";
	public static final String FIELD_EXP_IN_DW = "exp_in_dw";
	public static final String PREFIX_DWE="dwe_";
	public static final String SUFFIX_ASSAY_ID = "assay_id";
	public static final String SUFFIX_SAMPLE_ID = "sample_id";

    public static final String FIELD_FACTOR_PREFIX = "dwe_ba_";
	
}
