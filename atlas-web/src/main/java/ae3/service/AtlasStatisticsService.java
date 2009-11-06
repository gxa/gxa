package ae3.service;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.AtlasStatistics;

/**
 * Experiments listing service class
 */
public class AtlasStatisticsService {
    private AtlasDAO atlasDAO;
    private SolrServer solrServer;

    // logging
    final private Logger log = LoggerFactory.getLogger(getClass());

//    /**
//     * Constructor. Needs refernce to SQL connection containing ATLAS table.
//     *
//     * @param sql reference to SQL connection to be used for queries
//     * @throws java.sql.SQLException
//     */
//    public AtlasStatisticsService() {
//        sqlGetNewExperiments = sql.prepareStatement(
//                "select e.experiment_identifier, count(a.assay_id_key), e.experiment_description\n" +
//                        "from ae1__experiment__main e\n" +
//                        "join ae1__assay__main a on a.experiment_id_key = e.experiment_id_key\n" +
//                        "where e.experiment_identifier not in ('E-MTAB-24','E-MTAB-25','E-HD4D-1','E-HD4D-2','E-HD4D-3','E-TABM-145a','E-TABM-145b','E-TABM-145c') and e.experiment_id_key>?\n" +
//                        "group by e.experiment_id_key, e.experiment_identifier, e.experiment_description\n" +
//                        "order by e.experiment_id_key");
//        sqlNumExperiments = sql.prepareStatement("select count(e.experiment_id_key) from ae1__experiment__main e where experiment_accession not in ('E-MTAB-24','E-MTAB-25','E-HD4D-1','E-HD4D-2','E-HD4D-3','E-TABM-145a','E-TABM-145b','E-TABM-145c')");
//        sqlNumAssays = sql.prepareStatement("select count(a.assay_id_key) from ae1__assay__main a");
//
//        sqlNumGenes = sql.prepareStatement("select count(1) from ae2__gene__main");
//    }

    public AtlasDAO getAtlasDAO() {
        return atlasDAO;
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    public SolrServer getSolrServer() {
        return solrServer;
    }

    public void setSolrServer(SolrServer solrServer) {
        this.solrServer = solrServer;
    }

    /**
     * Calculates Atlas statistics
     *
     * @param lastExperimentId the last experiment loaded in this release
     * @param dataRelease      the current data release version
     * @return statistics object
     */
    public AtlasStatistics getStats(final int lastExperimentId, final String dataRelease) {
        int numExps = 0;
        int numAsss = 0;
        int numEfvs = 0;

//            ResultSet rs;
//            rs = sqlNumExperiments.executeQuery();
//            if (rs.next())
//                numExps = rs.getInt(1);
//            rs.close();
//
//            rs = sqlNumAssays.executeQuery();
//            if (rs.next())
//                numAsss = rs.getInt(1);
//            rs.close();
//
//            numEfvs = countEfvs();
//
//            final Stats stats = new Stats(numExps, numAsss, numEfvs, dataRelease);
//
//            sqlGetNewExperiments.setInt(1, lastExperimentId);
//            rs = sqlGetNewExperiments.executeQuery();
//            while (rs.next()) {
//                stats.addNewExperiment(new Exp(rs.getString(1), rs.getInt(2), rs.getString(3)));
//            }
//
//            //AZ:2009-07-06:caclulate number of genes
//            rs = sqlNumGenes.executeQuery();
//            if (rs.next())
//                stats.numGenes = rs.getInt(1);
//            rs.close();

        AtlasStatistics stats = getAtlasDAO().getAtlasStatistics(lastExperimentId, dataRelease);

        return stats;
    }

    private int countEfvs() {
        try {
            SolrQuery q = new SolrQuery("exp_in_dw:true");
            q.setRows(0);
            q.setFacet(true);
            q.setFacetMinCount(1);
            q.addFacetField("exp_factor_values_exact");
            q.setFacetLimit(-1);
            q.setFacetSort(false);
            QueryResponse qr = solrServer.query(q);
            return qr.getFacetFields().get(0).getValues().size();
        } catch (Exception e) {
            log.error("Something's gone terribly wrong calculating EFVs", e);
        }

        return 0;
    }
}