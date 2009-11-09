package uk.ac.ebi.gxa.web;

import ae3.dao.AtlasDao;
import ae3.service.AtlasDownloadService;
import ae3.service.AtlasResult;
import ae3.service.AtlasStatisticsService;
import ae3.service.compute.AtlasComputeService;
import ae3.service.structuredquery.AtlasStructuredQueryService;
import ae3.util.AtlasProperties;
import ae3.util.QueryHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import uk.ac.ebi.ae3.indexbuilder.Constants;
import uk.ac.ebi.microarray.atlas.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.AtlasStatistics;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A service that is used to perform various Atlas searches.  This needs to be wired with several data access classes,
 * as searches are performed against the Atlas database and the Atlas SOLR index.  There are also several nested
 * services that can be acquired from this class for doing download, compute, and query operations.
 * <p/>
 *
 * @author Misha Kapushesky (original)
 * @author Tony Burdett
 * @description EBI Microarray Informatics Team (c) 2007, 2008
 * @date 12-Feb-2008
 */
public class AtlasSearchService implements InitializingBean {
    private CoreContainer multiCore;
    private SolrServer experimentsSolrServer;
    private SolrServer atlasSolrServer;

    private AtlasStatisticsService atlasStatisticsService;
    private AtlasComputeService atlasComputeService;
    private AtlasDownloadService atlasDownloadService;
    private AtlasStructuredQueryService atlasQueryService;

    private File atlasIndex;
    private AtlasDAO atlasDatabaseDAO;
    private AtlasDao atlasSolrDAO;

    private AtlasStatistics stats;

    // logging
    protected final Logger log = LoggerFactory.getLogger(getClass());

    public CoreContainer getMultiCore() {
        return multiCore;
    }

    public void setMultiCore(CoreContainer multiCore) {
        this.multiCore = multiCore;
    }

    public SolrServer getExperimentsSolrServer() {
        return experimentsSolrServer;
    }

    public void setExperimentsSolrServer(SolrServer solr_expt) {
        this.experimentsSolrServer = solr_expt;
    }

    public SolrServer getAtlasSolrServer() {
        return atlasSolrServer;
    }

    public void setAtlasSolrServer(SolrServer atlasSolrServer) {
        this.atlasSolrServer = atlasSolrServer;
    }

    public AtlasStatisticsService getAtlasStatisticsService() {
        return atlasStatisticsService;
    }

    public void setAtlasStatisticsService(AtlasStatisticsService atlasStatisticsService) {
        this.atlasStatisticsService = atlasStatisticsService;
    }

    public AtlasComputeService getAtlasComputeService() {
        return atlasComputeService;
    }

    public void setAtlasComputeService(AtlasComputeService atlasComputeService) {
        this.atlasComputeService = atlasComputeService;
    }

    public AtlasDownloadService getAtlasDownloadService() {
        return atlasDownloadService;
    }

    public void setAtlasDownloadService(AtlasDownloadService atlasDownloadService) {
        this.atlasDownloadService = atlasDownloadService;
    }

    public AtlasStructuredQueryService getAtlasQueryService() {
        return atlasQueryService;
    }

    public void setAtlasQueryService(AtlasStructuredQueryService atlasQueryService) {
        this.atlasQueryService = atlasQueryService;
    }

    public File getAtlasIndex() {
        return atlasIndex;
    }

    public void setAtlasIndex(File atlasIndex) {
        this.atlasIndex = atlasIndex;
    }

    public AtlasDAO getAtlasDatabaseDAO() {
        return atlasDatabaseDAO;
    }

    public void setAtlasDatabaseDAO(AtlasDAO atlasDatabaseDAO) {
        this.atlasDatabaseDAO = atlasDatabaseDAO;
    }

    public AtlasDao getAtlasSolrDAO() {
        return atlasSolrDAO;
    }

    public void setAtlasSolrDAO(AtlasDao atlasSolrDAO) {
        this.atlasSolrDAO = atlasSolrDAO;
    }

    public void afterPropertiesSet() throws Exception {
        initialize();
    }

    public void initialize() {
        try {
            System.out.println("Initializing AtlasSearchService");

            // fixme: this shuld be configured low-level, e.g. in a DAO module and injected
            // startup SOLR server
            multiCore = new CoreContainer(atlasIndex.getAbsolutePath(), new File(atlasIndex, "solr.xml"));

            experimentsSolrServer = new EmbeddedSolrServer(multiCore, Constants.CORE_EXPT);
            atlasSolrServer = new EmbeddedSolrServer(multiCore, Constants.CORE_ATLAS);

            // create a SOLR DAO
            atlasSolrDAO = new AtlasDao(multiCore);

            // do required additional setup for SOLR things that weren't set by spring
            atlasQueryService = new AtlasStructuredQueryService(multiCore);
            atlasDownloadService.setAtlasQueryService(atlasQueryService);

            // do some precanned queries the interface requires
            try {
                atlasStatisticsService.setSolrServer(experimentsSolrServer);

                int lastExpId = AtlasProperties.getIntProperty("atlas.last.experiment");
                String dataRelease = AtlasProperties.getProperty("atlas.data.release");
                stats = atlasStatisticsService.getStats(lastExpId, dataRelease);
            }
            catch (Exception e) {
                log.error("Statistics failed", e);
            }

            new Thread() {
                public void run() {
                    atlasQueryService.getEfvListHelper().preloadData();
                }
            }.start();
            new Thread() {
                public void run() {
                    atlasQueryService.getEfoListHelper().preloadData();
                }
            }.start();
            new Thread() {
                public void run() {
                    atlasQueryService.getGeneListHelper().preloadData();
                }
            }.start();
        }
        catch (Exception e) {
            throw new RuntimeException("Fatal initialization error", e);
        }
    }

    /**
     * Should be called when app is going down.
     */
    public void shutdown() {
        log.info("Shutting down ArrayExpressSearchService.");

        atlasComputeService.shutdown();
        atlasComputeService = null;

        atlasDownloadService.shutdown();
        atlasQueryService = null;

        log.info("Shutting down DB connections and indexes");
        try {
            if (multiCore != null) {
                atlasSolrServer = null;
                experimentsSolrServer = null;
                multiCore.shutdown();
                multiCore = null;
            }
        }
        catch (Exception e) {
            log.error("Error shutting down ArrayExpressSearchService!", e);
        }
    }

    public Iterable<String> getGeneProperties() {
        return atlasQueryService.getGeneProperties();
    }

    public SortedSet<String> getAllAvailableAtlasSpecies() {
        SortedSet<String> species = new TreeSet<String>();
        for (String s : atlasQueryService.getGeneListHelper().listAllValues("species")) {
            SolrQuery q = new SolrQuery("gene_species:(" + s + ")");
            q.setRows(1);
            try {
                QueryResponse qr = atlasSolrServer.query(q);
                if (qr.getResults() != null && qr.getResults().size() > 0) {
                    String specie = (String) qr.getResults().get(0).getFieldValue("gene_species");
                    if (specie.length() > 1) {
                        species.add(specie.substring(0, 1).toUpperCase() + specie.substring(1).toLowerCase());
                    }
                }
            }
            catch (SolrServerException e) {
                throw new RuntimeException("can't get species list", e);
            }

        }
        return species;
    }


    public AtlasStatistics getStats() {
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

    public QueryResponse fullTextQueryGenes(String query) {
        if (query == null || query.equals("")) {
            return null;
        }

        try {
            // for now just search on the first 500 query terms
            if (query.split("\\s").length > 500) {
                Pattern pattern = Pattern.compile("(\\p{Alnum}+\\s){500}");
                Matcher matcher = pattern.matcher(query);

                if (matcher.find()) {
                    SolrQuery q = new SolrQuery(query);
                    q.setRows(100);
                    return atlasSolrServer.query(q);
                }
            }
            else {
                SolrQuery q = new SolrQuery(query);
                q.setRows(500);
                q.setHighlight(true);
                q.addHighlightField("gene_goterm");
                q.addHighlightField("gene_interproterm");
                q.addHighlightField("gene_keyword");
                q.addHighlightField("gene_name");
                q.addHighlightField("gene_synonym");
                q.setHighlightSnippets(100);
                QueryResponse queryResponse = atlasSolrServer.query(q);
                return queryResponse;
            }
        }
        catch (SolrServerException e) {
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
     *
     * @param query - A lucene query
     * @param start - a start record
     * @param rows  - maximum number of Documents
     * @return
     */
    public QueryResponse fullTextQueryExpts(String query, int start, int rows) {
        if (query == null || query.equals("")) {
            return null;
        }

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

            return experimentsSolrServer.query(q);
        }
        catch (SolrServerException e) {
            log.error("Solr error querying expeirments", e);
        }

        return null;

    }

    /**
     * Executes an atlas query to retrieve expt acc, desc, ef, efv, gene, updn, and p-value for requested gene ids,
     * optionally restricting to a supplied list of experiment ids.
     */
    @SuppressWarnings("unchecked")
    public List<AtlasResult> doAtlasQuery(final QueryResponse geneHitsResponse,
                                          final QueryResponse exptHitsResponse,
                                          final String geneExprFilter,
                                          final String geneSpeciesFilter) {
        if (geneHitsResponse == null && exptHitsResponse == null) {
            return null;
        }

        if (geneHitsResponse != null && geneHitsResponse.getResults().getNumFound() == 0) {
            return null;
        }

        if (exptHitsResponse != null && exptHitsResponse.getResults().getNumFound() == 0) {
            return null;
        }

        final Map<String, SolrDocument> solrExptMap =
                QueryHelper.convertSolrDocumentListToMap(exptHitsResponse, Constants.FIELD_DWEXP_ID);
        final Map<String, SolrDocument> solrGeneMap =
                QueryHelper.convertSolrDocumentListToMap(geneHitsResponse, "gene_id");

        // parse integer values of geneIds
        int[] geneIDs;
        if (solrGeneMap == null) {
            geneIDs = new int[0];
        }
        else {
            geneIDs = new int[solrGeneMap.keySet().size()];
            int i = 0;
            for (String s : solrGeneMap.keySet()) {
                geneIDs[i] = Integer.parseInt(s);
            }
        }

        // parse integer values of exptIds
        int[] exptIDs;
        if (solrExptMap == null) {
            exptIDs = new int[0];
        }
        else {
            exptIDs = new int[solrExptMap.keySet().size()];
            int i = 0;
            for (String s : solrExptMap.keySet()) {
                exptIDs[i] = Integer.parseInt(s);
            }
        }

        // derive whether this query is for genes "up" or "down"
        int upOrDown;
        if (geneExprFilter.equals("up")) {
            upOrDown = 1;
        }
        else if (geneExprFilter.equals("down")) {
            upOrDown = -1;
        }
        else {
            upOrDown = 0;
        }

        // parse the efvs being queried for
        String efvFilter = "";
        if (exptHitsResponse != null /*&& inGeneIds.length() == 0*/) {
            Set ss = new HashSet<String>();
            Map<String, Map<String, List<String>>> hl = exptHitsResponse.getHighlighting();

            for (Map<String, List<String>> vals : hl.values()) {
                if (vals == null || vals.size() == 0) {
                    continue;
                }
                for (String s : vals.get("exp_factor_values")) {
                    ss.add("'" + s.replaceAll("</{0,1}em>", "").replaceAll("'", "''") + "'");
                }
            }

            if (ss.size() > 0) {
                efvFilter = "and atlas.efv IN (" + StringUtils.join(ss.toArray(), ",") + ") \n";
            }
        }

        // get database atlas results
        atlasDatabaseDAO.getAtlasResults(geneIDs, exptIDs, upOrDown, efvFilter);

        // now enrich database results with data from index
        final List<AtlasResult> arset = new Vector<AtlasResult>();


//        try {
//            atlasQueryRunner.query(atlas_query_topN, new ResultSetHandler() {
//                public List<AtlasResult> handle(ResultSet rs) throws SQLException {
//                    while (rs.next()) {
//                        AtlasResult atlasResult = new AtlasResult();
//
//                        AtlasExperiment expt = null;
//                        AtlasGene gene = null;
//
//                        String experiment_id_key = rs.getString("experiment_id_key");
//                        String gene_id_key = rs.getString("gene_id_key");
//
//                        if (solrExptMap != null && solrExptMap.containsKey(experiment_id_key)) {
//                            expt = new AtlasExperiment(solrExptMap.get(experiment_id_key));
//                            expt.setExperimentHighlights(
//                                    exptHitsResponse.getHighlighting().get(expt.getDwExpAccession()));
//                        } else {
//                            expt = atlasSolrDAO.getExperimentById(experiment_id_key);
//                        }
//
//                        if (solrGeneMap != null && solrGeneMap.containsKey(gene_id_key)) {
//                            gene = new AtlasGene(solrGeneMap.get(gene_id_key));
//                            gene.setGeneHighlights(geneHitsResponse.getHighlighting().get(gene_id_key));
//                        } else {
//                            gene = atlasSolrDAO.getGeneById(gene_id_key).getGene();
//                        }
//
//                        if (expt == null || gene == null) {
//                            log.error("Atlas object retrieval error");
//                        }
//
//                        AtlasTuple atuple = new AtlasTuple(rs.getString("ef"), rs.getString("efv"), rs.getInt("updn"),
//                                rs.getDouble("updn_pvaladj"));
//
//                        //TODO: disabled highlighting in experiments, it's broken on SOLR side, it seems. Example: query for liver.
//                        if (expt != null && expt.getExperimentHighlights() != null) {
//                            List<String> s = expt.getExperimentHighlights().get("exp_factor_values");
//                            if (s != null) {
//                                for (String efv : s) {
//                                    if (atuple.getEfv().equals(efv.replaceAll("</{0,1}em>", ""))) {
//                                        atuple.setEfv(efv);
//                                        break;
//                                    }
//                                }
//                            }
//                        }
//
//                        if ((expt != null && gene != null)
//                                &&
//                                (geneSpeciesFilter == null || geneSpeciesFilter.equals("") ||
//                                        geneSpeciesFilter.equals("any") ||
//                                        geneSpeciesFilter.equalsIgnoreCase(gene.getGeneSpecies()))) {
//                            atlasResult.setExperiment(expt);
//                            atlasResult.setGene(gene);
//                            atlasResult.setAtuple(atuple);
//
//                            arset.add(atlasResult);
//                        }
//                    }
//
//                    return arset;
//                }
//            });
//
//            log.debug("Retrieved query completely: " + arset.size() + " records");
//        }
//        catch (SQLException e) {
//            log.error("Problem querying Atlas database", e);
//        }

        return arset;
    }
}
