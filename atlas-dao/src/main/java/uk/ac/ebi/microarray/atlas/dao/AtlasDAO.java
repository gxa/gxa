package uk.ac.ebi.microarray.atlas.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.sql.ResultSet;
import java.sql.SQLException;
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
  private static final String EXPERIMENTS_SELECT =
      "SELECT accession, description, performer, lab " +
          "FROM a2_experiment";
  private static final String EXPERIMENT_BY_ACC_SELECT =
      EXPERIMENTS_SELECT + " " +
          "WHERE accession=?";
  private static final String ASSAYS_BY_EXPERIMENT_ACCESSION =
      "SELECT a.accession, a.experimentid, a.arraydesignid " +
          "FROM a2_assay a, a2_experiment e" +
          "WHERE e.experimentid=a.experimentid " +
          "AND e.accession=?";
  private static final String SAMPLES_BY_ASSAY_ACCESSION =
      "SELECT s.accession, s.species, s.channel " +
          "FROM a2_sample s, a2_assay a, a2_assaysample ass " +
          "WHERE s.sampleid=ass.sampleid " +
          "AND a.assayid=ass.assayid " +
          "AND a.accession=?";
  private static final String DESIGN_ELEMENTS_BY_ARRAY_ACCESSION =
      "SELECT de.accession from A2_ARRAYDESIGN ad, A2_DESIGNELEMENT de " +
          "WHERE de.arraydesignid=ad.arraydesignid" +
          "AND ad.accession=?";
  private static final String GENES_BY_EXPERIMENT_ACCESSION =
      "SELECT DISTINCT geneid " +
          "FROM a2_gene " +
          "WHERE experiment_id_key=?"; // fixme: linking genes to experiments?

  private JdbcTemplate template;

  public JdbcTemplate getJdbcTemplate() {
    return template;
  }

  public void setJdbcTemplate(JdbcTemplate template) {
    this.template = template;
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
                                  new ExperimentRowMapper());

    return results.size() > 0 ? (Experiment) results.get(0) : null;
  }

  public List<Experiment> getAllExperiments() {
    List results = template.query(EXPERIMENTS_SELECT,
                                  new ExperimentRowMapper());
    return (List<Experiment>) results;
  }

  public List<Experiment> getAllPendingExperiments() {
    // todo - implement job register/load monitor table?
    return null;
  }

  public List<Assay> getAssaysByExperimentAccession(
      String experimentAccession) {
    List results = template.query(ASSAYS_BY_EXPERIMENT_ACCESSION,
                                  new Object[]{experimentAccession},
                                  new AssayMapper());

    return (List<Assay>) results;
  }

  public List<Sample> getSamplesByAssayAccession(String assayAccession) {
    List results = template.query(SAMPLES_BY_ASSAY_ACCESSION,
                                  new Object[]{assayAccession},
                                  new SampleMapper());

    return (List<Sample>) results;
  }

  /**
   * A convenience method that fetches the set of design element accessions by
   * array design accession.  The set of design element ids contains no
   * duplicates, and the results that are returned are the manufacturers
   * accession strings for each design element on an array design.  The
   * parameters accepted are a {@link java.sql.Connection} to a database
   * following the atlas schema, and the accession string of the array design
   * (which should be of the form A-ABCD-123).
   *
   * @param arrayDesignAccession the accession number of the array design to
   *                             query for
   * @return a set of unique design element accession strings
   */
  public List<String> getDesignElementsByArrayAccession(
      String arrayDesignAccession) {
    List results = template.query(DESIGN_ELEMENTS_BY_ARRAY_ACCESSION,
                                  new Object[]{arrayDesignAccession},
                                  new DesignElementMapper());
    return (List<String>) results;
  }

  // todo - this should return genes, once model supports them
  public List<Object> getGenesByExperimentAccession(String exptAccession) {
    List results = template.query(GENES_BY_EXPERIMENT_ACCESSION,
                                  new Object[]{exptAccession},
                                  new GeneRowMapper());
    return (List<Object>) results;

  }

  private class ExperimentRowMapper implements RowMapper {
    public Object mapRow(ResultSet resultSet, int i) throws SQLException {
      Experiment experiment = new Experiment();

      experiment.setAccession(resultSet.getString(1));
      experiment.setDescription(resultSet.getString(2));
      experiment.setPerformer(resultSet.getString(3));
      experiment.setLab(resultSet.getString(4));

      return experiment;
    }
  }

  private class AssayMapper implements RowMapper {
    public Object mapRow(ResultSet resultSet, int i) throws SQLException {
      Assay assay = new Assay();

      assay.setAccession(resultSet.getString(1));
      assay.setExperimentAccession(resultSet.getString(2));
      assay.setArrayDesignAcession(resultSet.getString(3));

      return assay;
    }
  }

  private class SampleMapper implements RowMapper {
    public Object mapRow(ResultSet resultSet, int i) throws SQLException {
      Sample sample = new Sample();

      sample.setAccession(resultSet.getString(1));
      sample.setSpecies(resultSet.getString(2));
      sample.setChannel(resultSet.getString(3));

      return sample;
    }
  }

  private class DesignElementMapper implements RowMapper {
    public Object mapRow(ResultSet resultSet, int i)
        throws SQLException {
      return resultSet.getString(1);
    }
  }

  private class GeneRowMapper implements RowMapper {

    public Object mapRow(ResultSet resultSet, int i) throws SQLException {
      // todo - this should map to Genes once they are present in the model
      return null;
    }
  }
}
