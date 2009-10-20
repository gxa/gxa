package uk.ac.ebi.microarray.atlas.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import uk.ac.ebi.microarray.atlas.model.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * A data access object designed for retrieving common sorts of data from the
 * atlas database.  This DAO should be configured with a spring {@link
 * JdbcTemplate} object which will be used to query the database.
 *
 * @author Tony Burdett
 * @date 21-Sep-2009
 */
public class AtlasDAO {
  // experiment queries
  private static final String EXPERIMENTS_SELECT =
      "SELECT accession, description, performer, lab, experimentid " +
          "FROM a2_experiment";
  private static final String EXPERIMENTS_PENDING_INDEX_SELECT =
      EXPERIMENTS_SELECT + " " +
          "WHERE something something"; // fixme: load monitor table?
  private static final String EXPERIMENTS_PENDING_NETCDF_SELECT =
      EXPERIMENTS_SELECT + " " +
          "WHERE something something"; // fixme: load monitor table?
  private static final String EXPERIMENT_BY_ACC_SELECT =
      EXPERIMENTS_SELECT + " " +
          "WHERE accession=?";

  // gene queries
  private static final String GENES_SELECT =
      "SELECT g.geneid, g.identifier, g.name, s.name AS species " +
          "FROM a2_gene g, a2_spec s " +
          "WHERE g.specid = s.specid";
  private static final String GENES_PENDING_SELECT =
      GENES_SELECT + " " +
          "AND something something"; // fixme: load monitor table?
  private static final String GENES_BY_EXPERIMENT_ACCESSION =
      "SELECT DISTINCT g.geneid, g.identifier, g.name, " +
          "s.name AS species, d.designelementid " +
          "FROM a2_gene g, a2_spec s, a2_designelement d, a2_assay a, " +
          "a2_experiment e " +
          "WHERE g.geneid=d.geneid " +
          "AND g.specid = s.specid " +
          "AND d.arraydesignid=a.arraydesignid " +
          "AND a.experimentid=e.experimentid " +
          "AND e.accession=?";
  private static final String PROPERTIES_BY_GENEID =
      "SELECT gp.name AS property, gpv.value AS propertyvalue " +
          "FROM a2_geneproperty gp, a2_genepropertyvalue gpv " +
          "WHERE gpv.genepropertyid=gp.genepropertyid " +
          "AND gpv.geneid=?";

  // assay queries
  private static final String ASSAYS_BY_EXPERIMENT_ACCESSION =
      "SELECT a.accession, e.accession, ad.accession, a.assayid " +
          "FROM a2_assay a, a2_experiment e, a2_arraydesign ad " +
          "WHERE e.experimentid=a.experimentid " +
          "AND a.arraydesignid=ad.arraydesignid " +
          "AND e.accession=?";
  private static final String PROPERTIES_BY_ASSAY_ACCESSION =
      "SELECT p.name AS property, p.accession, pv.name AS propertyvalue, " +
          "apv.isfactorvalue " +
          "FROM a2_property p, a2_propertyvalue pv, " +
          "a2_assaypropertyvalue apv, a2_assay a " +
          "WHERE apv.propertyvalueid=pv.propertyvalueid " +
          "AND pv.propertyid=p.propertyid " +
          "AND apv.assayid=a.assayid " +
          "AND a.accession=?";
  private static final String EXPRESSION_VALUES_BY_ASSAY_ID =
      "SELECT ev.designelementid, de.accession, ev.value " +
          "FROM a2_expressionvalue ev, a2_designelement de " +
          "WHERE ev.designelementid=de.designelementid " +
          "AND ev.assayid=?";

  // sample queries
  private static final String SAMPLES_BY_ASSAY_ACCESSION =
      "SELECT s.accession, s.species, s.channel, s.sampleid " +
          "FROM a2_sample s, a2_assay a, a2_assaysample ass " +
          "WHERE s.sampleid=ass.sampleid " +
          "AND a.assayid=ass.assayid " +
          "AND a.accession=?";
  private static final String PROPERTIES_BY_SAMPLE_ACCESSION =
      "SELECT " +
          "p.name AS property, " +
          "p.accession, " +
          "pv.name AS propertyvalue, " +
          "spv.isfactorvalue " +
          "FROM " +
          "a2_property p, " +
          "a2_propertyvalue pv, " +
          "a2_samplepropertyvalue spv, " +
          "a2_sample s " +
          "WHERE spv.propertyvalueid=pv.propertyvalueid " +
          "AND pv.propertyid=p.propertyid " +
          "AND spv.sampleid=s.sampleid " +
          "AND s.accession=?";

  // array and design element queries
  private static final String ARRAY_DESIGN_SELECT =
      "SELECT accession, type, name, provider, arraydesignid " +
          "FROM a2_arraydesign";
  private static final String ARRAY_DESIGN_BY_ACC_SELECT =
      ARRAY_DESIGN_SELECT + " " +
          "WHERE accession=?";
  private static final String DESIGN_ELEMENT_IDS_BY_ARRAY_ACCESSION =
      "SELECT de.designelementid from A2_ARRAYDESIGN ad, A2_DESIGNELEMENT de " +
          "WHERE de.arraydesignid=ad.arraydesignid " +
          "AND ad.accession=?";
  private static final String DESIGN_ELEMENT_IDS_BY_GENEID =
      "SELECT de.designelementid " +
          "FROM a2_designelement de " +
          "WHERE de.geneid=?";
  private static final String DESIGN_ELEMENT_ACCS_BY_ARRAY_ACCESSION =
      "SELECT de.accession " +
          "FROM a2_arraydesign ad, a2_designelement de " +
          "where de.arraydesignid=ad.arraydesignid " +
          "AND ad.accession=? ";

  // other useful queries
  private static final String ATLAS_COUNTS_BY_EXPERIMENTID =
      "SELECT p.Name AS property, pv.name AS propertyvalue, " +
          "CASE when ea.TSTAT < 0 THEN -1 ELSE 1 END AS UpDn, " +
          "COUNT(DISTINCT(g.geneid)) AS genes " +
          "FROM a2_expressionanalytics ea " +
          "JOIN a2_propertyvalue pv on pv.propertyvalueid=ea.propertyvalueid " +
          "JOIN a2_property p on p.propertyid=pv.propertyid " +
          "JOIN a2_designelement de on de.designelementid=ea.designelementid " +
          "JOIN a2_gene g on g.geneid=de.geneid " +
          "WHERE ea.experimentid=? " +
          "GROUP BY p.name, pv.name, CASE WHEN ea.pvaladj < 0 THEN -1 ELSE 1 END";
  private static final String EXPRESSIONANALYTICS_BY_GENEID =
      "SELECT ef.name AS ef, efv.name AS efv, a.experimentid, a.tstat, a.pvaladj " +
          "FROM a2_expressionanalytics a " +
          "JOIN a2_propertyvalue efv ON efv.propertyvalueid=a.propertyvalueid " +
          "JOIN a2_property ef ON ef.propertyid=efv.propertyid " +
          "JOIN a2_designelement de ON de.designelementid=a.designelementID " +
          "WHERE de.geneid=?";
  private static final String ONTOLOGY_MAPPINGS_BY_ONTOLOGYNAME =
      // fixme - work out the new query, this is for old schema
      "select experiment_id_key||'_'||ef||'_'||efv as mapkey, string_agg(accession) from (SELECT DISTINCT s.experiment_id_key," +
          "     LOWER(SUBSTR(oa.orig_value_src,    instr(oa.orig_value_src,    '_',    1,    3) + 1,    instr(oa.orig_value_src,    '__DM',    1,    1) -instr(oa.orig_value_src,    '_',    1,    3) -1)) ef," +
          "     oa.orig_value AS efv," +
          "     oa.accession" +
          "   FROM ontology_annotation oa," +
          "     ae1__sample__main s" +
          "   WHERE(s.sample_id_key = oa.sample_id_key OR s.assay_id_key = oa.assay_id_key)" +
          "   AND oa.ontology_id_key = 575119145) group by experiment_id_key, ef, efv";

  private JdbcTemplate template;

  public JdbcTemplate getJdbcTemplate() {
    return template;
  }

  public void setJdbcTemplate(JdbcTemplate template) {
    this.template = template;
  }

  public List<Experiment> getAllExperiments() {
    List results = template.query(EXPERIMENTS_SELECT,
                                  new ExperimentMapper());
    return (List<Experiment>) results;
  }

  public List<Experiment> getAllExperimentsPendingIndexing() {
    List results = template.query(EXPERIMENTS_PENDING_INDEX_SELECT,
                                  new ExperimentMapper());
    return (List<Experiment>) results;
  }

  public List<Experiment> getAllExperimentsPendingNetCDFs() {
    List results = template.query(EXPERIMENTS_PENDING_NETCDF_SELECT,
                                  new ExperimentMapper());
    return (List<Experiment>) results;
  }

  /**
   * Gets a single experiment from the Atlas Database, queried by the accession
   * of the experiment.
   *
   * @param accession the experiment's accession number (usually in the format
   *                  E-ABCD-1234)
   * @return an object modelling this experiment
   */
  public Experiment getExperimentByAccession(String accession) {
    List results = template.query(EXPERIMENT_BY_ACC_SELECT,
                                  new Object[]{accession},
                                  new ExperimentMapper());

    return results.size() > 0 ? (Experiment) results.get(0) : null;
  }

  public List<Gene> getAllGenes() {
    List results = template.query(GENES_SELECT,
                                  new GeneMapper());
    return (List<Gene>) results;
  }

  public List<Gene> getAllPendingGenes() {
    List results = template.query(GENES_PENDING_SELECT,
                                  new GeneMapper());
    return (List<Gene>) results;
  }

  public List<Gene> getGenesByExperimentAccession(String exptAccession) {
    List results = template.query(GENES_BY_EXPERIMENT_ACCESSION,
                                  new Object[]{exptAccession},
                                  new GeneMapper());
    return (List<Gene>) results;

  }

  public void getPropertiesForGenes(List<Gene> genes) {
    // also fetch all properties
    for (Gene gene : genes) {
      // fixme: this is inefficient - we'll end up generating lots of queries.  Is it better to handle with a big join?
      List propResults = template.query(PROPERTIES_BY_GENEID,
                                        new Object[]{gene.getGeneID()},
                                        new GenePropertyMapper());
      // and set on gene
      gene.setProperties(propResults);
    }
  }

  public List<Assay> getAssaysByExperimentAccession(
      String experimentAccession) {
    List results = template.query(ASSAYS_BY_EXPERIMENT_ACCESSION,
                                  new Object[]{experimentAccession},
                                  new AssayMapper());

    List<Assay> assays = (List<Assay>) results;

    // also fetch all properties
    for (Assay assay : assays) {
      // fixme: this is inefficient - we'll end up generating lots of queries.  Is it better to handle with a big join?
      List propResults = template.query(PROPERTIES_BY_ASSAY_ACCESSION,
                                        new Object[]{assay.getAccession()},
                                        new PropertyMapper());
      // and set on assay
      assay.setProperties(propResults);
    }

    return assays;
  }

  public void getExpressionValuesForAssays(List<Assay> assays) {
    // fetch all expression values
    for (Assay assay : assays) {
      // fixme: this is inefficient - we'll end up generating lots of queries.  Is it better to handle with a big join?
      List evResults = template.query(EXPRESSION_VALUES_BY_ASSAY_ID,
                                      new Object[]{assay.getAssayID()},
                                      new ExpressionValueMapper());
      // and set on assay
      assay.setExpressionValues((List<ExpressionValue>) evResults);
    }
  }

  public List<Sample> getSamplesByAssayAccession(String assayAccession) {
    List results = template.query(SAMPLES_BY_ASSAY_ACCESSION,
                                  new Object[]{assayAccession},
                                  new SampleMapper());

    // fixme: this doesn't adequately retrieve all assay accessions for samples
    List<Sample> samples = (List<Sample>) results;

    // also fetch all properties
    for (Sample sample : samples) {
      // fixme: hack setting of assay accessions, this is broken as it assumes 1:1
      List<String> assays = new ArrayList<String>();
      assays.add(assayAccession);
      sample.setAssayAccessions(assays);

      // fixme: this is inefficient - we'll end up generating lots of queries.  Is it better to handle with a big join?
      List propResults = template.query(PROPERTIES_BY_SAMPLE_ACCESSION,
                                        new Object[]{sample.getAccession()},
                                        new PropertyMapper());
      // and set on assay
      sample.setProperties(propResults);
    }

    return samples;
  }

  public List<ArrayDesign> getAllArrayDesigns() {
    List results = template.query(ARRAY_DESIGN_SELECT,
                                  new ArrayDesignMapper());

    return (List<ArrayDesign>) results;
  }

  public ArrayDesign getArrayDesignByAccession(String accession) {
    List results = template.query(ARRAY_DESIGN_BY_ACC_SELECT,
                                  new Object[]{accession},
                                  new ArrayDesignMapper());

    return results.size() > 0 ? (ArrayDesign) results.get(0) : null;
  }

  /**
   * A convenience method that fetches the set of design element ids by array
   * design accession.  The set of design element ids contains no duplicates,
   * and the results that are returned are the internal database ids for design
   * elements.  This takes the accession of the array design as a parameter.
   *
   * @param arrayDesignAccession the accession number of the array design to
   *                             query for
   * @return a set of unique design element id integers
   */
  public List<Integer> getDesignElementIDsByArrayAccession(
      String arrayDesignAccession) {
    List results = template.query(DESIGN_ELEMENT_IDS_BY_ARRAY_ACCESSION,
                                  new Object[]{arrayDesignAccession},
                                  new DesignElementMapper());
    return (List<Integer>) results;
  }

  /**
   * A convenience method that fetches the set of design element accessions by
   * array design accession.  The set of design element accessions contains no
   * duplicates, and the results that are returned are the manufacturers
   * accession strings for each design element on an array design. This takes
   * the accession of the array design (which should be of the form
   * A-ABCD-123).
   *
   * @param arrayDesignAccession the accession number of the array design to
   *                             query for
   * @return a set of unique design element accession strings
   */
  public List<String> getDesignElementAccessionsByArrayAccession(
      String arrayDesignAccession) {
    List results = template.query(DESIGN_ELEMENT_ACCS_BY_ARRAY_ACCESSION,
                                  new Object[]{arrayDesignAccession},
                                  new DesignElementAccMapper());
    return (List<String>) results;
  }


  public List<Integer> getDesignElementIDsByGeneID(int geneID) {
    List results = template.query(DESIGN_ELEMENT_IDS_BY_GENEID,
                                  new Object[]{geneID},
                                  new DesignElementMapper());
    return (List<Integer>) results;
  }

  public List<AtlasCount> getAtlasCountsByExperimentID(int experimentID) {
    List results = template.query(ATLAS_COUNTS_BY_EXPERIMENTID,
                                  new Object[]{experimentID},
                                  new AtlasCountMapper());
    return (List<AtlasCount>) results;
  }

  public List<ExpressionAnalysis> getExpressionAnalyticsByGeneID(
      int geneID) {
    List results = template.query(EXPRESSIONANALYTICS_BY_GENEID,
                                  new Object[]{geneID},
                                  new ExpressionAnalyticsMapper());
    return (List<ExpressionAnalysis>) results;
  }

  public List<OntologyMapping> getOntologyMappingsForOntology(
      String ontologyName) {
    List results = template.query(ONTOLOGY_MAPPINGS_BY_ONTOLOGYNAME,
                                  new Object[]{ontologyName},
                                  new OntologyMappingMapper());
    return (List<OntologyMapping>) results;
  }

  private class ExperimentMapper implements RowMapper {
    public Object mapRow(ResultSet resultSet, int i) throws SQLException {
      Experiment experiment = new Experiment();

      experiment.setAccession(resultSet.getString(1));
      experiment.setDescription(resultSet.getString(2));
      experiment.setPerformer(resultSet.getString(3));
      experiment.setLab(resultSet.getString(4));
      experiment.setExperimentID(resultSet.getString(5));

      return experiment;
    }
  }

  private class GeneMapper implements RowMapper {
    public Gene mapRow(ResultSet resultSet, int i) throws SQLException {
      Gene gene = new Gene();

      gene.setGeneID(resultSet.getInt(1));
      gene.setIdentifier(resultSet.getString(2));
      gene.setName(resultSet.getString(3));
      gene.setSpecies(resultSet.getString(4));
      gene.setDesignElementID(resultSet.getInt(5));

      return gene;
    }
  }

  private class AssayMapper implements RowMapper {
    public Object mapRow(ResultSet resultSet, int i) throws SQLException {
      Assay assay = new Assay();

      assay.setAccession(resultSet.getString(1));
      assay.setExperimentAccession(resultSet.getString(2));
      assay.setArrayDesignAcession(resultSet.getString(3));
      assay.setAssayID(resultSet.getInt(4));

      return assay;
    }
  }

  private class ExpressionValueMapper implements RowMapper {
    public Object mapRow(ResultSet resultSet, int i) throws SQLException {
      ExpressionValue ev = new ExpressionValue();

      ev.setDesignElementID(resultSet.getInt(1));
      ev.setDesignElementAccession(resultSet.getString(2));
      ev.setValue(resultSet.getFloat(3));

      return ev;
    }
  }

  private class SampleMapper implements RowMapper {
    public Object mapRow(ResultSet resultSet, int i) throws SQLException {
      Sample sample = new Sample();

      sample.setAccession(resultSet.getString(1));
      sample.setSpecies(resultSet.getString(2));
      sample.setChannel(resultSet.getString(3));
      sample.setSampleID(resultSet.getInt(4));

      return sample;
    }
  }

  private class ArrayDesignMapper implements RowMapper {
    public Object mapRow(ResultSet resultSet, int i)
        throws SQLException {
      ArrayDesign array = new ArrayDesign();

      array.setAccession(resultSet.getString(1));
      array.setType(resultSet.getString(2));
      array.setName(resultSet.getString(3));
      array.setProvider(resultSet.getString(4));
      array.setArrayDesignID(resultSet.getString(5));

      return array;
    }
  }

  private class DesignElementMapper implements RowMapper {
    public Object mapRow(ResultSet resultSet, int i)
        throws SQLException {
      return resultSet.getInt(1);
    }
  }

  private class DesignElementAccMapper implements RowMapper {
    public Object mapRow(ResultSet resultSet, int i)
        throws SQLException {
      return resultSet.getString(1);
    }
  }

  private class ExpressionAnalyticsMapper implements RowMapper {
    public Object mapRow(ResultSet resultSet, int i) throws SQLException {
      ExpressionAnalysis ea = new ExpressionAnalysis();

      ea.setEfName(resultSet.getString(1));
      ea.setEfvName(resultSet.getString(2));
      ea.setExperimentID(resultSet.getLong(3));
      ea.setTStatistic(resultSet.getDouble(4));
      ea.setPValAdjusted(resultSet.getDouble(5));

      return ea;
    }
  }

  private class OntologyMappingMapper implements RowMapper {
    public Object mapRow(ResultSet resultSet, int i) throws SQLException {
      OntologyMapping mapping = new OntologyMapping();

      mapping.setExperimentID(resultSet.getString(1));
      mapping.setEfName(resultSet.getString(2));
      mapping.setEfvName(resultSet.getString(3));
      // quick bit of sugar to reformat single ,/; separated string into an array
      mapping.setOntologyTermAccessions(resultSet.getString(4).split("[,;]"));

      return mapping;
    }
  }

  private class PropertyMapper implements RowMapper {
    public Object mapRow(ResultSet resultSet, int i) throws SQLException {
      Property property = new Property();

      property.setName(resultSet.getString(1));
      property.setAccession(resultSet.getString(2));
      property.setValue(resultSet.getString(3));
      property.setFactorValue(resultSet.getBoolean(4));

      return property;
    }
  }

  private class GenePropertyMapper implements RowMapper {

    public Object mapRow(ResultSet resultSet, int i) throws SQLException {
      Property property = new Property();

      property.setName(resultSet.getString(1));
      property.setValue(resultSet.getString(2));
      property.setFactorValue(false);

      return property;
    }
  }

  private class AtlasCountMapper implements RowMapper {
    public Object mapRow(ResultSet resultSet, int i) throws SQLException {
      AtlasCount property = new AtlasCount();

      property.setProperty(resultSet.getString(1));
      property.setPropertyValue(resultSet.getString(2));
      property.setUpOrDown(resultSet.getString(3));
      property.setGeneCount(resultSet.getInt(4));

      return property;
    }
  }
}
