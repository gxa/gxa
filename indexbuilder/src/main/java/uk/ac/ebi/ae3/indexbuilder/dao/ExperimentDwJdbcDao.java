package uk.ac.ebi.ae3.indexbuilder.dao;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import uk.ac.ebi.ae3.indexbuilder.model.Experiment;

/**
 * The class prepares the sql statement and gets data for experiments from DW database.
 * The class uses 
 * 
 * @author mdylag
 * @version 1.0
 * 
 *
 */
public class ExperimentDwJdbcDao
{
	/** The instance of JdbcTemplate **/
	private JdbcTemplate jdbcTemplate;

	/** The SQL string which returns xml for one experiment **/
	private static final String SQL_ASXML = "  SELECT XmlElement(\"experiment\",XmlAttributes( experiment.experiment_id_key, experiment.experiment_identifier, experiment.experiment_description )," +
			" (SELECT XmlAgg ( XmlForest ( experiment_type.value as \"type\") ) FROM ae1__experiment_type__dm experiment_type WHERE experiment.experiment_id_key=experiment_type.experiment_id_key)," +
			" (xmlelement(\"assay_attributes\",(SELECT distinct XmlAgg(XmlForest ( ba_age.value as \"ba_age\" )) FROM ae1__assay_age__dm ba_age WHERE experiment.experiment_id_key=ba_age.experiment_id_key)," +
			" (SELECT distinct XmlAgg(XmlForest ( ba_biometric.value as \"ba_biometric\" )) FROM ae1__assay_biometric__dm ba_biometric WHERE experiment.experiment_id_key=ba_biometric.experiment_id_key)," +
			" (SELECT distinct XmlAgg(XmlForest ( ba_cellline.value as \"ba_cellline\" )) FROM ae1__assay_cellline__dm ba_cellline WHERE experiment.experiment_id_key=ba_cellline.experiment_id_key)," +
			" (SELECT distinct XmlAgg(XmlForest ( ba_celltype.value as \"ba_celltype\" )) FROM ae1__assay_celltype__dm ba_celltype WHERE experiment.experiment_id_key=ba_celltype.experiment_id_key)," +
			" (SELECT distinct XmlAgg(XmlForest ( ba_clinhistory.value as \"ba_clinhistory\" )) FROM ae1__assay_clinhistory__dm ba_clinhistory WHERE experiment.experiment_id_key=ba_clinhistory.experiment_id_key)," +
			" (SELECT distinct XmlAgg(XmlForest ( ba_clininfo.value as \"ba_clininfo\" )) FROM ae1__assay_clininfo__dm ba_clininfo WHERE experiment.experiment_id_key=ba_clininfo.experiment_id_key)," +
			" (SELECT distinct XmlAgg(XmlForest ( ba_clintreatment.value as \"ba_clintreatment\" )) FROM ae1__assay_clintreatment__dm ba_clintreatment WHERE experiment.experiment_id_key=ba_clintreatment.experiment_id_key)," +
			" (SELECT distinct XmlAgg(XmlForest ( ba_compound.value as \"ba_compound\" )) FROM ae1__assay_compound__dm ba_compound WHERE experiment.experiment_id_key=ba_compound.experiment_id_key)," +
			" (SELECT distinct XmlAgg(XmlForest ( ba_cultivar.value as \"ba_cultivar\" )) FROM ae1__assay_cultivar__dm ba_cultivar WHERE experiment.experiment_id_key=ba_cultivar.experiment_id_key)," +
			" (SELECT distinct XmlAgg(XmlForest ( ba_devstage.value as \"ba_devstage\" )) FROM ae1__assay_devstage__dm ba_devstage WHERE experiment.experiment_id_key=ba_devstage.experiment_id_key)," +
			" (SELECT distinct XmlAgg(XmlForest ( ba_diseaseloc.value as \"ba_diseaseloc\" )) FROM ae1__assay_diseaseloc__dm ba_diseaseloc WHERE experiment.experiment_id_key=ba_diseaseloc.experiment_id_key)," +
			" (SELECT distinct XmlAgg(XmlForest ( ba_diseasestaging.value as \"ba_diseasestaging\" )) FROM ae1__assay_diseasestaging__dm ba_diseasestaging WHERE experiment.experiment_id_key=ba_diseasestaging.experiment_id_key)," +
			" (SELECT distinct XmlAgg(XmlForest ( ba_diseasestate.value as \"ba_diseasestate\" )) FROM ae1__assay_diseasestate__dm ba_diseasestate WHERE experiment.experiment_id_key=ba_diseasestate.experiment_id_key)," +
			" (SELECT distinct XmlAgg(XmlForest ( ba_dose.value as \"ba_dose\" )) FROM ae1__assay_dose__dm ba_dose WHERE experiment.experiment_id_key=ba_dose.experiment_id_key)," +
			" (SELECT distinct XmlAgg(XmlForest ( ba_ecotype.value as \"ba_ecotype\" )) FROM ae1__assay_ecotype__dm ba_ecotype WHERE experiment.experiment_id_key=ba_ecotype.experiment_id_key)," +
			" (SELECT distinct XmlAgg(XmlForest ( ba_envhistory.value as \"ba_envhistory\" )) FROM ae1__assay_envhistory__dm ba_envhistory WHERE experiment.experiment_id_key=ba_envhistory.experiment_id_key)," +
			" (SELECT distinct XmlAgg(XmlForest ( ba_familyhistory.value as \"ba_familyhistory\" )) FROM ae1__assay_familyhistory__dm ba_familyhistory WHERE experiment.experiment_id_key=ba_familyhistory.experiment_id_key)," +
			" (SELECT distinct XmlAgg(XmlForest ( ba_genmodif.value as \"ba_genmodif\" )) FROM ae1__assay_genmodif__dm ba_genmodif WHERE experiment.experiment_id_key=ba_genmodif.experiment_id_key)," +
			" (SELECT distinct XmlAgg(XmlForest ( ba_genotype.value as \"ba_genotype\" )) FROM ae1__assay_genotype__dm ba_genotype WHERE experiment.experiment_id_key=ba_genotype.experiment_id_key)," +
			" (SELECT distinct XmlAgg(XmlForest ( ba_histology.value as \"ba_histology\" )) FROM ae1__assay_histology__dm ba_histology WHERE experiment.experiment_id_key=ba_histology.experiment_id_key)," +
			" (SELECT distinct XmlAgg(XmlForest ( ba_indgeneticchar.value as \"ba_indgeneticchar\" )) FROM ae1__assay_indgeneticchar__dm ba_indgeneticchar WHERE experiment.experiment_id_key=ba_indgeneticchar.experiment_id_key)," +
			" (SELECT distinct XmlAgg(XmlForest ( ba_individual.value as \"ba_individual\" )) FROM ae1__assay_individual__dm ba_individual WHERE experiment.experiment_id_key=ba_individual.experiment_id_key)," +
			" (SELECT distinct XmlAgg(XmlForest ( ba_light.value as \"ba_light\" )) FROM ae1__assay_light__dm ba_light WHERE experiment.experiment_id_key=ba_light.experiment_id_key)," +
			" (SELECT distinct XmlAgg(XmlForest ( ba_media.value as \"ba_media\" )) FROM ae1__assay_media__dm ba_media WHERE experiment.experiment_id_key=ba_media.experiment_id_key)," +
    		" (SELECT distinct XmlAgg(XmlForest ( ba_observation.value as \"ba_observation\" )) FROM ae1__assay_observation__dm ba_observation WHERE experiment.experiment_id_key=ba_observation.experiment_id_key)," +
    		" (SELECT distinct XmlAgg(XmlForest ( ba_organism.value as \"ba_organism\" )) FROM ae1__assay_organism__dm ba_organism WHERE experiment.experiment_id_key=ba_organism.experiment_id_key)," +
    		" (SELECT distinct XmlAgg(XmlForest ( ba_organismpart.value as \"ba_organismpart\" )) FROM ae1__assay_organismpart__dm ba_organismpart WHERE experiment.experiment_id_key=ba_organismpart.experiment_id_key)," +
    		" (SELECT distinct XmlAgg(XmlForest ( ba_organismstatus.value as \"ba_organismstatus\" )) FROM ae1__assay_organismstatus__dm ba_organismstatus WHERE experiment.experiment_id_key=ba_organismstatus.experiment_id_key)," +
    		" (SELECT distinct XmlAgg(XmlForest ( ba_protocoltype.value as \"ba_protocoltype\" )) FROM ae1__assay_protocoltype__dm ba_protocoltype WHERE experiment.experiment_id_key=ba_protocoltype.experiment_id_key)," +
    		" (SELECT distinct XmlAgg(XmlForest ( ba_performer.value as \"ba_performer\" )) FROM ae1__assay_performer__dm ba_performer WHERE experiment.experiment_id_key=ba_performer.experiment_id_key)," +
    		" (SELECT distinct XmlAgg(XmlForest ( ba_phenotype.value as \"ba_phenotype\" )) FROM ae1__assay_phenotype__dm ba_phenotype WHERE experiment.experiment_id_key=ba_phenotype.experiment_id_key)," +
    		" (SELECT distinct XmlAgg(XmlForest ( ba_qcdescrtype.value as \"ba_qcdescrtype\" )) FROM ae1__assay_qcdescrtype__dm ba_qcdescrtype WHERE experiment.experiment_id_key=ba_qcdescrtype.experiment_id_key)," +
    		" (SELECT distinct XmlAgg(XmlForest ( ba_sex.value as \"ba_sex\" )) FROM ae1__assay_sex__dm ba_sex WHERE experiment.experiment_id_key=ba_sex.experiment_id_key)," +
    		" (SELECT distinct XmlAgg(XmlForest ( ba_strainorline.value as \"ba_strainorline\" )) FROM ae1__assay_strainorline__dm ba_strainorline WHERE experiment.experiment_id_key=ba_strainorline.experiment_id_key)," +
    		" (SELECT distinct XmlAgg(XmlForest ( ba_targetcelltype.value as \"ba_targetcelltype\" )) FROM ae1__assay_targetcelltype__dm ba_targetcelltype WHERE experiment.experiment_id_key=ba_targetcelltype.experiment_id_key)," +
    		" (SELECT distinct XmlAgg(XmlForest ( ba_temperature.value as \"ba_temperature\" )) FROM ae1__assay_temperature__dm ba_temperature WHERE experiment.experiment_id_key=ba_temperature.experiment_id_key)," +
    		" (SELECT distinct XmlAgg(XmlForest ( ba_testtype.value as \"ba_testtype\" )) FROM ae1__assay_testtype__dm ba_testtype WHERE experiment.experiment_id_key=ba_testtype.experiment_id_key)," +
    		" (SELECT distinct XmlAgg(XmlForest ( ba_testresult.value as \"ba_testresult\" )) FROM ae1__assay_testresult__dm ba_testresult WHERE experiment.experiment_id_key=ba_testresult.experiment_id_key)," +
    		" (SELECT distinct XmlAgg(XmlForest ( ba_test.value as \"ba_test\" )) FROM ae1__assay_test__dm ba_test WHERE experiment.experiment_id_key=ba_test.experiment_id_key)," +
    		" (SELECT distinct XmlAgg(XmlForest ( ba_time.value as \"ba_time\" )) FROM ae1__assay_time__dm ba_time WHERE experiment.experiment_id_key=ba_time.experiment_id_key)," +
    		" (SELECT distinct XmlAgg(XmlForest ( ba_tumorgrading.value as \"ba_tumorgrading\" )) FROM ae1__assay_tumorgrading__dm ba_tumorgrading WHERE experiment.experiment_id_key=ba_tumorgrading.experiment_id_key)," +
    		" (SELECT distinct XmlAgg(XmlForest ( ba_vehicle.value as \"ba_vehicle\" )) FROM ae1__assay_vehicle__dm ba_vehicle WHERE experiment.experiment_id_key=ba_vehicle.experiment_id_key)))," +
    		" (XmlElement(\"sample_attributes\",(SELECT distinct XmlAgg ( XmlForest ( sample_all.value as \"bs_unknown\" ) ) FROM ae1__sample_all__dm sample_all WHERE sample_all.experiment_id_key=experiment.experiment_id_key" +
    		")))).getClobVal() as xml FROM ae1__experiment__main experiment WHERE experiment.experiment_accession=?";

	/** An instance of JDBC RowMapper for SQL_ASXML**/
	private RowMapper rowMapper = new RowMapperExperimentXml();

	/**
	 * Set the default DataSource to be used by the ExperimentDwJdbcDao.
	 * @param dataSource -an instance of DataSource class
	 */
	public void setDataSource(DataSource dataSource)
	{
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}
	
	/**
	 * 
	 * @param identifier
	 * @return
	 */
	public String getExperimentAsXml(Experiment experiment)
	{
		//String xml = (String)this.jdbcTemplate.queryForObject(SQL_ASXML, new Object[] {experiment.getAccession()},rowMapper);
		List<String> l= this.jdbcTemplate.query(SQL_ASXML, new Object[] {experiment.getAccession()},rowMapper);
		String xml = null;
		if (l.size() != 0)
			xml = l.get(0);
		//String xml = (String)this.jdbcTemplate.queryForObject(SQL_ASXML, new Object[] {experiment.getAccession()},rowMapper);
		
		return xml;
	}
}
