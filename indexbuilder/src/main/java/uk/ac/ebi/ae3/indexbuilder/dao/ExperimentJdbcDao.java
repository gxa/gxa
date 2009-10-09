package uk.ac.ebi.ae3.indexbuilder.dao;

import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import uk.ac.ebi.ae3.indexbuilder.model.Experiment;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
/**
 * The class gets data from AE database.
 *  
 * @author Miroslaw Dylag
 *
 */
@Deprecated
public class ExperimentJdbcDao
{
	/** The instance of Spring JDBC template */
	private JdbcTemplate jdbcTemplate;
	/** The SQL statement which returns a indicate experiment*/
	private static final String sqlExperimentsByAccession = "select distinct e.id, i.identifier as accession, case when v.user_id = 1 then 1 else 0 end as \"public\" " +
	 "from tt_experiment e left outer join tt_identifiable i on i.id = e.id " +
	 "left outer join tt_extendable ext on ext.id = e.id " +
	 "left outer join pl_visibility v on v.label_id = ext.label_id where i.identifier = ?" +
	 "order by i.identifier asc";
	
	/** The SQL statement which returns all experiments**/
	private static final String sqlExperiments = "select distinct e.id, i.identifier as accession, case when v.user_id = 1 then 1 else 0 end as \"public\" " +
									 "from tt_experiment e left outer join tt_identifiable i on i.id = e.id " +
									 "left outer join tt_extendable ext on ext.id = e.id " +
									 "left outer join pl_visibility v on v.label_id = ext.label_id " +
									 "order by i.identifier asc";
	
	/** The SQL statement which returns all data as the XML document for specified experiment **/	
	
    private static String sqlExperimentXml="select XmlElement( \"experiment\"" +
	        " , XmlAttributes( e.id as \"id\", i.identifier as \"accession\", nvt_name.value as \"name\", nvt_releasedate.value as \"releasedate\", nvt_miamegold.value as \"miamegold\" )" +
	        " , ( select XmlAgg( XmlElement( \"user\", v.user_id ) ) from tt_extendable ext left outer join pl_visibility v on v.label_id = ext.label_id where ext.id = e.id )" +
	        " , ( select XmlAgg( XmlElement( \"secondaryaccession\", sa.value ) ) from tt_namevaluetype sa where sa.t_extendable_id = e.id and sa.name = 'SecondaryAccession' )" +
	        " , ( select XmlAgg( XmlElement( \"sampleattribute\", XmlAttributes( i4samattr.category as \"category\", i4samattr.value as \"value\") ) ) from ( select  /*+ LEADING(b) INDEX(o) INDEX(c) INDEX(b)*/ distinct b.experiments_id as id, o.category, o.value from tt_ontologyentry o, tt_characteris_t_biomateri c, tt_biomaterials_experiments b where b.biomaterials_id = c.t_biomaterial_id and c.characteristics_id = o.id ) i4samattr where i4samattr.id = e.id group by i4samattr.id )" +
	        " , ( select XmlAgg( XmlElement( \"experimentalfactor\", XmlAttributes( i4efvs.name as \"name\", i4efvs.value as \"value\") ) ) from (select /*+ leading(d) index(d) index(doe) index(tl) index(f) index(fi) index(fv) index(voe) index(m) */ distinct d.t_experiment_id as id, fi.name as name, ( case when voe.value is not null then voe.value else m.value end ) as value from tt_experimentdesign d, tt_ontologyentry doe, tt_types_t_experimentdesign tl, tt_experimentalfactor f, tt_identifiable fi, tt_factorvalue fv, tt_ontologyentry voe, tt_measurement m where doe.id = tl.types_id and tl.t_experimentdesign_id = d.id and f.t_experimentdesign_id (+) = d.id and fv.experimentalfactor_id (+) = f.id and voe.id (+) = fv.value_id and fi.id (+) = f.id and m.id (+) = fv.measurement_id) i4efvs where i4efvs.id = e.id group by i4efvs.id )" +
	        " , ( select XmlElement( \"miamescore\", XmlAgg( XmlElement( \"score\", XmlAttributes( nvt_miamescores.name as \"name\", nvt_miamescores.value as \"value\" ) ) ) ) from tt_namevaluetype nvt_miamescores, tt_namevaluetype nvt_miame where nvt_miame.id = nvt_miamescores.t_namevaluetype_id and nvt_miame.t_extendable_id = e.id and nvt_miame.name = 'AEMIAMESCORE' group by nvt_miame.value )" +
	        " , ( select /*+ index(pba) */ XmlAgg( XmlElement( \"arraydesign\", XmlAttributes( a.arraydesign_id as \"id\", i4array.identifier as \"accession\", nvt_array.value as \"name\" , count(a.arraydesign_id) as \"count\" ) ) ) from tt_bioassays_t_experiment ea inner join tt_physicalbioassay pba on pba.id = ea.bioassays_id inner join tt_bioassaycreation h on h.id = pba.bioassaycreation_id inner join tt_array a on a.id = h.array_id inner join tt_identifiable i4array on i4array.id = a.arraydesign_id inner join tt_namevaluetype nvt_array on nvt_array.t_extendable_id = a.arraydesign_id and nvt_array.name = 'AEArrayDisplayName' where ea.t_experiment_id = e.id group by a.arraydesign_id, i4array.identifier, nvt_array.value )" +
	        " , ( select /*+ leading(i7) index(bad)*/ XmlAgg( XmlElement( \"bioassaydatagroup\", XmlAttributes( badg.id as \"id\", i8.identifier as \"name\", count(badg.id) as \"bioassaydatacubes\", ( select substr( i10.identifier, 3, 4 ) from tt_arraydesign_bioassaydat abad, tt_identifiable i10 where abad.bioassaydatagroups_id = badg.id and i10.id = abad.arraydesigns_id and rownum = 1 ) as \"arraydesignprovider\", ( select d.dataformat from tt_bioassays_t_bioassayd b, tt_bioassaydata c, tt_biodatacube d, tt_bioassaydat_bioassaydat badbad where b.t_bioassaydimension_id = c.bioassaydimension_id and c.biodatavalues_id = d.id and badbad.bioassaydatas_id = c.id and badbad.bioassaydatagroups_id = badg.id and rownum = 1) as \"dataformat\", ( select count(bbb.bioassays_id) from tt_bioassays_bioassaydat bbb where bbb.bioassaydatagroups_id = badg.id ) as \"bioassays\", ( select count(badg.id) from tt_derivedbioassaydata dbad, tt_bioassaydat_bioassaydat bb where bb.bioassaydatagroups_id = badg.id and dbad.id = bb.bioassaydatas_id and rownum = 1 ) as \"isderived\" ) ) ) from  tt_bioassaydatagroup badg, tt_bioassaydat_bioassaydat bb, tt_bioassaydata bad, tt_identifiable i8 where badg.experiment_id = e.id and bb.bioassaydatagroups_id = badg.id and bad.id = bb.bioassaydatas_id and i8.id = bad.designelementdimension_id group by i8.identifier, badg.id )" +
	        " , ( select XmlAgg( XmlElement( \"bibliography\", XmlAttributes( trim(db.accession) as \"accession\", trim(b.publication) AS \"publication\", trim(b.authors) AS \"authors\", trim(b.title) as \"title\", trim(b.year) as \"year\", trim(b.volume) as \"volume\", trim(b.issue) as \"issue\", trim(b.pages) as \"pages\", trim(b.uri) as \"uri\" ) ) ) from tt_bibliographicreference b, tt_description dd, tt_accessions_t_bibliogra ab, tt_databaseentry db where b.t_description_id = dd.id and dd.t_describable_id = e.id and ab.t_bibliographicreference_id(+) = b.id and db.id (+)= ab.accessions_id )" +
	        " , ( select XmlAgg( XmlElement( \"provider\", XmlAttributes( pp.firstname || ' ' || pp.lastname AS \"contact\", c.email AS \"email\", value AS \"role\" ) ) ) from tt_identifiable ii, tt_ontologyentry o, tt_providers_t_experiment p, tt_roles_t_contact r, tt_person pp, tt_contact c where c.id = r.t_contact_id and ii.id = r.t_contact_id and r.roles_id = o.id and pp.id = ii.id and ii.id = p.providers_id and p.t_experiment_id = e.id )" +
	        " , ( select XmlAgg( XmlElement( \"experimentdesign\", expdesign ) ) from ( select  /*+ index(ed) */ distinct ed.t_experiment_id as id, translate(replace(oe.value,'_design',''),'_',' ') as expdesign from tt_experimentdesign ed, tt_types_t_experimentdesign tte, tt_ontologyentry oe where tte.t_experimentdesign_id = ed.id and oe.id = tte.types_id and oe.category = 'ExperimentDesignType' ) t where t.id = e.id )" +
	        " , XmlAgg( XmlElement( \"description\", XmlAttributes( d.id as \"id\" ), d.text ) ) " +
	        " ).getClobVal() as xml" +
	        " from tt_experiment e" +
	        "  left outer join tt_description d on d.t_describable_id = e.id" +
	        "  left outer join tt_identifiable i on i.id = e.id" +
	        "  left outer join tt_namevaluetype nvt_releasedate on ( nvt_releasedate.t_extendable_id = e.id and nvt_releasedate.name = 'ArrayExpressLoadDate' )" +
	        "  left outer join tt_namevaluetype nvt_name on ( nvt_name.t_extendable_id = e.id and nvt_name.name = 'AEExperimentDisplayName' )" +
	        "  left outer join tt_namevaluetype nvt_miamegold on ( nvt_miamegold.t_extendable_id=e.id and nvt_miamegold.name='AEMIAMEGOLD' )" +
	        " where" +
	        "  e.id = ?" +
	        " or i.identifier = ?"+
	        " group by" +
	        "  e.id" +
	        "  , i.identifier" +
	        "  , nvt_name.value" +
	        "  , nvt_releasedate.value" +
	        "  , nvt_miamegold.value"; 
	/** An instance of JDBC RowMapper for sqlExperiments **/
	private RowMapper rowMapperExperiments = new RowMapperExperiments();
	/** An instance of JDBC RowMapper for sqlExperimentXml**/
	private RowMapper rowMapper = new RowMapperExperimentXml();

	
	/**
	 * Set DataSource instance.
	 * @param dataSource
	 */
	public void setDataSource(DataSource dataSource)
	{
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}
	/**
	 * Gets data for one experiment
	 * @param experiment
	 * @return
	 */
	public String getExperimentAsXml(Experiment experiment)
	{
		
		String xml = (String)this.jdbcTemplate.queryForObject(sqlExperimentXml, new Object[] {experiment.getId(),experiment.getAccession()},rowMapper);
		return xml;
	}
	

	/**
	 * Return all experiments or specific experiment
	 * @return Collection of Experiments
	 */
	public Collection<Experiment> getExperiments(String expAccession)
	{
	    if (StringUtils.isEmpty(expAccession))
	    {
		Collection<Experiment> colection=this.jdbcTemplate.query(sqlExperiments, rowMapperExperiments);
		return colection;
	    }
	    else
	    {
		Collection<Experiment> colection=this.jdbcTemplate.query(sqlExperimentsByAccession, new Object[] {expAccession},rowMapperExperiments);
		return colection;
		
	    }
		
	}
	
	
	/**
	 * Inner class
	 * TODO: Document ME
	 * @author Miroslaw Dylag
	 *
	 */
	class RowMapperExperiments implements ParameterizedRowMapper<Experiment>
	{
		/**
		 * Maps result set of SQL whci is in sqlExperiments to the Experiment object.
		 */		
		public Experiment mapRow(ResultSet rst, int arg1) throws SQLException
		{
			Experiment exp = new Experiment();
			exp.setId(rst.getLong(1));
			exp.setAccession(rst.getString(2));
			exp.setPub(rst.getBoolean(3));
			return exp;
		}
		
	}
	
	
}
