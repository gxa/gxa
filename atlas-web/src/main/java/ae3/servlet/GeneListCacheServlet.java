package ae3.servlet;

import ae3.service.structuredquery.AtlasStructuredQueryService;
import ae3.service.structuredquery.AutoCompleteItem;
import ae3.service.structuredquery.GeneProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import uk.ac.ebi.gxa.web.Atlas;
import uk.ac.ebi.gxa.web.AtlasSearchService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by IntelliJ IDEA. User: Andrey Date: Jul 21, 2009 Time: 3:35:16 PM To change this template use File |
 * Settings | File Templates.
 */
public class GeneListCacheServlet extends HttpServlet {
    public static final int PAGE_SIZE = 1000;

    public static boolean done = false;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public void init() throws ServletException {
        new Thread() {
            public void run() {

                BufferedOutputStream bfind = null;

                try {

                    bfind = new BufferedOutputStream(new FileOutputStream(getFileName()));

                    String letters = "0abcdefghigklmnopqrstuvwxyz";

                    bfind.write("<r>".getBytes());

                    for (int i = 0; i != letters.length(); i++) {
                        String prefix = String.valueOf(letters.charAt(i));

                        Collection<AutoCompleteItem> Genes = queryIndex(prefix, PAGE_SIZE);

                        if (prefix.equals("0")) {
                            prefix = "num";
                        }

                        for (AutoCompleteItem j : Genes) {
                            String geneName = j.getValue();

                            bfind.write(String.format("<%1$s id=\"%3$s\">%2$s</%1$s>\n", prefix, geneName,
                                                      j.getId()).getBytes());
                        }
                    }

                    bfind.write("</r>".getBytes());
                }
                catch (Exception ex) {
                    log.info("ERROR creating gene names cache:" + ex.getMessage());

                }
                finally {
                    if (null != bfind) {
                        try {
                            bfind.close();
                            done = true;
                        }
                        catch (Exception Ex) {
                            //no op
                        }
                    }
                }
            }
        }.start();

        // add a reference to this servlet to the context
        getServletContext().setAttribute(Atlas.GENES_CACHE.key(), this);
    }

    private static String getFileName() {
        String basePath = System.getProperty("java.io.tmpdir");

        final String geneListFileName = basePath + File.separator + "geneNames.xml";

        return geneListFileName;
    }

    public Collection<AutoCompleteItem> getGenes(String prefix, Integer recordCount) throws Exception {
        if ((!done) | (recordCount > PAGE_SIZE)) {
            return queryIndex(prefix, recordCount);
        }
        else {
            Collection<AutoCompleteItem> result = new ArrayList<AutoCompleteItem>();

            XPath xpath = XPathFactory.newInstance().newXPath();

            if (prefix.equals("0")) {
                prefix = "num";
            }

            String expression = "/r/" + prefix;
            InputSource inputSource = new InputSource(getFileName()); //

            Object nodes1 = xpath.evaluate(expression, inputSource, XPathConstants.NODESET);

            NodeList nodes = (NodeList) nodes1;

            for (int i = 0; i != nodes.getLength(); i++) {
                Node n = nodes.item(i);

                String name = n.getTextContent();
                String id = ((Element) n).getAttribute("id");

                Long count = 1L;

                AutoCompleteItem ai = new AutoCompleteItem(name, id, name, count);

                result.add(ai);
            }

            return result;
        }
    }

    private Collection<AutoCompleteItem> queryIndex(String prefix, Integer recordCount) throws Exception {
        AtlasSearchService searchService =
                (AtlasSearchService) getServletContext().getAttribute(Atlas.SEARCH_SERVICE.key());

        AtlasStructuredQueryService queryService = searchService.getAtlasQueryService();

        Collection<AutoCompleteItem> Genes = queryService.getGeneListHelper()
                .autoCompleteValues(GeneProperties.GENE_PROPERTY_NAME, prefix, recordCount, null);

        //AZ:2008-07-07 "0" means all numbers
        if (prefix.equals("0")) {
            for (int i = 1; i != 10; i++) {
                Genes.addAll((queryService.getGeneListHelper().autoCompleteValues(GeneProperties.GENE_PROPERTY_NAME,
                                                                                  String.valueOf(i), recordCount,
                                                                                  null)));
            }

        }

        ArrayList<AutoCompleteItem> result = new ArrayList<AutoCompleteItem>();

        for (AutoCompleteItem a : Genes) {
            int iPos = Collections.binarySearch(result, a, new Comparator<AutoCompleteItem>() {
                public int compare(AutoCompleteItem t1, AutoCompleteItem t2) {
                    return t1.getValue().compareTo(t2.getValue());
                }
            });

            if (iPos < 0) {
                result.add(-1 * (iPos + 1), a);
            }
        }

        return result;
    }
}

