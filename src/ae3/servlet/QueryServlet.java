package ae3.servlet;

import ae3.service.ArrayExpressSearchService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import output.XmlTableWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.sql.Connection;
import java.util.Vector;

/**
 * User: ostolop
 * Date: 08-Feb-2008
 * <p/>
 * EBI Microarray Informatics Team (c) 2007
 */

public class QueryServlet extends HttpServlet {
    protected final Log log = LogFactory.getLog(getClass());

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

        // write it out
        try {
            XmlTableWriter xtw = new XmlTableWriter(response.getWriter());

//            ArrayExpressSearchService.instance().writeAtlasQuery(sbInGeneIds.toString(), "", null, null, "", xtw);
        } catch (IOException e) {
             log.error(e);
        }
    }


    private void doFullTextSearch(HttpServletRequest request, HttpServletResponse response) {
        try {
//            Hits gene_doc = ArrayExpressSearchService.instance().fullTextQueryGenes(request.getParameter("q"));
//            Hits expt_doc = ArrayExpressSearchService.instance().fullTextQueryExpts(request.getParameter("q"));

            String xslt = request.getParameter("xsl");
            if (xslt == null) xslt = "i.xsl";

//            Source gene_xmlSource = new DOMSource(gene_doc);
//            Source expt_xmlSource = new DOMSource(expt_doc);
            Source xsltSource = new StreamSource(getServletContext().getResourceAsStream(xslt));

            TransformerFactory transFact = TransformerFactory.newInstance();
            Transformer trans = transFact.newTransformer(xsltSource);

            response.setContentType("text/xml");

            response.getWriter().println("<xml>");

//            trans.transform(gene_xmlSource, new StreamResult(response.getWriter()));
//            trans.transform(expt_xmlSource, new StreamResult(response.getWriter()));

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
