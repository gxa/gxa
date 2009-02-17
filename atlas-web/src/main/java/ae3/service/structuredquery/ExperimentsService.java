package ae3.service.structuredquery;

import ae3.model.AtlasExperiment;
import ae3.dao.AtlasObjectNotFoundException;
import ae3.dao.AtlasDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Experiments listing service class
 */
public class ExperimentsService {
    private PreparedStatement sqlGetExperiments;
    private PreparedStatement sqlGetAllGeneExperiments;
    private Log log = LogFactory.getLog(ExperimentsService.class);

    /**
     * Constructor. Needs refernce to SQL connection containing ATLAS table.
     * @param sql reference to SQL connection to be used for queries
     * @throws SQLException
     */
    public ExperimentsService(Connection sql) throws SQLException  {
        sqlGetExperiments = sql.prepareStatement(
                "SELECT experiment_id_key, avg(updn_pvaladj) as updn_pvaladj,updn FROM aemart.atlas" +
                " WHERE gene_id_key = ? AND ef = ? AND efv = ? GROUP BY experiment_id_key,updn");

        sqlGetAllGeneExperiments = sql.prepareStatement(
                "select experiment_id_key, ef, efv, avg(round(updn_pvaladj,100)) as updn_pvaladj from atlas " +
                        "where gene_id_key = ? group by ef,efv,experiment_id_key"
        );
    }

    private void addExperimentsToList(ExperimentList list, ResultSet rs, AtlasExperiment experiment) throws SQLException {
        boolean isUp = !rs.getString("updn").contains("-");
        list.add(new ExperimentRow(
                experiment.getDwExpId(),
                experiment.getAerExpName(),
                experiment.getAerExpAccession(),
                experiment.getAerExpDescription(),
                rs.getDouble("updn_pvaladj"),
                (isUp ? ExperimentRow.UpDn.UP : ExperimentRow.UpDn.DOWN)));
    }

    /**
     * Returns list of experiments by gene id, factor and factorvalue
     * @param gene_id_key gene id
     * @param factor factor name
     * @param factorValue factor value
     * @return {@link ae3.service.structuredquery.ExperimentList} container class
     */
    public ExperimentList getExperiments(String gene_id_key, String factor, String factorValue) {
        final ExperimentList results = new ExperimentList();
        try {
            log.info("Listing experiments for gene:" + gene_id_key + " ef:" + factor + " efv:" + factorValue);
            sqlGetExperiments.setString(1, gene_id_key);
            sqlGetExperiments.setString(2, factor);
            sqlGetExperiments.setString(3, factorValue);
            ResultSet rs = sqlGetExperiments.executeQuery();
            while (rs.next()) {
                try {
                    AtlasExperiment experiment = AtlasDao.getExperimentByIdDw(rs.getString("experiment_id_key"));
                    if (experiment != null) {
                        addExperimentsToList(results, rs, experiment);
                    }
                } catch (AtlasObjectNotFoundException e) {
                    log.error(e);
                }
            }
        } catch (SQLException e) {
            log.error(e);
        }
        return results;
    }

    /**
     * Returns list of all experiments for specific gene
     * @param gene_id_key gene id
     * @return {@link ae3.service.structuredquery.ExperimentList}
     */
    public EfvTree<ExperimentList> getExperiments(String gene_id_key) {
        final EfvTree<ExperimentList> results = new EfvTree<ExperimentList>();
        try {
            log.info("Listing experiments for gene:" + gene_id_key);
            sqlGetAllGeneExperiments.setString(1, gene_id_key);
            ResultSet rs = sqlGetAllGeneExperiments.executeQuery();
            EfvTree.Creator<ExperimentList> creator = new EfvTree.Creator<ExperimentList>() {
                public ExperimentList make() {
                    return new ExperimentList();
                }
            };
            while (rs.next()) {
                try {
                    AtlasExperiment experiment = AtlasDao.getExperimentByIdDw(rs.getString("experiment_id_key"));
                    if (experiment != null) {
                        addExperimentsToList(results.getOrCreate(rs.getString("ef"), rs.getString("efv"), creator),
                                rs, experiment);
                    }
                } catch (AtlasObjectNotFoundException e) {
                    log.error(e);
                }
            }
            return results;
        } catch (SQLException e) {
            log.error(e);
        }
        return results;
    }
}