package ae3.service.structuredquery;

import ae3.model.AtlasExperiment;
import ae3.dao.AtlasDao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Experiments listing service class
 */
public class ExperimentsService {
    private PreparedStatement sqlGetExperiments;
    private PreparedStatement sqlGetAllGeneExperiments;
    final private Logger log = LoggerFactory.getLogger(getClass());

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
    
    private void addExperimentsToList(ExperimentList list, AtlasExperiment experiment, boolean isUp, double pval ) {
//        boolean isUp = !rs.getString("updn").contains("-");
        list.add(new ExperimentRow(
                experiment.getDwExpId(),
                experiment.getAerExpName(),
                experiment.getAerExpAccession(),
                experiment.getAerExpDescription(),
                pval,
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
                AtlasExperiment experiment = AtlasDao.getExperimentByIdDw(rs.getString("experiment_id_key"));
                if (experiment != null) {
                    addExperimentsToList(results, rs, experiment);
                }
            }
            rs.close();
        } catch (SQLException e) {
            log.error("Exception querying Atlas database", e);
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
                AtlasExperiment experiment = AtlasDao.getExperimentByIdDw(rs.getString("experiment_id_key"));
                if (experiment != null) {
                    addExperimentsToList(results.getOrCreate(rs.getString("ef"), rs.getString("efv"), creator),
                            rs, experiment);
                }
            }
            rs.close();
            return results;
        } catch (SQLException e) {
            log.error("Exception querying Atlas database", e);
        }
        return results;
    }
    
    /**
     * Populates experimentList with values from index
     * @param exps: A list of the exp_info field values
     * @return ExperimentList
     */
    public ExperimentList getExperiments(ArrayList<String> exps){
    	final ExperimentList results = new ExperimentList();
    	for(String exp : exps){
    		String[] exp_info = exp.split("/");
    		String exp_id = exp_info[0];
    		boolean isUp = !exp_info[1].contains("-");
    		double pval = Double.parseDouble(exp_info[2]);
    		AtlasExperiment experiment = AtlasDao.getExperimentByIdDw(exp_id);
            if (experiment != null) {
                addExperimentsToList(results, experiment, isUp, pval);
            }
    	}
    	return results;
    }

    public void shutdown() {
        try {
            sqlGetAllGeneExperiments.close();
            sqlGetExperiments.close();
        } catch(SQLException e) {
            log.error("Exception closing connections to Atlas database", e);
        }
    }
}