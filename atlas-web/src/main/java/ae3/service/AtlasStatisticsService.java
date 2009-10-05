package ae3.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;

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
    private PreparedStatement sqlNumGenes;
    private SolrServer solrExpt;
    final private Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Constructor. Needs refernce to SQL connection containing ATLAS table.
     * @param sql reference to SQL connection to be used for queries
     * @throws java.sql.SQLException
     */
    public AtlasStatisticsService(Connection sql, SolrServer solrExpt) throws SQLException {
        sqlGetNewExperiments = sql.prepareStatement(
                "select e.experiment_identifier, count(a.assay_id_key), e.experiment_description\n" +
                        "from ae1__experiment__main e\n" +
                        "join ae1__assay__main a on a.experiment_id_key = e.experiment_id_key\n" +
                        "where e.experiment_identifier not in ('E-MTAB-24','E-MTAB-25','E-HD4D-1','E-HD4D-2','E-HD4D-3','E-TABM-145a','E-TABM-145b','E-TABM-145c') and e.experiment_id_key>?\n" +
                        "group by e.experiment_id_key, e.experiment_identifier, e.experiment_description\n" +
                        "order by e.experiment_id_key");
        sqlNumExperiments = sql.prepareStatement("select count(e.experiment_id_key) from ae1__experiment__main e where experiment_accession not in ('E-MTAB-24','E-MTAB-25','E-HD4D-1','E-HD4D-2','E-HD4D-3','E-TABM-145a','E-TABM-145b','E-TABM-145c')");
        sqlNumAssays = sql.prepareStatement("select count(a.assay_id_key) from ae1__assay__main a");

        sqlNumGenes = sql.prepareStatement("select count(1) from ae2__gene__main");

        this.solrExpt = solrExpt;
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
        private int numGenes;
        private String dataRelease;

        public Stats(int numExperiments, int numAssays, int numEfvs, String dataRelease) {
            this.dataRelease = dataRelease;
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

        public String getDataRelease() {
            return dataRelease;
        }

        public int getNumGenes() {
            return numGenes;
        }
    };

    private int countEfvs() {
        try {
            SolrQuery q = new SolrQuery("exp_in_dw:true");
            q.setRows(0);
            q.setFacet(true);
            q.setFacetMinCount(1);
            q.addFacetField("exp_factor_values_exact");
            q.setFacetLimit(-1);
            q.setFacetSort(false);
            QueryResponse qr = solrExpt.query(q);
            return qr.getFacetFields().get(0).getValues().size();
        } catch(Exception e) {
            log.error("Something's gone terribly wrong calculating EFVs", e);
        }

        return 0;
    }

    /**
     * Calculates Atlas statistics
     * @return statistics object
     * @param lastExperimentId
     */
    public Stats getStats(final int lastExperimentId, final String dataRelease) {
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

            numEfvs = countEfvs();

            final Stats stats = new Stats(numExps, numAsss, numEfvs, dataRelease);

            sqlGetNewExperiments.setInt(1, lastExperimentId);
            rs = sqlGetNewExperiments.executeQuery();
            while (rs.next()) {
                stats.addNewExperiment(new Exp(rs.getString(1), rs.getInt(2), rs.getString(3)));
            }

            //AZ:2009-07-06:caclulate number of genes
            rs = sqlNumGenes.executeQuery();
            if(rs.next())
                stats.numGenes = rs.getInt(1);
            rs.close();


            return stats;
        } catch (SQLException e) {
            log.error("Error querying Atlas database", e);
        }
        return null;
    }

    public void shutdown() {
        try {
            sqlGetNewExperiments.close();
            sqlNumAssays.close();
            sqlNumExperiments.close();
            sqlNumGenes.close();
        } catch(SQLException e) {
            log.error("Error querying Atlas database", e);
        }
    }

}