package ae3.service;

import ae3.dao.AtlasDao;
import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import ae3.model.AtlasTuple;
import ae3.ols.webservice.axis.Query;
import ae3.ols.webservice.axis.QueryServiceLocator;
import ae3.service.compute.AtlasComputeService;
import ae3.service.structuredquery.AtlasStructuredQueryService;
import ae3.util.AtlasProperties;
import ae3.util.QueryHelper;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ae3.indexbuilder.Constants;

import javax.sql.DataSource;
import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: ostolop
 * Date: 12-Feb-2008
 * <p/>
 *
 * The singleton application class for servicing ArrayExpress searches
 *
 * TODO: Add specialized exceptions.
 *
 * EBI Microarray Informatics Team (c) 2007, 2008
 */
public class ArrayExpressSearchService {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    //// Full-text Search Index Instances

    private CoreContainer multiCore;
    private SolrServer solr_expt;
    private SolrServer solr_atlas;
    private String solrIndexLocation;

    // ArrayExpress (AEW/Atlas) RDBMS DataSource
    private DataSource theAEDS;
    private QueryRunner theAEQueryRunner;

    private AtlasStructuredQueryService squeryService;
    private AtlasStatisticsService.Stats stats;

    private AtlasComputeService computeService;
    private AtlasDownloadService downloadService;

    private AtlasDao atlasDao;

    private ArrayExpressSearchService() {}
    private static ArrayExpressSearchService _instance = null;

    /**
     * Returns the singleton instance.
     *
     * @return Singleton instance of AtlasSearch
     */
    public static ArrayExpressSearchService instance() {
      if(null == _instance) {
         _instance = new ArrayExpressSearchService();
      }

      return _instance;
    }

    public void initialize() {
        try {
          File solrIndexDir = new File(solrIndexLocation);
          log.info("SOLR Index at: " + solrIndexDir.getAbsolutePath());
          solrIndexLocation = solrIndexDir.getAbsolutePath();

	        multiCore = new CoreContainer(solrIndexLocation, new File(solrIndexLocation, "solr.xml"));

            solr_expt = new EmbeddedSolrServer(multiCore, Constants.CORE_EXPT);
            solr_atlas = new EmbeddedSolrServer(multiCore, Constants.CORE_ATLAS);

            atlasDao = new AtlasDao(multiCore);

            squeryService = new AtlasStructuredQueryService(multiCore);

            try {
                computeService = new AtlasComputeService();
            } catch (Exception e) {
                log.error("Compute service initialization failed", e);
            }

            downloadService = new AtlasDownloadService();

            try {
                AtlasStatisticsService sserv = new AtlasStatisticsService(theAEDS.getConnection(), solr_expt);

                int lastExpId = AtlasProperties.getIntProperty("atlas.last.experiment");
                String dataRelease = AtlasProperties.getProperty("atlas.data.release");
                stats = sserv.getStats(lastExpId, dataRelease);
            } catch (Exception e) {
                log.error("Statistics failed", e);
            }

            new Thread() { public void run() { squeryService.getEfvListHelper().preloadData(); } }.start();
            new Thread() { public void run() { squeryService.getEfoListHelper().preloadData(); } }.start();
            new Thread() { public void run() { squeryService.getGeneListHelper().preloadData(); } }.start();

            theAEQueryRunner = new QueryRunner(theAEDS);
            
        } catch (Exception e) {
            throw new RuntimeException("Fatal initialization error", e);
        }

    }



    /**
     * Should be called when app is going down.
     */
    public void shutdown() {
        log.info("Shutting down ArrayExpressSearchService.");

        computeService.shutdown();
        computeService = null;

        downloadService.shutdown();
        squeryService = null;

        log.info("Shutting down DB connections and indexes");
        try {
            if (theAEDS != null) theAEDS = null;

            if (multiCore != null) {
                solr_atlas = null;
            	solr_expt = null;
            	multiCore.shutdown();
                multiCore = null;
            }
        } catch (Exception e) {
            log.error("Error shutting down ArrayExpressSearchService!", e);
        }
    }

    public QueryResponse fullTextQueryGenes(String query) {
        if (query == null || query.equals(""))
            return null;

        try {
           // for now just search on the first 500 query terms
           if ( query.split("\\s").length > 500 ) {
               Pattern pattern = Pattern.compile("(\\p{Alnum}+\\s){500}");
               Matcher matcher = pattern.matcher(query);

               if(matcher.find()) {
                   SolrQuery q = new SolrQuery(query);
                   q.setRows(100);
                   return solr_atlas.query(q);
               }
           } else {
               SolrQuery q = new SolrQuery(query);
               q.setRows(500);
               q.setHighlight(true);
               q.addHighlightField("gene_goterm");
               q.addHighlightField("gene_interproterm");
               q.addHighlightField("gene_keyword");
               q.addHighlightField("gene_name");
               q.addHighlightField("gene_synonym");
               q.setHighlightSnippets(100);
               QueryResponse queryResponse = solr_atlas.query(q);
               return queryResponse;
           }
        } catch (SolrServerException e) {
            log.error("Solr error querying genes", e);
        }

        return null;
   }


    /**
     * Performs a full text SOLR search on experiments.
     *
     * @param query query terms, can be a complete Lucene query or just a bit of text
     * @return {@link org.w3c.dom.Document}
     */
    public QueryResponse fullTextQueryExpts(String query) {
    	return fullTextQueryExpts(query, 0, 50);
    }

    /**
     * Performs pagination and full text SOLR search on experiments.
     * @param query - A lucene query
     * @param start - a start record
     * @param rows - maximum number of Documents
     * @return
     */
    public QueryResponse fullTextQueryExpts(String query, int start, int rows)
    {
        if (query == null || query.equals(""))
            return null;

        try {
            if (query.indexOf("exp_factor_values:") == -1) {
                query = "exp_factor_values:(" + query + ")";
            }
            SolrQuery q = new SolrQuery(query);
            q.setHighlight(true);
            q.addHighlightField("exp_factor_values");
            q.setHighlightSnippets(100);
            q.setRows(rows);
            q.setStart(start);
            q.setFilterQueries("exp_in_dw:true");

            return solr_expt.query(q);
        } catch (SolrServerException e) {
            log.error("Solr error querying expeirments", e);
        }

        return null;

    }

    /**
     * Executes an atlas query to retrieve expt acc, desc, ef, efv, gene, updn, and p-value for requested gene ids,
     * optionally restricting to a supplied list of experiment ids.
     */
    @SuppressWarnings("unchecked")
    public List<AtlasResult> doAtlasQuery(  final QueryResponse geneHitsResponse,
                                            final QueryResponse exptHitsResponse,
                                            final String geneExprFilter,
                                            final String geneSpeciesFilter) {
        if (geneHitsResponse == null && exptHitsResponse == null)
            return null;

        if (geneHitsResponse != null && geneHitsResponse.getResults().getNumFound() == 0)
            return null;

        if (exptHitsResponse != null && exptHitsResponse.getResults().getNumFound() == 0)
            return null;

        String updn_filter = " and updn <> 0\n";

        if (geneExprFilter.equals("up"))
            updn_filter = " and updn = 1\n";

        if (geneExprFilter.equals("down"))
            updn_filter = " and updn = -1\n";

        final Map<String, SolrDocument> solrExptMap = QueryHelper.convertSolrDocumentListToMap(exptHitsResponse, Constants.FIELD_DWEXP_ID);
        final Map<String, SolrDocument> solrGeneMap = QueryHelper.convertSolrDocumentListToMap(geneHitsResponse, "gene_id");

        String inGeneIds = (solrGeneMap == null ? "" : StringUtils.join(solrGeneMap.keySet(), ","));
        String inExptIds = (solrExptMap == null ? "" : StringUtils.join(solrExptMap.keySet(), ","));

        String efvFilter = "";

        if (exptHitsResponse != null /*&& inGeneIds.length() == 0*/ ) {
            Set ss = new HashSet<String>();
            Map<String,Map<String, List<String>>> hl = exptHitsResponse.getHighlighting();

            for ( Map<String, List<String>> vals : hl.values() ) {
                if (vals == null || vals.size() == 0) continue;
                for(String s : vals.get("exp_factor_values")) {
                    ss.add("'" + s.replaceAll("</{0,1}em>","").replaceAll("'", "''") + "'");
                }
            }

            if (ss.size() > 0)
                efvFilter = "and atlas.efv IN (" + StringUtils.join(ss.toArray(), ",") + ") \n";
        }

        String atlas_query_topN = "SELECT * FROM (\n" +
                " SELECT\n" +
                "         atlas.experiment_id_key,\n" +
                "         atlas.gene_id_key, \n" +
                "         atlas.ef, \n" +
                "         atlas.efv,\n" +
                "         atlas.updn,\n" +
                "         atlas.updn_tstat,\n" +
                "         atlas.updn_pvaladj,\n" +
                "         row_number()\n" +
                "            OVER (\n" +
                "              PARTITION BY atlas.EXPERIMENT_ID_KEY, atlas.ef, atlas.efv\n" +
                "              ORDER BY atlas.updn_pvaladj asc, UPDN_TSTAT desc\n" +
                "            ) TopN \n"+ //,\n" +
                "        from aemart.atlas atlas \n" +
                "        where gene_id_key <> 0 " +
                " and atlas.experiment_id_key NOT IN (211794549,215315583,384555530,411493378,411512559) \n" + // ignore E-TABM-145a,b,c
                (inGeneIds.length() != 0 ? "and atlas.gene_id_key       IN (" + inGeneIds + ") \n" : "" ) +
                (inExptIds.length() != 0 ? "and atlas.experiment_id_key IN (" + inExptIds + ") \n" : "" ) +
                updn_filter +
                efvFilter   +
                ")\n" +
                " WHERE \n" +
                " TopN <= 20 and gene_id_key is not null\n" +
                " ORDER by updn_pvaladj asc, updn_tstat desc";

        log.debug(atlas_query_topN);

        final List<AtlasResult> arset = new Vector<AtlasResult>();

        try {
            theAEQueryRunner.query(atlas_query_topN, new ResultSetHandler() {
                public List<AtlasResult> handle(ResultSet rs) throws SQLException {
                    while(rs.next()) {
                        AtlasResult atlasResult = new AtlasResult();

                        AtlasExperiment expt = null;
                        AtlasGene gene       = null;

                        String experiment_id_key = rs.getString("experiment_id_key");
                        String gene_id_key = rs.getString("gene_id_key");

                        if (solrExptMap != null && solrExptMap.containsKey(experiment_id_key)) {
                            expt = new AtlasExperiment(solrExptMap.get(experiment_id_key));
                            expt.setExperimentHighlights(exptHitsResponse.getHighlighting().get(expt.getDwExpAccession()));
                        } else {
                            expt = atlasDao.getExperimentById(experiment_id_key);
                        }

                        if (solrGeneMap != null && solrGeneMap.containsKey(gene_id_key)) {
                            gene = new AtlasGene(solrGeneMap.get(gene_id_key));
                            gene.setGeneHighlights(geneHitsResponse.getHighlighting().get(gene_id_key));
                        } else {
                            gene = atlasDao.getGeneById(gene_id_key).getGene();
                        }

                        if(expt == null || gene == null) {
                            log.error("Atlas object retrieval error");
                        }

                        AtlasTuple atuple = new AtlasTuple(rs.getString("ef"), rs.getString("efv"), rs.getInt("updn"), rs.getDouble("updn_pvaladj"));

                        //TODO: disabled highlighting in experiments, it's broken on SOLR side, it seems. Example: query for liver.
                        if ( expt != null && expt.getExperimentHighlights() != null) {
                            List<String> s = expt.getExperimentHighlights().get("exp_factor_values");
                            if (s != null ) {
                                for (String efv : s) {
                                    if(atuple.getEfv().equals(efv.replaceAll("</{0,1}em>",""))) {
                                        atuple.setEfv(efv);
                                        break;
                                    }
                                }
                            }
                        }

                        if( ( expt != null && gene != null )
                                &&
                            ( geneSpeciesFilter == null || geneSpeciesFilter.equals("") || geneSpeciesFilter.equals("any") || geneSpeciesFilter.equalsIgnoreCase(gene.getGeneSpecies())) ) {
                                atlasResult.setExperiment(expt);
                                atlasResult.setGene(gene);
                                atlasResult.setAtuple(atuple);

                            arset.add(atlasResult);
                        }
                    }

                    return arset;
                }
            });

            log.debug("Retrieved query completely: " + arset.size() + " records" );
        } catch (SQLException e) {
            log.error("Problem querying Atlas database", e);
        }

        return arset;
    }


    public void setAEDataSource(DataSource aeds) {
        this.theAEDS = aeds;
    }

    public void setSolrIndexLocation(String solrIndexLocation) {
        this.solrIndexLocation = solrIndexLocation;
    }

    public AtlasStructuredQueryService getStructQueryService() {
        return squeryService;
    }

    public Iterable<String> getGeneProperties(){
    	return squeryService.getGeneProperties();
    }

    public SortedSet<String> getAllAvailableAtlasSpecies() {
        SortedSet<String> species = new TreeSet<String>();
        for(String s : squeryService.getGeneListHelper().listAllValues("species")) {
            SolrQuery q = new SolrQuery("gene_species:(" + s + ")");
            q.setRows(1);
            try {
                QueryResponse qr = solr_atlas.query(q);
                if(qr.getResults() != null && qr.getResults().size() > 0) {
                    String specie = (String)qr.getResults().get(0).getFieldValue("gene_species");
                    if(specie.length() > 1)
                        species.add(specie.substring(0, 1).toUpperCase() + specie.substring(1).toLowerCase());
                }
            } catch(SolrServerException e) {
                throw new RuntimeException("can't get species list", e);
            }

        }
        return species;
    }


    public AtlasStatisticsService.Stats getStats() {
        return stats;
    }

    /**
     * Gets the Solr core from the main core container. Don't forget to close when finished with it.
     *
     * @param coreName core to return
     * @return SolrCore
     */
    public SolrCore getSolrCore(final String coreName) {
        return multiCore.getCore(coreName);
    }

    public AtlasComputeService getComputeService() {
        return computeService;
    }

    public AtlasDownloadService getDownloadService() {
        return downloadService;
    }

    public AtlasDao getAtlasDao() {
        return atlasDao;
    }
}
