package ae3.servlet;

import ae3.service.ArrayExpressSearchService;
import ae3.util.AtlasProperties;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Prepares for and allows downloading of wholesale dump of gene identifiers for all genes in Atlas
 */
public class GeneIdentifiersDumpDownloadServlet extends FileDownloadServlet {
    protected final Log log = LogFactory.getLog(getClass());

    @Override
    public void init() throws ServletException {
        basePath = System.getProperty("java.io.tmpdir");
        final String dumpGeneIdsAbsoluteFilename = basePath + File.separator + AtlasProperties.getProperty("atlas.dump.geneidentifiers.filename");

        new Thread() { public void run() { dumpGeneIdentifiers(dumpGeneIdsAbsoluteFilename); } }.start();
    }


    @Override
    /**
     * Returns filename where the gene identifiers are dumped to. If the file doesn't exist for some reason,
     * generates the dump.
     */
    protected String getRequestedFilename(HttpServletRequest request) {
        String dumpGeneIdsFilename         = AtlasProperties.getProperty("atlas.dump.geneidentifiers.filename");
        String dumpGeneIdsAbsoluteFileName = basePath + File.separator + dumpGeneIdsFilename;

        File dumpGeneIdsFile = new File(dumpGeneIdsAbsoluteFileName);

        if (!dumpGeneIdsFile.exists()) {
            dumpGeneIdentifiers(dumpGeneIdsAbsoluteFileName);
        }

        return dumpGeneIdsFilename;
    }

    /**
     * Generates a special file containing all gene identifiers, for external users to use for linking
     *
     * @param dumpGeneIdsFilename name of file to write the gene identifiers to
     */
    private void dumpGeneIdentifiers(final String dumpGeneIdsFilename) {
        try {
            File dumpGeneIdsFile = new File(dumpGeneIdsFilename);

            SolrCore core = ArrayExpressSearchService.instance().getSolrCore("atlas");
            RefCounted<SolrIndexSearcher> searcher = core.getSearcher();
            IndexReader r = searcher.get().getReader();

            log.info("Writing gene ids file from index to " + dumpGeneIdsFilename);

            BufferedWriter out = new BufferedWriter(new FileWriter(dumpGeneIdsFile));

            List<String> geneids = Arrays.asList(StringUtils.split(AtlasProperties.getProperty("atlas.dump.geneidentifiers"), ','));

            TermEnum terms = r.terms();

            while (terms.next()) {
                Term t = terms.term();
                String f = t.field();

                if (geneids.contains(f)) {
                    out.write(t.field() + "\t" + t.text());
                    out.newLine();
                }
            }

            searcher.decref();
            core.close();

            out.close();
        } catch (IOException e) {
            log.error("Failed to dump gene identifiers from index", e);
        }
    }
}
