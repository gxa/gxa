package ae3.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.servlet.DirectSolrConnection;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.core.MultiCore;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Hits;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.StringReader;
import java.io.IOException;
import java.io.File;

import output.TableWriter;

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
            MultiCore.getRegistry().load(solrIndexLocation, new File(solrIndexLocation, "multicore.xml"));
            solr_gene = new EmbeddedSolrServer("gene");
            solr_expt = new EmbeddedSolrServer("expt");
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
        try {
           // for now just search on the first 500 query terms
           if ( query.split("\\s").length > 500 ) {
               Pattern pattern = Pattern.compile("(\\p{Alnum}+\\s){500}");
               Matcher matcher = pattern.matcher(query);

               if(matcher.find()) {
                   String qi = matcher.group();
                   SolrQuery q = new SolrQuery(query);
                   q.setRows(30);
                   return solr_gene.query(q);
               }
           } else {
               SolrQuery q = new SolrQuery(query);
               q.setRows(30);
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
            SolrQuery q = new SolrQuery(query);
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
    public long getAtlasQueryCount(String inGeneIds, String inExptIds) {
        if ( inGeneIds == null || inGeneIds.equals("") ) 
            return 0;

        String atlas_count_query = "select count(*) from\n" +
                "(select /*+INDEX(atlas atlas_by_de) INDEX(expt)*/ \n" +
                " atlas.fpvaladj, atlas.ef, atlas.efv, atlas.updn, atlas.DESIGNELEMENT_ID_KEY\n" +
                "from aemart.atlas atlas, aemart.ae2__designelement__main gene, aemart.ae1__experiment__main expt\n" +
                "where atlas.designelement_id_key=gene.designelement_id_key \n" +
                "and expt.experiment_id_key=atlas.experiment_id_key\n" +
                "and gene.gene_id_key IN ( " + inGeneIds + ") \n" +
                (inExptIds.length() != 0 ? "and expt.experiment_accession in (" + inExptIds + ")\n" : "" )+
                "and updn <> 0)";

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
    public long writeAtlasQuery(String inGeneIds, String inExptIds, TableWriter tw) throws IOException {
    	
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
        long numrecs = 0;

        try {
            connection = getAEConnection();
            tw.writeHeader();

            try {

                PreparedStatement stm = connection.prepareStatement(atlas_query);
                ResultSet rs = stm.executeQuery();
                log.info("Executed query");
                while (rs.next()) {
                    HashMap<String,Object> expt = new HashMap<String,Object>();
                    expt.put("expt_acc", rs.getString("experiment_accession"));
                    expt.put("expt_desc", rs.getString("experiment_description"));
                    expt.put("rank", rs.getDouble("rank"));
                    expt.put("ef",rs.getString("expfactor"));
                    expt.put("efv", rs.getString("efv"));
                    expt.put("gene", rs.getString("gene"));
                    expt.put("updn", rs.getInt("updn"));

                    tw.writeRow(expt);
                    numrecs++;
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
     * Similar to {@link #writeAtlasQuery(String, String, output.TableWriter)} but doesn't write anything.
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

            MultiCore.getRegistry().shutdown();
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
}
