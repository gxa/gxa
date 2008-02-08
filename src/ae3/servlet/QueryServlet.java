package ae3.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.servlet.DirectSolrConnection;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.StringReader;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Vector;

/**
 * User: ostolop
 * Date: 08-Feb-2008
 * <p/>
 * EBI Microarray Informatics Team (c) 2007
 */

public class QueryServlet extends HttpServlet {
    Log log = LogFactory.getLog("ae3");

    public Connection getAEConnection() throws SQLException {
        DataSource ds = (DataSource) getServletContext().getAttribute("aewds");
        if (ds != null)
            return ds.getConnection();

        return null;
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String queryType = request.getParameter("qt");

        if (queryType == null || queryType.equals("ft")) {
            doFullTextSearch(request, response);
        } else if (queryType.equals("p")) {
            // search for p-values
            doPValueSearch(request, response);
        } else if (queryType.equals("a")) {
            // search for atlas values
            doAtlasSearch(request, response);
        } else if (queryType.equals("t")) {
            // top-n genes query
            doTopGenesForExperiment(null, null);
        }
    }

    private void doAtlasSearch(HttpServletRequest request, HttpServletResponse response) {
        String[] inGeneIds = request.getParameterValues("gs");

        StringBuilder sbInGeneIds = new StringBuilder();
        for ( String gene_id : inGeneIds ) {
            sbInGeneIds.append(gene_id);
            if (!gene_id.equals(inGeneIds[inGeneIds.length-1]))
                sbInGeneIds.append(",");
        }

        String rank_query = "select /*+INDEX(atlas atlas_by_de)*/ distinct atlas.experiment_id_key, nvl(atlas.fpvaladj,999.0) as rank, atlas.ef as expfactor, " +
                "gene.gene_id_key, gene.gene_identifier, gene.designelement_name, atlas.efv, atlas.updn " +
                "  from aemart.atlas atlas, aemart.ae2__designelement__main gene " +
                "  where " +
                "  atlas.designelement_id_key=gene.designelement_id_key " +
                "  and gene_id_key IN (" + sbInGeneIds + ")" +
                "  order by atlas.experiment_id_key, rank";

        log.info(rank_query);

        Connection connection = null;
        Vector<String> expts = null;

        try {
            connection = getAEConnection();
            expts = new Vector<String>();

            try {
                PreparedStatement stm = connection.prepareStatement(rank_query);
                ResultSet rs = stm.executeQuery();

                while (rs.next()) {
                    expts.add(rs.getString(1) + ":" + rs.getDouble(2) + ":" + rs.getString(3) + ":" + rs.getString(4) + ":" + rs.getString(7) + ":" + rs.getString(8));
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

        // write it out
        try {
            PrintWriter pw = response.getWriter();

            pw.println("<?xml version='1.0'?>");
            pw.println("<expts>");
            for(String expt : expts ) {
                pw.println("<expt>" + expt + "</expt>");
            }
            pw.println("</expts>");
        } catch (IOException e) {
             log.error(e);
        }
    }

    private void doFullTextSearch(HttpServletRequest request, HttpServletResponse response) {
        try {
            DirectSolrConnection solr_gene = (DirectSolrConnection) getServletContext().getAttribute("solr_gene");
            DirectSolrConnection solr_expt = (DirectSolrConnection) getServletContext().getAttribute("solr_expt");

            String gene_xml = solr_gene.request("/select?wt=xml&q=" + request.getParameter("q"), null);
            String expt_xml = solr_expt.request("/select?wt=xml&q=" + request.getParameter("q"), null);

            String xslt = request.getParameter("xsl");
            if (xslt == null) xslt = "i.xsl";

            Source gene_xmlSource = new StreamSource(new StringReader(gene_xml));
            Source expt_xmlSource = new StreamSource(new StringReader(expt_xml));
            Source xsltSource = new StreamSource(getServletContext().getResourceAsStream(xslt));

            TransformerFactory transFact = TransformerFactory.newInstance();
            Transformer trans = transFact.newTransformer(xsltSource);

            response.setContentType("text/xml");

            response.getWriter().println("<xml>");

            trans.transform(gene_xmlSource, new StreamResult(response.getWriter()));
            trans.transform(expt_xmlSource, new StreamResult(response.getWriter()));

            response.getWriter().println("</xml>");
        } catch (Exception e) {
            log.error("Problem searching SOLR indexes", e);
        }
    }

    public Vector<String> doTopGenesForExperiment(String exptIdentifier, Integer numGenes) {
        String rank_query = "select /*+FIRST_ROWS*/ gene.gene_identifier, " +
                "ranks.percent_rank " +
                "from " +
                "aemart.ae2__designelement_ranks_limma ranks," +
                "aemart.ae1__experiment__main e, " +
                "aemart.ae2__designelement__main gene where " +
                "ranks.designelement_id_key=gene.designelement_id_key " +
                "and gene.gene_identifier is not null " +
                "and e.experiment_identifier = '" + exptIdentifier + "' " +
                "and ranks.EXPERIMENT_ID_KEY=e.experiment_id_key " +
                "and rownum < " + numGenes + " order by percent_rank";

        Connection connection = null;
        Vector<String> genes = null;

        try {
            connection = getAEConnection();
            genes = new Vector<String>();

            try {
                PreparedStatement stm = connection.prepareStatement(rank_query);
                ResultSet rs = stm.executeQuery();

                while (rs.next()) {
                    genes.add(rs.getString(1) + ": " + rs.getDouble(2));
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

        return genes;
    }

    private void doPValueSearch(HttpServletRequest request, HttpServletResponse response) {
        String[] inGeneIds = request.getParameterValues("gs");

        StringBuilder sbInGeneIds = new StringBuilder();
        for ( String gene_id : inGeneIds ) {
            sbInGeneIds.append(gene_id);
            if (!gene_id.equals(inGeneIds[inGeneIds.length-1]))
                sbInGeneIds.append(",");
        }

        String rank_query = "select /*+INDEX(atlas atlas_by_de)*/ distinct atlas.experiment_id_key, nvl(atlas.fpvaladj,999.0) as rank, atlas.ef as expfactor, " +
                "gene.gene_id_key, gene.gene_identifier, gene.designelement_name " +
                "  from aemart.atlas atlas, aemart.ae2__designelement__main gene " +
                "  where " +
                "  atlas.designelement_id_key=gene.designelement_id_key " +
                "  and gene_id_key IN (" + sbInGeneIds + ")" +
                "  order by atlas.experiment_id_key, rank";

        log.info(rank_query);

        Connection connection = null;
        Vector<String> expts = null;

        try {
            connection = getAEConnection();
            expts = new Vector<String>();

            try {
                PreparedStatement stm = connection.prepareStatement(rank_query);
                ResultSet rs = stm.executeQuery();

                while (rs.next()) {
                    expts.add(rs.getString(1) + ":" + rs.getDouble(2) + ":" + rs.getString(3) + ":" + rs.getString(4));
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

        // write it out
        try {
            PrintWriter pw = response.getWriter();

            pw.println("<?xml version='1.0'?>");
            pw.println("<expts>");
            for(String expt : expts ) {
                pw.println("<expt>" + expt + "</expt>");
            }
            pw.println("</expts>");
        } catch (IOException e) {
             log.error(e);
        }
    }

    /**
     * Creates SOLR QueryWebService parameters
     *
     * @param query     : the QueryWebService submitted by the user
     * @param species   : gene species (optional)
     * @param expIds    : list of experiment ids. gene search is restricted to these experiments only. (optional)
     * @param queryType : gene or experiment QueryWebService
     * @return SOLR QueryWebService
     */
    private String prepareLuceneQuery(String query, String species, String expIds, String queryType) {
        String luceneQuery = "";

        if (queryType.equals("gene")) {
            if (query.contains("go:") || query.contains("GO:"))
                luceneQuery += "gene_goterm:(" + query.substring(3) + ")";
            else
                luceneQuery += "gene_ids:(" + query + ") OR (" + query + ") gene_name:(" + query + ")^10 gene_synonym:(" + query + ")^5";
            if (!species.equals("")) {
                luceneQuery = "(" + luceneQuery + ") "; // AND gene_species:\""+reqParams.get("species")+"\"";
            }
            if (!expIds.equals("")) {
                luceneQuery = "(" + luceneQuery + ") AND gene_experiment: (" + expIds + ")";
            }
        } else if (queryType.equals("exp")) {
            luceneQuery = "exp_description:" + query + " OR exp_factors:" + query +
                    " OR bs_attribute:" + query + " OR exp_accession:" + query;
        }
        return luceneQuery;
    }
}
