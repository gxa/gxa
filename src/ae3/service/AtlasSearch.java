package ae3.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.servlet.DirectSolrConnection;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;

import javax.sql.DataSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.HashMap;
import java.util.Vector;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.StringReader;
import java.io.IOException;

import html.TableWriter;

/**
 * User: ostolop
 * Date: 12-Feb-2008
 * <p/>
 * EBI Microarray Informatics Team (c) 2007
 */
public class AtlasSearch {
    private static final Log log = LogFactory.getLog(AtlasSearch.class);
    private DirectSolrConnection solr_gene;
    private DirectSolrConnection solr_expt;
    private DataSource ds;

    private AtlasSearch() {};

    private static AtlasSearch _instance = null;

    public static AtlasSearch instance() {
      if(null == _instance) {
         _instance = new AtlasSearch();
      }

      return _instance;
    }

    public Connection getAEConnection() throws SQLException {
        if (ds != null)
            return ds.getConnection();

        return null;
    }


     public Document fullTextQueryGenes(String query) {
        String res = null;
        try {
            res = solr_gene.request("/select?wt=xml&rows=50&q=" + query, null);

            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            return docBuilder.parse(new InputSource(new StringReader(res)));
        } catch (ParserConfigurationException e) {
            log.error(e);
        } catch (IOException e) {
            log.error(e);
        } catch (SAXException e) {
            log.error(e);
        } catch (Exception e) {
            log.error(e);
        }

        return null;
    }

    public Document fullTextQueryExpts(String query) {
        String res = null;
        query = "exp_description:" + query + "+OR+exp_factors:" + query + "+OR+bs_attribute:" + query + "+OR+exp_accession:" + query;
        try {
            res = solr_expt.request("/select?wt=xml&rows=50&q=" + query, null);

            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            return docBuilder.parse(new InputSource(new StringReader(res)));
        } catch (ParserConfigurationException e) {
            log.error(e);
        } catch (IOException e) {
            log.error(e);
        } catch (SAXException e) {
            log.error(e);
        } catch (Exception e) {
            log.error(e);
        }

        return null;
    }

    public int getAtlasQueryCount(String inGeneIds, String inExptIds) {
        String atlas_count_query = "select count(*) from\n" +
                "(select /*+INDEX(atlas atlas_by_de) INDEX(expt)*/ \n" +
                " atlas.fpvaladj, atlas.ef, atlas.efv, atlas.updn, atlas.DESIGNELEMENT_ID_KEY\n" +
                "from aemart.atlas atlas, aemart.ae2__designelement__main gene\n" +
                "where atlas.designelement_id_key=gene.designelement_id_key \n" +
                "and gene.gene_id_key IN ( " + inGeneIds + ") \n" +
                (inExptIds.length() != 0 ? "and atlas.experiment_id_key in (" + inExptIds + ")\n" : "" )+
                "and updn <> 0)";

        log.info(atlas_count_query);

        Connection connection = null;
        int count = -1;

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

    public void writeAtlasQuery(String inGeneIds, String inExptIds, TableWriter tw) throws IOException {
        String atlas_query = "select /*+INDEX(atlas atlas_by_de) INDEX(expt)*/ \n" +
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
                                (inExptIds.length() != 0 ? "and atlas.experiment_id_key in (" + inExptIds + ")\n" : "" )+
                                "order by rank, expfactor, updn desc, experiment_accession";

        log.info(atlas_query);

        Connection connection = null;

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

    }


    public Vector<HashMap<String,Object>> atlasQuery(String inGeneIds, String inExptIds) {
        String atlas_query = "select /*+INDEX(atlas atlas_by_de)*/ \n" +
                "distinct expt.experiment_accession,\n" +
                "expt.experiment_description, \n" +
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
                (inExptIds.length() != 0 ? "and atlas.experiment_id_key in (" + inExptIds + ")\n" : "" )+
//                "and rownum<1001\n" +
                "order by rank, expfactor, updn desc, experiment_accession";

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

    public void setSolrGene(DirectSolrConnection solr_gene) {
        this.solr_gene = solr_gene;
    }

    public void setSolrExpt(DirectSolrConnection solr_expt) {
        this.solr_expt = solr_expt;
    }

    public void setDataSource(DataSource ds) {
        this.ds = ds;
    }

    public void shutdown() {
        try {
            if (solr_gene != null)
                solr_gene.close();

            if (solr_expt != null)
                solr_expt.close();

            if (ds != null) {
                ds = null;
            }
        } catch (Exception e) {
            log.error("Error shutting down AtlasSearch!", e);
        }
    }
}
