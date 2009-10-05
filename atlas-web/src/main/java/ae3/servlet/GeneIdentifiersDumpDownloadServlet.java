package ae3.servlet;

import ae3.service.ArrayExpressSearchService;
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Prepares for and allows downloading of wholesale dump of gene identifiers for all genes in Atlas.
 *
 *
 */
public class GeneIdentifiersDumpDownloadServlet extends FileDownloadServlet {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private String dumpGeneIdsFilename;


    public String getDumpGeneIdsFilename() {
        return dumpGeneIdsFilename;
    }

    public void setDumpGeneIdsFilename(String dumpGeneIdsFilename) {
        this.dumpGeneIdsFilename = dumpGeneIdsFilename;
    }

    @Override
    public void init() throws ServletException {
        setBasePath(System.getProperty("java.io.tmpdir"));
        setDumpGeneIdsFilename(AtlasProperties.getProperty("atlas.dump.geneidentifiers.filename"));

        new Thread() { public void run() {
            SolrCore core = null;
            try {
                core = ArrayExpressSearchService.instance().getSolrCore("atlas");
                dumpGeneIdentifiers(core);
            } finally {
                if (core != null)
                    core.close();
            }
        } }.start();
    }


    @Override
    /**
     * Returns filename where the gene identifiers are dumped to. If the file doesn't exist for some reason,
     * generates the dump.
     *
     */
    protected String getRequestedFilename(HttpServletRequest request) {
        String dumpGeneIdsAbsoluteFilename = getBasePath() + File.separator + getDumpGeneIdsFilename();

        File dumpGeneIdsFile = new File(dumpGeneIdsAbsoluteFilename);

        if (!dumpGeneIdsFile.exists()) {
            SolrCore core = null;
            try {
                core = ArrayExpressSearchService.instance().getSolrCore("atlas");
                dumpGeneIdentifiers(core);
            } finally {
                if (core != null)
                    core.close();
            }
        }

	    log.info("Gene identifiers dump download request");

        return getDumpGeneIdsFilename();
    }

    /**
     * Generates a special file containing all gene identifiers, for external users to use for linking.
     *
     * @param core SolrCore to use
     */
    public void dumpGeneIdentifiers(SolrCore core) {
        try {
            String dumpGeneIdsAbsoluteFilename = getBasePath() + File.separator + getDumpGeneIdsFilename();
            File dumpGeneIdsFile = new File(dumpGeneIdsAbsoluteFilename);

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
            out.close();
        } catch (IOException e) {
            log.error("Failed to dump gene identifiers from index", e);
        }
    }
}
