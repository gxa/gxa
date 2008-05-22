package ae3.service;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.core.MultiCore;
import output.HtmlTableWriter;
import output.TableWriter;
import uk.ac.ebi.ae3.indexbuilder.Constants;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import ae3.model.AtlasTuple;
import ae3.dao.AtlasDao;
import ae3.dao.AtlasObjectNotFoundException;

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
    private static final Log log = LogFactory.getLog(ArrayExpressSearchService.class);

    //// Full-text Search Index Instances

    private MultiCore  multiCore;
    private SolrServer solr_gene;
    private SolrServer solr_expt;
    private String solrIndexLocation;

    //// RDBMS Sources

    // ArrayExpress (AEW/Atlas) RDBMS DataSource
    private DataSource theAEDS;

    // In-memory (local ArrayExpress AEW/Atlas helper) RDBMS Datasource
    private DataSource memAEDS;


    //// DataServer Instance(s)

    // private ArrayExpresssDataServer aeds;


    //// AtlasResultSet Cache
    // TODO: Refactor to an external cache (e.g., ehcache) for caching AtlasResultSets, evicting LRU ones.
    //       When an element is removed from the cache, it needs to get removed from the in-memory DB as
    //       well (see the remove method on AtlasResultSetCache).
    private static AtlasResultSetCache arsCache = new AtlasResultSetCache();

    private ArrayExpressSearchService() {};
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
        Connection conn = null;

        try {
	        multiCore = new MultiCore(solrIndexLocation, new File(solrIndexLocation, "multicore.xml"));
            solr_gene = new EmbeddedSolrServer(multiCore,"gene");
            solr_expt = new EmbeddedSolrServer(multiCore,"expt");

            conn = getMEMConnection();
            PreparedStatement stmt = conn.prepareStatement(
                    "CREATE TABLE atlas (" +
                            "idkey uuid, " +
                            "experiment_id int, " +
                            "experiment_accession varchar(255), " +
                            "experiment_description varchar(300), " +
                            "gene_id int, " +
                            "gene_name varchar(255), " +
                            "gene_identifier varchar(255), " +
                            "gene_species varchar(255)," +
                            "ef varchar(255), " +
                            "efv varchar(255), " +
                            "updn int, " +
                            "updn_pvaladj double," +
                            "gene_highlights varchar(4000) )"
            );            
            stmt.execute();
            stmt.close();
        } catch (Exception e) {
            log.error(e);
        } finally {
            if (conn != null) try {
                conn.close();
            } catch (SQLException e) {}
        }
    }

    /**
     * Should be called when app is going down.
     */
    public void shutdown() {
        log.info("Shutting down ArrayExpressSearchService.");
        try {
            if (theAEDS != null) theAEDS = null;
            if (memAEDS != null) memAEDS = null;
            if (multiCore != null)
            {
            	multiCore.shutdown();
            	solr_gene = null;
            	solr_expt = null;
            }
        } catch (Exception e) {
            log.error("Error shutting down ArrayExpressSearchService!", e);
        }
    }


    /**
     * Gives a connection from the pool. Don't forget to close.
     *
     * @return a connection from the pool
     * @throws SQLException
     */
    public Connection getAEConnection() throws SQLException {
        if (theAEDS != null)
            return theAEDS.getConnection();

        return null;
    }

    /**
     * Gives a connection from the pool. Don't forget to close.
     *
     * @return a connection from the pool
     * @throws SQLException
     */
    public Connection getMEMConnection() throws SQLException {
        if (memAEDS != null)
            return memAEDS.getConnection();

        return null;
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
                   String qi = matcher.group();
                   SolrQuery q = new SolrQuery(query);
                   q.setRows(100);
                   return solr_gene.query(q);
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
               QueryResponse queryResponse = solr_gene.query(q);
               return queryResponse;
           }
        } catch (SolrServerException e) {
            log.error(e);
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

        if (query.length()>500)
            query = query.substring(0,500);

        try {
        	
       		SolrQuery q = new SolrQuery(query);          
            q.setHighlight(true);
            q.addHighlightField(Constants.FIELD_AER_FV_OE);
            q.setHighlightSnippets(500);
            q.setRows(rows);
            q.setStart(start);
//            q.setFilterQueries();
            return solr_expt.query(q);
        } catch (SolrServerException e) {
            log.error(e);
        }

        return null;
    	
    }
    
    /**
     * Returns number of documents which the query find.
     * @param query - the lucene query
     * @return
     */
    public long getNumDoc(String query) throws SolrServerException
    {
    	SolrDocumentList l=getNumDoc(query, false, false);
    	return l.getNumFound();
    }
    
    public SolrDocumentList getNumDoc(String query, boolean countSample, boolean countFactor) throws SolrServerException
    {
  	
        SolrQuery q = new SolrQuery(query);
        if (countFactor | countSample)
            q.setFacet(true);
        if (countSample)
        	q.setFields(Constants.FIELD_AER_SAAT_CAT);
        if (countFactor)
        	q.setFields(Constants.FIELD_AER_FV_FACTORNAME);        
        q.setRows(1);
        q.setStart(0);
        QueryResponse queryResponse = solr_expt.query(q);
        SolrDocumentList l=queryResponse.getResults();
        return l;
    }
        
    /**
     * 
     */

    /**
     * Executes an atlas query to retrieve expt acc, desc, ef, efv, gene, updn, and p-value for requested gene ids,
     * optionally restricting to a supplied list of experiment ids.
     */
    public AtlasResultSet doAtlasQuery(QueryResponse geneHitsResponse, QueryResponse exptHitsResponse, String geneExprFilter, String geneSpeciesFilter) throws IOException {
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

//        String gene_species_filter = "";
//        if (geneSpeciesFilter != null)
//            gene_species_filter = " and UPPER(ad_species.value)='" + geneSpeciesFilter + "'";

        Map<String, SolrDocument> solrExptMap = convertSolrDocumentListToMap(exptHitsResponse, "exp_id");
        Map<String, SolrDocument> solrGeneMap = convertSolrDocumentListToMap(geneHitsResponse, "gene_id");

        String inGeneIds = (solrGeneMap == null ? "" : StringUtils.join(solrGeneMap.keySet(), ","));
        String inExptIds = (solrExptMap == null ? "" : StringUtils.join(solrExptMap.keySet(), ","));

        String efvFilter = "";

        if (exptHitsResponse != null /*&& inGeneIds.length() == 0*/ ) {
            Set ss = new HashSet<String>();
            Map<String,Map<String, List<String>>> hl = exptHitsResponse.getHighlighting();

            for ( Map<String, List<String>> vals : hl.values() ) {
                if (vals == null || vals.size() == 0) continue;
                for(String s : vals.get("exp_factor_value")) {
                    ss.add("'" + s.replaceAll("</{0,1}em>","").replaceAll("'", "''") + "'");
                }
            }

            if (ss.size() > 0)
                efvFilter = "and atlas.efv IN (" + StringUtils.join(ss.toArray(), ",") + ") \n";
        }


        String arsCacheKey = inGeneIds + inExptIds +  efvFilter + updn_filter + geneSpeciesFilter;
        if (arsCache.containsKey(arsCacheKey))
            return arsCache.get(arsCacheKey);

        String atlas_query_topN = "SELECT * FROM (\n" +
                " SELECT\n" +
                "         atlas.experiment_id_key,\n" +
//                "         expt.experiment_identifier,\n" +
//                "         expt.experiment_description, \n" +
                "         atlas.gene_id_key, \n" +
//                "         gene.gene_name,\n" +
//                "         gene.gene_identifier,\n" +
//                "         gene.designelement_name,\n" +
//                "         gene_species.value as species,\n" +
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
//                "         min(UPDN_PVALADJ) over (partition by efv) as min_updn_pvaladj_PER_EFV,\n" +
//                "         count(case when updn = -1 then null else 1 end) over (partition by efv) as countup_PER_EFV,\n" +
//                "         count(case when updn =  1 then null else 1 end) over (partition by efv) as countdn_PER_EFV,\n" +
//                "         min(UPDN_PVALADJ) over (partition by atlas.GENE_ID_KEY) as min_updn_pvaladj_PER_GENE,\n" +
//                "         count(case when updn = -1 then null else 1 end) over (partition by atlas.gene_id_key) as countup_PER_GENE,\n" +
//                "         count(case when updn =  1 then null else 1 end) over (partition by atlas.gene_id_key) as countdn_PER_GENE\n" +
                "        from aemart.atlas atlas \n" + //, aemart.ae2__designelement__main gene , aemart.ae1__experiment__main expt, aemart.AE2__GENE_SPECIES__DM gene_species \n" +
                "        where 1 = 1" + // atlas.designelement_id_key=gene.designelement_id_key \n" +
//                " and atlas.experiment_id_key=expt.experiment_id_key\n" +
//                " and gene.gene_identifier is not null\n" +
                " and atlas.experiment_id_key NOT IN (211794549,215315583,384555530) \n" + // ignore E-TABM-145a,b,c
//                " and ad_species.arraydesign_id_key=atlas.arraydesign_id_key\n" +
//                " and gene_species.gene_id_key=gene.gene_id_key \n" +
                (inGeneIds.length() != 0 ? "and atlas.gene_id_key       IN (" + inGeneIds + ") \n" : "" ) +
                (inExptIds.length() != 0 ? "and atlas.experiment_id_key IN (" + inExptIds + ") \n" : "" ) +
                updn_filter +
                efvFilter   +
//                gene_species_filter +
                ")\n" +
                " WHERE \n" +
                " TopN <= 20 and gene_id_key is not null\n" +
                " ORDER by updn_pvaladj asc, updn_tstat desc";

        log.info(atlas_query_topN);

        Connection connection = null;
        AtlasResultSet ars    = null;

        try {
            connection = getAEConnection();

            try {
                PreparedStatement stm = connection.prepareStatement(atlas_query_topN);

                ResultSet rs = stm.executeQuery();
                log.info("Executed query");

                ars = new AtlasResultSet();
                int recs = 0;

                while (rs.next()) {
                    AtlasResult atlasResult = new AtlasResult();

                    AtlasExperiment expt = null;
                    AtlasGene gene       = null;

                    String experiment_id_key = rs.getString("experiment_id_key");
                    String gene_id_key = rs.getString("gene_id_key");

                    try {
                        if (solrExptMap != null && solrExptMap.containsKey(experiment_id_key))
                            expt = AtlasDao.getExperimentByIdDw(solrExptMap.get(experiment_id_key), exptHitsResponse);
                        else
                            expt = AtlasDao.getExperimentByIdDw(experiment_id_key);


                        if (solrGeneMap != null && solrGeneMap.containsKey(gene_id_key))
                            gene = AtlasDao.getGene(solrGeneMap.get(gene_id_key), geneHitsResponse);
                        else 
                            gene = AtlasDao.getGene(gene_id_key);
                    } catch (AtlasObjectNotFoundException e) {
                        log.error(e);
                    }

                    AtlasTuple atuple = new AtlasTuple(rs.getString("ef"), rs.getString("efv"), rs.getInt("updn"), rs.getDouble("updn_pvaladj"));

                    if ( expt.getExperimentHighlights() != null) {
                        List<String> s = expt.getExperimentHighlights().get("exp_factor_value");
                        if (s != null ) {
                            for (String efv : s) {
                                if(atuple.getEfv().equals(efv.replaceAll("</{0,1}em>",""))) {
                                    atuple.setEfv(efv);
                                    break;
                                }
                            }
                        }
                    }

                    atlasResult.setExperiment(expt);
                    atlasResult.setGene(gene);
                    atlasResult.setAtuple(atuple);

//
//                    if (geneHitsResponse != null) {
//                        Map<String, List<String>> hilites = geneHitsResponse.getHighlighting().get(atlasResult.getGene().getGeneId());
//
//                        Set<String> hls = new HashSet<String>();
//                        for (String hlf : hilites.keySet()) {
//                            hls.add(hlf + ": " + StringUtils.join(hilites.get(hlf), ";"));
//                        }
//
//                        if(hls.size() > 0)
//                            atlasResult.getGene().setGeneHighlights(StringUtils.join(hls,"<br/>"));
//                    }
//

                    if(geneSpeciesFilter == null || geneSpeciesFilter.equals("any") || geneSpeciesFilter.equalsIgnoreCase(gene.getGeneSpecies())) {
                        ars.add(atlasResult);
                        recs++;
                    }
                }

                rs.close();
                stm.close();

                log.info("Retrieved query completely: " + recs + " records" );

                ars.setAvailableInDB(true);
                arsCache.put(arsCacheKey, ars);
            } catch (SQLException e) {
                log.error("SQL Error!", e);
            }
        } catch (SQLException e) {
            log.error("Couldn't get connection", e);
        } finally {
            if (connection != null) try {
                connection.close();
            } catch (Exception e) {
            }
        }

        return ars;
    }

    public void setAEDataSource(DataSource aeds) {
        this.theAEDS = aeds;
    }

    public void setMEMDataSource(DataSource memds) {
        this.memAEDS = memds;
    }

    public void setSolrIndexLocation(String solrIndexLocation) {
        this.solrIndexLocation = solrIndexLocation;
    }

//    public long writeAtlasQuery(QueryResponse geneHitsResponse, QueryResponse exptHitsResponse, String updn_filter, HtmlTableWriter tw) throws IOException {
//        if (geneHitsResponse == null && exptHitsResponse == null)
//            return 0;
//
//        String inGeneIds = getSqlInClauseFromSolrHits(geneHitsResponse, "gene_id");
//        String inExptIds = getSqlInClauseFromSolrHits(exptHitsResponse, "exp_id");
//
//        return writeAtlasQuery(inGeneIds, inExptIds, exptHitsResponse, geneHitsResponse, updn_filter, tw);
//    }


    private static Map<String, SolrDocument> convertSolrDocumentListToMap (QueryResponse queryResponse, String idField ) {
        if (queryResponse == null)
            return null;

        SolrDocumentList hits = queryResponse.getResults();
        Map<String, SolrDocument> idMap = new HashMap<String, SolrDocument>();

        for (SolrDocument doc : hits) {
            String id = (String) doc.getFieldValue(idField);
            if(id != null) idMap.put(id, doc);
        }

        return idMap;
    }

    protected void finalize() throws Throwable {
        shutdown();
    }

    public SortedSet<String> getAllAvailableAtlasSpecies() {
        Connection connection = null;
        SortedSet<String> species = new TreeSet<String>();

        try {
            connection = getAEConnection();

            try {
                PreparedStatement stm = connection.prepareStatement("SELECT DISTINCT value FROM ae2__gene_species__dm WHERE value IS NOT NULL AND NOT (value LIKE 'UNK%')");

                ResultSet rs = stm.executeQuery();
                log.info("Executed query");

                while (rs.next()) {
                    String s = rs.getString(1).toLowerCase();
                    species.add(s.substring(0,1).toUpperCase() + s.substring(1));
                }

                rs.close();
                stm.close();
            } catch (SQLException e) {
                log.error("SQL Error!", e);
            }
        } catch (SQLException e) {
            log.error("Couldn't get connection", e);
        } finally {
            if (connection != null) try {
                connection.close();
            } catch (Exception e) {
            }
        }

        return species;
    }
}