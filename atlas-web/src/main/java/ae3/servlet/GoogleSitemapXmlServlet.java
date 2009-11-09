package ae3.servlet;

import ae3.util.AtlasProperties;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.web.Atlas;
import uk.ac.ebi.gxa.web.AtlasSearchService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPOutputStream;


/**
 * Prepares for and allows downloading of Google Sitemap XML files
 */
public class GoogleSitemapXmlServlet extends FileDownloadServlet {
    protected final Logger log = LoggerFactory.getLogger(getClass());


    @Override
    public void init() throws ServletException {
        setBasePath(System.getProperty("java.io.tmpdir"));

        new Thread() {
            public void run() {
                SolrCore core = null;
                try {
                    AtlasSearchService searchService =
                            (AtlasSearchService) getServletContext().getAttribute(Atlas.SEARCH_SERVICE.key());
                    core = searchService.getSolrCore("atlas");
                    writeGeneSitemap(core);
                }
                finally {
                    if (core != null) {
                        core.close();
                    }
                }

                // writeExptSitemap(ArrayExpressSearchService.instance().getSolrCore("expt"));
            }
        }.start();
    }


    @Override
    /**
     * Returns filename where the gene identifiers are dumped to. If the file doesn't exist for some reason,
     * generates the dump.
     *
     */
    protected String getRequestedFilename(HttpServletRequest request) {
        final File geneSitemapIndexFile = new File(getBasePath() + File.separator + "geneSitemapIndex.xml.gz");

        if (!geneSitemapIndexFile.exists()) {
            SolrCore core = null;
            try {
                AtlasSearchService searchService =
                        (AtlasSearchService) getServletContext().getAttribute(Atlas.SEARCH_SERVICE.key());

                core = searchService.getSolrCore("atlas");
                writeGeneSitemap(core);
            }
            finally {
                if (core != null) {
                    core.close();
                }
            }
        }

        final String reqPI = request.getPathInfo();

        log.info("Request for sitemap d/l: " + reqPI);

        return reqPI;
    }

    /**
     * Generates a special file containing all gene identifiers, for external users to use for linking.
     *
     * @param core SolrCore to use
     */
    public void writeGeneSitemap(SolrCore core) {
        try {
            final String geneSitemapIndexFile = getBasePath() + File.separator + "geneSitemapIndex.xml";

            RefCounted<SolrIndexSearcher> searcher = core.getSearcher();
            IndexReader r = searcher.get().getReader();

            log.info("Generating gene sitemap, index in " + geneSitemapIndexFile);
            BufferedOutputStream bfind = new BufferedOutputStream(new FileOutputStream(geneSitemapIndexFile));

            bfind.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n".getBytes("UTF-8"));
            bfind.write("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n".getBytes("UTF-8"));

            List<String> geneids =
                    Arrays.asList(StringUtils.split(AtlasProperties.getProperty("atlas.dump.geneidentifiers"), ','));

            TermEnum terms = r.terms();

            int c = 0;
            GZIPOutputStream gzout = null;
            while (terms.next()) {
                if (0 == c % 50000) {
                    if (null != gzout) {
                        gzout.write("</urlset>".getBytes("UTF-8"));
                        gzout.close();
                        if (c > 0) {
                            bfind.write(
                                    ("<sitemap><loc>http://www.ebi.ac.uk/gxa/sitemap/geneSitemap" + (c / 50000 - 1) +
                                            ".xml.gz</loc></sitemap>\n").getBytes("UTF-8"));
                        }
                    }

                    gzout = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(
                            getBasePath() + File.separator + "geneSitemap" + c / 50000 + ".xml.gz")));
                    gzout.write(
                            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n".getBytes(
                                    "UTF-8"));
                }

                Term t = terms.term();
                String f = t.field();

                if (geneids.contains(f) && gzout != null) {
                    gzout.write(("<url><loc>http://www.ebi.ac.uk/gxa/gene/" + t.text() + "</loc></url>\n").getBytes(
                            "UTF-8"));
                    c++;
                }
            }

            if (gzout != null) {
                gzout.write("</urlset>".getBytes("UTF-8"));
                gzout.close();
                bfind.write(("<sitemap><loc>http://www.ebi.ac.uk/gxa/sitemap/geneSitemap" + (c / 50000) +
                        ".xml.gz</loc></sitemap>\n").getBytes("UTF-8"));
            }

            bfind.write("</sitemapindex>".getBytes("UTF-8"));

            searcher.decref();
            bfind.close();
        }
        catch (IOException e) {
            log.error("Failed to create gene sitemap from index", e);
        }
    }
}
