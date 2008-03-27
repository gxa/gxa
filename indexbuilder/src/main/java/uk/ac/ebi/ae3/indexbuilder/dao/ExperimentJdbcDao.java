package uk.ac.ebi.ae3.indexbuilder.dao;

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import javax.sql.DataSource;

import org.dom4j.DocumentException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import uk.ac.ebi.ae3.indexbuilder.model.Experiment;
import uk.ac.ebi.ae3.indexbuilder.utils.XmlUtil;
/**
 * 
 * @author mdylag
 *
 */
public class ExperimentJdbcDao
{
	/**  */
	private JdbcTemplate jdbcTemplate;
	private static final String sqlExperiments = "select distinct e.id, i.identifier as accession, case when v.user_id = 1 then 1 else 0 end as \"public\" " +
									 "from tt_experiment e left outer join tt_identifiable i on i.id = e.id " +
									 "left outer join tt_extendable ext on ext.id = e.id " +
									 "left outer join pl_visibility v on v.label_id = ext.label_id " +
									 "order by i.identifier asc";
	
	private static String sqlExperimentXml = "select XmlElement( \"experiment\"" +
    " , XmlAttributes( i.identifier as \"accnum\", e.id as \"id\", nvt_name.value as \"name\", nvt_releasedate.value as \"releasedate\", nvt_miamegold.value as \"miamegold\" )" +
    " , ( select XmlElement( \"users\", XmlAgg( XmlElement( \"user\", XmlAttributes( v.user_id as \"id\" ) ) ) ) from tt_extendable ext left outer join pl_visibility v on v.label_id = ext.label_id where ext.id = e.id )" +
    " , ( select XmlElement( \"secondaryaccessions\", XmlAgg( XmlElement(\"secondaryaccession\", sa.value ) ) ) from tt_namevaluetype sa where sa.t_extendable_id = e.id and sa.name = 'SecondaryAccession' )" +
    " , ( select XmlElement(\"sampleattributes\", XmlAgg( XmlElement( \"sampleattribute\", XmlAttributes( i4samattr.category, i4samattr.value )))) from ( select  /*+ LEADING(b) INDEX(o) INDEX(c) INDEX(b)*/ distinct b.experiments_id as id, o.category, o.value from tt_ontologyentry o, tt_characteris_t_biomateri c, tt_biomaterials_experiments b where b.biomaterials_id = c.t_biomaterial_id and c.characteristics_id = o.id ) i4samattr where i4samattr.id = e.id group by i4samattr.id )" +
    " , ( select XmlElement( \"factorvalues\", XmlAgg( XmlElement( \"factorvalue\", XmlAttributes( i4efvs.factorname, i4efvs.fv_oe, i4efvs.fv_measurement )))) from (select /*+ leading(d) index(d) index(doe) index(tl) index(f) index(fi) index(fv) index(voe) index(m) */ distinct d.t_experiment_id as id, fi.name as factorName, voe.value as FV_OE, m.value as FV_MEASUREMENT from tt_experimentdesign d, tt_ontologyentry doe, tt_types_t_experimentdesign tl, tt_experimentalfactor f, tt_identifiable fi, tt_factorvalue fv, tt_ontologyentry voe, tt_measurement m where doe.id = tl.types_id and tl.t_experimentdesign_id = d.id and f.t_experimentdesign_id (+) = d.id and fv.experimentalfactor_id (+) = f.id and voe.id (+) = fv.value_id and fi.id (+) = f.id and m.id (+) = fv.measurement_id) i4efvs where i4efvs.id = e.id group by i4efvs.id )                , ( select XmlElement( \"miamescores\", XmlAttributes( nvt_miame.value as \"miamescore\" ), XmlAgg( XmlElement( \"miamescore\", XmlAttributes( nvt_miamescores.name as \"name\", nvt_miamescores.value as \"value\" ) ) ) ) from tt_namevaluetype nvt_miamescores, tt_namevaluetype nvt_miame, tt_identifiable i4miame where nvt_miame.id=nvt_miamescores.t_namevaluetype_id and nvt_miame.t_extendable_id=i4miame.id and i4miame.identifier=i.identifier and nvt_miame.name='AEMIAMESCORE' group by nvt_miame.t_extendable_id, nvt_miame.value, i4miame.identifier )" +
    " , ( select XmlElement( \"miamescores\", XmlAttributes( nvt_miame.value as \"miamescore\" ), XmlAgg( XmlElement( \"miamescore\", XmlAttributes( nvt_miamescores.name as \"name\", nvt_miamescores.value as \"value\" ) ) ) ) from tt_namevaluetype nvt_miamescores, tt_namevaluetype nvt_miame, tt_identifiable i4miame where nvt_miame.id=nvt_miamescores.t_namevaluetype_id and nvt_miame.t_extendable_id=i4miame.id and i4miame.identifier=i.identifier and nvt_miame.name='AEMIAMESCORE' group by nvt_miame.t_extendable_id, nvt_miame.value, i4miame.identifier)" +
    " , ( select /*+ index(pba) */ XmlElement( \"arraydesigns\", XmlAgg( XmlElement( \"arraydesign\", XmlAttributes( a.arraydesign_id as \"id\", i4array.identifier as \"identifier\", nvt_array.value as \"name\" , count(a.arraydesign_id) as \"count\" ) ) ) ) from tt_bioassays_t_experiment ea inner join tt_physicalbioassay pba on pba.id = ea.bioassays_id inner join tt_bioassaycreation h on h.id = pba.bioassaycreation_id inner join tt_array a on a.id = h.array_id inner join tt_identifiable i4array on i4array.id = a.arraydesign_id inner join tt_namevaluetype nvt_array on nvt_array.t_extendable_id = a.arraydesign_id and nvt_array.name = 'AEArrayDisplayName' where ea.t_experiment_id = e.id group by a.arraydesign_id, i4array.identifier, nvt_array.value )" +
    " , ( select /*+ leading(i7) index(bad)*/ XmlElement( \"bioassaydatagroups\", XmlAgg( XmlElement( \"bioassaydatagroup\", XmlAttributes( i8.identifier as \"name\", badg.id as \"id\", count(badg.id) as \"num_bad_cubes\", ( select substr( i10.identifier, 3, 4 ) as \"arraydesign\" from tt_arraydesign_bioassaydat abad, tt_identifiable i10 where abad.bioassaydatagroups_id=badg.id and i10.id=abad.arraydesigns_id and rownum = 1 ) as \"arraydesign\", ( select d.dataformat from tt_bioassays_t_bioassayd b, tt_bioassaydata c, tt_biodatacube d, tt_bioassaydat_bioassaydat badbad where b.t_bioassaydimension_id = c.bioassaydimension_id and c.biodatavalues_id = d.id and badbad.bioassaydatas_id = c.id and badbad.bioassaydatagroups_id = badg.id and rownum = 1) as \"dataformat\", ( select count(bbb.bioassays_id) from tt_bioassays_bioassaydat bbb where bbb.bioassaydatagroups_id = badg.id ) as \"bioassay_count\", ( select count(badg.id) from tt_derivedbioassaydata dbad, tt_bioassaydat_bioassaydat bb where bb.bioassaydatagroups_id = badg.id and dbad.id = bb.bioassaydatas_id and rownum = 1) as \"is_derived\" ), ))) from  tt_bioassaydatagroup badg, tt_bioassaydat_bioassaydat bb, tt_bioassaydata bad, tt_identifiable i8 where badg.experiment_id = e.id and bb.bioassaydatagroups_id = badg.id and bad.id = bb.bioassaydatas_id and i8.id = bad.designelementdimension_id group by i8.identifier, badg.id )" +
    " , ( select XmlElement( \"bibliography\", XmlAttributes( trim(db.ACCESSION) as \"accession\", trim(b.publication) AS \"publication\", trim(b.authors) AS \"authors\", trim(b.title) AS \"title\", trim(b.year) AS \"year\", trim(b.volume) AS \"volume\", trim(b.issue) AS \"issue\", trim(b.pages) AS \"pages\", trim(b.uri) AS \"uri\" ) ) FROM tt_bibliographicreference b, tt_description dd, tt_accessions_t_bibliogra ab, tt_databaseentry db WHERE b.t_description_id=dd.id AND dd.t_describable_id=e.id AND ab.T_BIBLIOGRAPHICREFERENCE_ID(+)=b.id AND db.id (+)= ab.ACCESSIONS_ID and rownum=1)" +
    " , ( select distinct XmlElement ( \"providers\", XmlAgg ( XmlElement ( \"provider\", XmlAttributes ( pp.firstname || ' ' || pp.lastname AS \"contact\", c.email AS \"email\", value AS \"role\" ) ) ) ) FROM tt_identifiable ii, tt_ontologyentry o, tt_providers_t_experiment p, tt_roles_t_contact r, tt_person pp, tt_contact c WHERE c.id = r.t_contact_id AND ii.id = r.T_CONTACT_ID AND r.ROLES_ID = o.ID AND pp.id = ii.id AND ii.id = p.PROVIDERS_ID AND p.T_EXPERIMENT_ID = e.id )" +
    " , ( select /*+ index(ed) */ distinct XmlElement ( \"experimentdesigns\", XmlAgg( XmlElement ( \"experimentdesign\", XmlAttributes ( translate(replace(oe.value,'_design',''),'_',' ') as \"type\" ) ) ) ) FROM tt_experimentdesign ed, tt_types_t_experimentdesign tte, tt_ontologyentry oe WHERE ed.t_experiment_id = e.id AND tte.t_experimentdesign_id = ed.id AND oe.id = tte.types_id AND oe.CATEGORY = 'ExperimentDesignType' )" +
    " , XmlAgg( XmlElement( \"description\", XmlAttributes( d.id AS \"id\" ), d.text ) ) " +
    " ).getClobVal() as xml" +
    " from tt_experiment e" +
    "  left outer join tt_description d on d.t_describable_id = e.id" +
    "  left outer join tt_identifiable i on i.id = e.id" +
    "  left outer join tt_namevaluetype nvt_releasedate on ( nvt_releasedate.t_extendable_id = e.id and nvt_releasedate.name = 'ArrayExpressLoadDate' )" +
    "  left outer join tt_namevaluetype nvt_name on ( nvt_name.t_extendable_id = e.id and nvt_name.name = 'AEExperimentDisplayName' )" +
    "  left outer join tt_namevaluetype nvt_miamegold on ( nvt_miamegold.t_extendable_id=e.id and nvt_miamegold.name='AEMIAMEGOLD' )" +
    " where" +
    "  e.id = ?" +
    " group by" +
    "  e.id" +
    "  , i.identifier" +
    "  , nvt_name.value" +
    "  , nvt_releasedate.value" +
    "  , nvt_miamegold.value";
	
	private RowMapper rowMapperExperiments = new RowMapperExperiments();
	private RowMapper rowMapper = new RowMapperExperimentXml(); 
	public void setDataSource(DataSource dataSource)
	{
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}
	/**
	 * 
	 * @param id
	 * @return
	 */	
	public Experiment getExperiment(Experiment experiment) throws DocumentException
	{
		String xml = getExperimentAsXml(experiment);
		XmlUtil.createExperiment(xml,experiment);
		System.out.println(xml);
		System.out.println(experiment.getName());
		return experiment;
	}
	/**
	 * 
	 * @param experiment
	 * @return
	 */
	public String getExperimentAsXml(Experiment experiment)
	{
		String xml = (String)this.jdbcTemplate.queryForObject(sqlExperimentXml, new Object[] {experiment.getId()},rowMapper);
		return xml;
	}
	

	/**
	 * 
	 * @return
	 */
	public Collection<Experiment> getExperiments()
	{
		Collection<Experiment> colection=this.jdbcTemplate.query(sqlExperiments, rowMapperExperiments);
		return colection;
		
	}
	
	/**
	 * 
	 * @author mdylag
	 *
	 */
	class RowMapperExperiments implements ParameterizedRowMapper<Experiment>
	{
		public Experiment mapRow(ResultSet arg0, int arg1) throws SQLException
		{
			Experiment exp = new Experiment();
			exp.setId(arg0.getLong(1));
			exp.setAccession(arg0.getString(2));
			exp.setPub(arg0.getBoolean(3));
			return exp;
		}
		
	}
	/**
	 * 
	 * @author mdylag
	 *
	 */
	class RowMapperExperimentXml implements ParameterizedRowMapper<String>
	{
		public String mapRow(ResultSet arg0, int arg1) throws SQLException
		{
			Experiment exp = new Experiment();
			Clob clob=arg0.getClob(1);
			String str=clob.getSubString(1,(int)clob.length());
			return str;
		}
		
	}
	 
	/* */
}
