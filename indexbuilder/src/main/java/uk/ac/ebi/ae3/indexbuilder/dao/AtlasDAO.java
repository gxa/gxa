package uk.ac.ebi.ae3.indexbuilder.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import uk.ac.ebi.microarray.atlas.loader.model.Experiment;

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
  private static final String GENES_BY_EXPERIMENT_ACCESSION =
      "SELECT DISTINCT geneid " +
          "FROM A2_GENE " +
          "WHERE experiment_id_key = ?"; // fixme: linking genes to experiments?

  private JdbcTemplate template;

  // logging
  private Log log = LogFactory.getLog(this.getClass().getSimpleName());

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

  // todo - this should return genes, once model supports them
  public List<Object> getGenesByExperimentAccession(String exptAccession) {
    List results = template.query(GENES_BY_EXPERIMENT_ACCESSION,
                                  new Object[] {exptAccession},
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

  private class GeneRowMapper implements RowMapper {

    public Object mapRow(ResultSet resultSet, int i) throws SQLException {
      // todo - this should map to Genes once they are present in the model
      return null;
    }
  }
}
