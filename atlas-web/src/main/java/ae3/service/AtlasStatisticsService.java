package ae3.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Experiments listing service class
 */
public class AtlasStatisticsService {
    private PreparedStatement sqlGetNewExperiments;
    private PreparedStatement sqlNumExperiments;
    private PreparedStatement sqlNumAssays;
    private PreparedStatement sqlNumEfvs;
    private Log log = LogFactory.getLog(getClass());

    /**
     * Constructor. Needs refernce to SQL connection containing ATLAS table.
     * @param sql reference to SQL connection to be used for queries
     * @throws java.sql.SQLException
     */
    public AtlasStatisticsService(Connection sql) throws SQLException {
        sqlGetNewExperiments = sql.prepareStatement(
                "select e.experiment_identifier, count(a.assay_id_key), e.experiment_description\n" +
                        "from ae1__experiment__main e\n" +
                        "join ae1__assay__main a on a.experiment_id_key = e.experiment_id_key\n" +
                        "where e.experiment_id_key>?\n" +
                        "group by e.experiment_id_key, e.experiment_identifier, e.experiment_description\n" +
                        "order by e.experiment_id_key");
        sqlNumExperiments = sql.prepareStatement("select count(e.experiment_id_key) from ae1__experiment__main e");
        sqlNumAssays = sql.prepareStatement("select count(a.assay_id_key) from ae1__assay__main a");
        sqlNumEfvs = sql.prepareStatement("select count(distinct a.efv) from atlas a");
    }

    static public class Exp {
        private String accession;
        private int assayCount;
        private String descr;

        public Exp(String accession, int assayCount, String descr) {
            this.accession = accession;
            this.assayCount = assayCount;
            this.descr = descr;
        }

        public String getAccession() {
            return accession;
        }

        public int getAssayCount() {
            return assayCount;
        }

        public String getDescr() {
            return descr;
        }
    };

    static public class Stats {
        private Collection<Exp> newExperiments;
        private int numExperiments;
        private int numAssays;
        private int numEfvs;

        public Stats(int numExperiments, int numAssays, int numEfvs) {
            this.newExperiments = new ArrayList<Exp>();
            this.numExperiments = numExperiments;
            this.numAssays = numAssays;
            this.numEfvs = numEfvs;
        }

        public Collection<Exp> getNewExperiments() {
            return newExperiments;
        }

        public int getNumExperiments() {
            return numExperiments;
        }

        public int getNumAssays() {
            return numAssays;
        }

        public int getNumEfvs() {
            return numEfvs;
        }

        void addNewExperiment(Exp exp) {
            newExperiments.add(exp);
        }
    };

    /**
     * Calculates Atlas statistics
     * @return statistics object
     * @param lastExperimentId
     */
    public Stats getStats(final int lastExperimentId) {
        try {
            int numExps = 0;
            int numAsss = 0;
            int numEfvs = 0;

            ResultSet rs;

            rs = sqlNumExperiments.executeQuery();
            if(rs.next())
                numExps = rs.getInt(1);
            rs.close();

            rs = sqlNumAssays.executeQuery();
            if(rs.next())
                numAsss = rs.getInt(1);
            rs.close();
            
            rs = sqlNumEfvs.executeQuery();
            if(rs.next())
                numEfvs = rs.getInt(1);
            rs.close();

            final Stats stats = new Stats(numExps, numAsss, numEfvs);

            sqlGetNewExperiments.setInt(1, lastExperimentId);
            rs = sqlGetNewExperiments.executeQuery();
            while (rs.next()) {
                stats.addNewExperiment(new Exp(rs.getString(1), rs.getInt(2), rs.getString(3)));
            }

            return stats;
        } catch (SQLException e) {
            log.error(e);
        }
        return null;
    }

}