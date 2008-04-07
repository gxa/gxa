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

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
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
 * Singleton class for executing searches.
 *
 * Keeps references to Direct SOLR (Gene, Experiment) and to an Oracle DBCP (set up at web app start time).
 *
 * TODO: Add specialized exceptions.
 *
 * EBI Microarray Informatics Team (c) 2007
 */
public class AtlasSearch {
    private static final Log log = LogFactory.getLog(AtlasSearch.class);

    private SolrServer solr_gene;
    private SolrServer solr_expt;
    private DataSource ds;
    MultiCore multiCore;
    private String solrIndexLocation;

    private AtlasSearch() {};

    private static AtlasSearch _instance = null;

    /**
     * Returns the singleton instance.
     *
     * @return Singleton instance of AtlasSearch
     */
    public static AtlasSearch instance() {
      if(null == _instance) {
         _instance = new AtlasSearch();
      }

      return _instance;
    }

    public void initialize() {
        // setSolrExpt(new DirectSolrConnection(exptIndexLocation, exptIndexLocation + "/data", null));
        // setSolrGene(new DirectSolrConnection(geneIndexLocation, geneIndexLocation + "/data", null));

        try {
	    multiCore = new MultiCore(solrIndexLocation, new File(solrIndexLocation, "multicore.xml"));
            solr_gene = new EmbeddedSolrServer(multiCore,"gene");
            solr_expt = new EmbeddedSolrServer(multiCore,"expt");
        } catch (Exception e) {
            log.error(e);
        }
    }

    /**
     * Gives a connection from the pool. Don't forget to close.
     *
     * @return a connection from the pool
     * @throws SQLException
     */
    public Connection getAEConnection() throws SQLException {
        if (ds != null)
            return ds.getConnection();

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
                   q.setRows(500);
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
               q.setHighlightSnippets(500);
//               q.set("hl.mergeContiguous","true");
               QueryResponse queryResponse = solr_gene.query(q);
               return queryResponse;
           }
        } catch (SolrServerException e) {
            log.error(e);
        }

        return null;
   }


    /**
     * Performs a full text SOLR search on genes.
     *
     * @param query query terms, can be a complete Lucene query or just a bit of text
     * @return {@link org.w3c.dom.Document}
     */
//     public Document fullTextSolrQueryGenes(String query) {
//        String res;
//        Document doc = null;
//
//        try {
//            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
//
//            if ( query.split("\\s").length > 500 ) {
//                Pattern pattern = Pattern.compile("(\\p{Alnum}+\\s){500}");
//                Matcher matcher = pattern.matcher(query);
//
//                while (matcher.find()) {
//                    String qi = matcher.group();
//                    res = solr_gene.request("/select?wt=xml&rows=500&q=gene_ids:(" + qi + ")", null);
//
//                    if ( doc == null)
//                        doc = docBuilder.parse(new InputSource(new StringReader(res)));
//                    else {
//                        Element tmp = docBuilder.parse(new InputSource(new StringReader(res))).getDocumentElement();
//                        doc.getDocumentElement().appendChild(doc.importNode(tmp, true));
//                    }
//                }
//            } else {
//                res = solr_gene.request("/select?wt=xml&rows=500&q=" + query + " gene_ids:(" + query + ")", null);
//                doc = docBuilder.parse(new InputSource(new StringReader(res)));
//            }
//        } catch (ParserConfigurationException e) {
//            log.error(e);
//        } catch (IOException e) {
//            log.error(e);
//        } catch (SAXException e) {
//            log.error(e);
//        } catch (Exception e) {
//            log.error(e);
//        }
//
//        return doc;
//    }

    /**
     * Performs a full text SOLR search on experiments.
     *
     * @param query query terms, can be a complete Lucene query or just a bit of text
     * @return {@link org.w3c.dom.Document}
     */
    public QueryResponse fullTextQueryExpts(String query) {
        if (query == null || query.equals(""))
            return null;

        if (query.length()>500)
            query = query.substring(0,500);

        try {
            SolrQuery q = new SolrQuery("exp_factor_value:(" + query + ")");
            q.setHighlight(true);
            q.addHighlightField("exp_factor_value");
            q.setHighlightSnippets(500);
            q.setRows(50);
            return solr_expt.query(q);
        } catch (SolrServerException e) {
            log.error(e);
        }

        return null;
    }

//     public Document fullTextSolrQueryExpts(String query) {
//        String res;
//
//         if (query.length()>500)
//             query = query.substring(0,500);
//
//        query = "exp_description:" + query + "+OR+exp_factors:" + query + "+OR+bs_attribute:" + query + "+OR+exp_accession:" + query;
//
//        try {
//            res = solr_expt.request("/select?wt=xml&rows=50&q=" + query, null);
//
//            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
//            return docBuilder.parse(new InputSource(new StringReader(res)));
//        } catch (ParserConfigurationException e) {
//            log.error(e);
//        } catch (IOException e) {
//            log.error(e);
//        } catch (SAXException e) {
//            log.error(e);
//        } catch (Exception e) {
//            log.error(e);
//        }
//
//        return null;
//    }

    /**
     * Similar to {@link #atlasQuery(String, String)} but only returns the count of results
     *
     * TODO: Exception to check on missing inGeneIds.
     *
     * @param inGeneIds comma-separated list of gene ids to retrieve data for (required)
     * @param inExptIds comma-separated list of experiment ids on which to restrict the genes (optional)
     * @return
     */
    public long getAtlasQueryCount(String inGeneIds, String inExptIds, String gene_expr_filter) {
//        if ( inGeneIds == null || inGeneIds.equals("") )
//            return 0;
//
        String updn_filter = " and updn <> 0)";

        if (gene_expr_filter.equals("up"))
            updn_filter = " and updn = 1)";

        if (gene_expr_filter.equals("down"))
            updn_filter = " and updn = -1)";

        String atlas_count_query = "select count(*) from\n" +
                "(select /*+INDEX(atlas atlas_by_de) INDEX(expt)*/ \n" +
                " atlas.fpvaladj, atlas.ef, atlas.efv, atlas.updn, atlas.DESIGNELEMENT_ID_KEY\n" +
                "from aemart.atlas atlas, aemart.ae2__designelement__main gene, aemart.ae1__experiment__main expt\n" +
                "where atlas.designelement_id_key=gene.designelement_id_key \n" +
                "and expt.experiment_id_key=atlas.experiment_id_key\n" +
                (inGeneIds.length() != 0 ? "and gene.gene_id_key IN ( " + inGeneIds + ") \n" : "" ) +
                (inExptIds.length() != 0 ? "and expt.experiment_accession in (" + inExptIds + ")\n" : "" ) +
                updn_filter;

        log.info(atlas_count_query);

        Connection connection = null;
        long count = 0;

        try {
            connection = getAEConnection();

            try {
                PreparedStatement stm = connection.prepareStatement(atlas_count_query);
                ResultSet rs = stm.executeQuery();
                log.info("Executed query");
                while (rs.next()) {
                    count = rs.getInt(1);
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

        return count;
    }

    /**
     * Executes an atlas query to retrieve expt acc, desc, ef, efv, gene, updn, and p-value for requested gene ids,
     * optionally restricting to a supplied list of experiment ids.
     *
     * Then writes the results to passed in {@link TableWriter}.
     *
     * TODO: Exception to check on missing inGeneIds.
     *
     * @param inGeneIds comma-separated list of gene ids to retrieve data for (required)
     * @param inExptIds comma-separated list of experiment ids on which to restrict the genes (optional)
     * @param tw        instance of {@link output.TableWriter} to write query results to
     */
    public long writeAtlasQuery(String inGeneIds, String inExptIds, QueryResponse exptHitsResponse, QueryResponse geneHitsResponse,
                                String gene_expr_filter, TableWriter tw) throws IOException {
        if(inGeneIds == null) inGeneIds = "";
        if(inExptIds == null) inExptIds = "";

        String updn_filter = " and updn <> 0\n";

        if (gene_expr_filter.equals("up"))
            updn_filter = " and updn = 1\n";

        if (gene_expr_filter.equals("down"))
            updn_filter = " and updn = -1\n";

        String efvFilter = "";

        if (exptHitsResponse != null && inGeneIds.length() == 0 ) {
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

        if (exptHitsResponse != null && exptHitsResponse.getResults().getNumFound() == 0)
            return 0;

        if (geneHitsResponse != null && geneHitsResponse.getResults().getNumFound() == 0)
            return 0;

        String atlas_query_topN = "SELECT * FROM (\n" +
                "    select    \n" +
                "         expt.experiment_id_key,\n" +
                "         expt.experiment_identifier,\n" +
                "         expt.experiment_description, \n" +
                "         nvl(atlas.fpvaladj,999.0) as rank, \n" +
                "         atlas.ef as expfactor, \n" +
                "         gene.gene_id_key, \n" +
                "         gene.GENE_NAME,\n" +
                "         gene.gene_identifier,\n" +
                "         gene.designelement_name,\n" +
                "         atlas.efv,\n" +
                "         atlas.updn,\n" +
                "         atlas.updn_tstat,\n" +
                "         atlas.updn_pvaladj,\n" +
                "         row_number()\n" +
                "         OVER (\n" +
                "          PARTITION BY expt.experiment_id_key \n" +
                "          ORDER BY atlas.updn_pvaladj asc, atlas.ef, gene_name || gene_identifier, updn desc\n" +
                "        ) TopN from aemart.atlas atlas, aemart.ae2__designelement__main gene, aemart.ae1__experiment__main expt\n" +
                "        where atlas.designelement_id_key=gene.designelement_id_key \n" +
                "and atlas.experiment_id_key=expt.experiment_id_key\n" +
                "and gene.gene_identifier is not null \n" +
                (inGeneIds.length() != 0 ? "and gene.gene_id_key IN ( " + inGeneIds + ") \n" : "" ) +
                updn_filter +
                (inExptIds.length() != 0 ? "and expt.experiment_id_key in (" + inExptIds + ")\n" : "" ) +
                efvFilter +
                ")\n" +
                "WHERE TopN <= 20 ORDER by updn_pvaladj asc, expfactor, gene_name || gene_identifier, updn desc";

        log.info(atlas_query_topN);

        Connection connection = null;
        long numrecs = 0;

        try {
            connection = getAEConnection();
            tw.writeHeader(null);

            try {
                PreparedStatement stm = connection.prepareStatement(atlas_query_topN);
                ResultSet rs = stm.executeQuery();
                log.info("Executed query");

                while (rs.next()) {
                    HashMap<String,Object> expt = new HashMap<String,Object>();

                    String expt_id = rs.getString("experiment_id_key");
                    String gene_id = rs.getString("gene_id_key");

                    expt.put("expt_acc", rs.getString("experiment_identifier"));
                    expt.put("expt_desc", rs.getString("experiment_description"));
                    expt.put("rank", rs.getDouble("rank"));
                    expt.put("ef",rs.getString("expfactor"));
                    expt.put("efv", rs.getString("efv"));
                    expt.put("gene_name", rs.getString("gene_name"));
                    expt.put("gene_identifier", rs.getString("gene_identifier"));
                    expt.put("updn", rs.getInt("updn"));
                    expt.put("updn_tstat", rs.getDouble("updn_tstat"));
                    expt.put("updn_pvaladj", rs.getDouble("updn_pvaladj"));

                    if (geneHitsResponse != null) {
                        Map<String, List<String>> hilites = geneHitsResponse.getHighlighting().get(gene_id);

                        Set<String> hls = new HashSet<String>();
                        for (String hlf : hilites.keySet()) {
                            hls.add(hlf + ": " + StringUtils.join(hilites.get(hlf), ";"));
                        }

                        if(hls.size() > 0)
                            expt.put("gene_hls", StringUtils.join(hls,"<br/>"));
                    }

                    if ( exptHitsResponse != null ) {
                        Map<String,Map<String, List<String>>> hl = exptHitsResponse.getHighlighting();
                        List<String> s = hl.get(expt_id).get("exp_factor_value");
                        if (s != null ) {
                            for (String efv : s) {
                                if(expt.get("efv").equals(efv.replaceAll("</{0,1}em>",""))) {
                                    expt.put("efv", efv);
                                    tw.writeRow(expt);
                                    numrecs++;
                                    break;
                                }
                            }
                        } else {
                            tw.writeRow(expt);
                            numrecs++;
                        }
                    } else {
                        tw.writeRow(expt);
                        numrecs++;
                    }
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
            tw.writeFooter();
        }

        return numrecs;
    }

    /**
     * Similar to writeAtlasQuery but doesn't write anything.
     *
     * TODO: Exception to check on missing inGeneIds.
     *
     * @param inGeneIds comma-separated list of gene id's to retrieve data for (required)
     * @param inExptIds comma-separated list of experiment id's on which to restrict the genes (optional)
     * @return a Vector of HashMaps, one HashMap per atlas query result, keys
     *         are 'expt_acc, expt_desc, ef, efv, gene, updn, rank' for experiment accession, description, factor,
     *         factor value, gene name + identifier in parentheses, up/dn label (-1,1) and adjusted p-value.
     */
    public Vector<HashMap<String,Object>> atlasQuery(String inGeneIds, String inExptIds) {
        String atlas_query = "select /*+ INDEX(atlas atlas_by_de) INDEX(expt) */ \n" +
                                "         expt.experiment_accession,\n" +
                                "         expt.experiment_description, \n" +
                                "         nvl(atlas.fpvaladj,999.0) as rank, \n" +
                                "         atlas.ef as expfactor, \n" +
                                "         gene.gene_id_key, \n" +
                                "         gene.GENE_NAME || ' (' || gene.gene_identifier || ')' as gene,\n" +
                                "         gene.designelement_name,\n" +
                                "         atlas.efv,\n" +
                                "         atlas.updn\n" +
                                "from aemart.atlas atlas, aemart.ae2__designelement__main gene, aemart.ae1__experiment__main expt\n" +
                                "where atlas.designelement_id_key=gene.designelement_id_key \n" +
                                "and atlas.experiment_id_key=expt.experiment_id_key\n" +
                                "and gene.gene_id_key IN ( " + inGeneIds + " ) \n" +
                                "and updn <> 0\n" +
                                (inExptIds.length() != 0 ? "and expt.experiment_accession in (" + inExptIds + ")\n" : "" )+
                                "order by rank, expfactor, gene, updn desc, experiment_accession";

        log.info(atlas_query);

        Connection connection = null;
        Vector<HashMap<String,Object>> expts = null;

        try {
            connection = getAEConnection();
            expts = new Vector<HashMap<String,Object>>();

            try {
                PreparedStatement stm = connection.prepareStatement(atlas_query);
                ResultSet rs = stm.executeQuery();

                while (rs.next()) {
                    HashMap<String,Object> expt = new HashMap<String,Object>();
                    expt.put("expt_acc", rs.getString("experiment_accession"));
                    expt.put("expt_desc", rs.getString("experiment_description"));
                    expt.put("rank", rs.getDouble("rank"));
                    expt.put("ef",rs.getString("expfactor"));
                    expt.put("efv", rs.getString("efv"));
                    expt.put("gene", rs.getString("gene"));
                    expt.put("updn", rs.getInt("updn"));
                    expts.add(expt);
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

        return expts;
    }

//    public void setSolrGene(DirectSolrConnection solr_gene) {
//        this.solr_gene = solr_gene;
//    }
//
//    public void setSolrExpt(DirectSolrConnection solr_expt) {
//        this.solr_expt = solr_expt;
//    }
//
    public void setDataSource(DataSource ds) {
        this.ds = ds;
    }

    /**
     * Should be called when app is going down.
     */
    public void shutdown() {
        log.info("Shutting down AtlasSearch.");
        try {
            if (ds != null) ds = null;

            multiCore.shutdown();
            solr_gene = null;
            solr_expt = null;
        } catch (Exception e) {
            log.error("Error shutting down AtlasSearch!", e);
        }
    }

    protected void finalize() throws Throwable {
        shutdown();
    }

    public void setSolrIndexLocation(String solrIndexLocation) {
        this.solrIndexLocation = solrIndexLocation;
    }

    public String getSolrIndexLocation() {
        return solrIndexLocation;       
    }

    public long writeAtlasQuery(QueryResponse geneHitsResponse, QueryResponse exptHitsResponse, String updn_filter, HtmlTableWriter tw) throws IOException {
        if (geneHitsResponse == null && exptHitsResponse == null)
            return 0;

        String inGeneIds = getSqlInClauseFromSolrHits(geneHitsResponse, "gene_id");
        String inExptIds = getSqlInClauseFromSolrHits(exptHitsResponse, "exp_id");

        return writeAtlasQuery(inGeneIds, inExptIds, exptHitsResponse, geneHitsResponse, updn_filter, tw);
    }

    private String getSqlInClauseFromSolrHits (QueryResponse queryResponse, String idField ) {
        if (queryResponse == null)
            return null;

        SolrDocumentList hits = queryResponse.getResults();
        Set<String> idSet = new HashSet<String>();

        for (SolrDocument doc : hits ) {
            String id = (String) doc.getFieldValue(idField);
            if(id != null) idSet.add(id);
        }

        return StringUtils.join(idSet, ",");
    }


}
