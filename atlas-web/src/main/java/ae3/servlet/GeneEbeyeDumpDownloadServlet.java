package ae3.servlet;

import ae3.model.AtlasGene;
import ae3.service.XML4dbDumps;
import ae3.util.AtlasProperties;
import org.apache.solr.core.SolrCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.web.Atlas;
import uk.ac.ebi.gxa.web.AtlasSearchService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Prepares for and allows downloading of wholesale dump of gene identifiers for all genes in Atlas.
 */
public class GeneEbeyeDumpDownloadServlet extends FileDownloadServlet {
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
        setDumpGeneIdsFilename(AtlasProperties.getProperty("atlas.dump.ebeye.filename"));

        new Thread() {
            public void run() {
                SolrCore core = null;
                try {
                    AtlasSearchService searchService =
                            (AtlasSearchService) getServletContext().getAttribute(Atlas.SEARCH_SERVICE.key());
                    core = searchService.getSolrCore("atlas");
                    dumpGeneIdentifiers();
                }
                finally {
                    if (core != null) {
                        core.close();
                    }
                }
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
        String dumpGeneIdsAbsoluteFilename = getBasePath() + File.separator + getDumpGeneIdsFilename();

        File dumpGeneIdsFile = new File(dumpGeneIdsAbsoluteFilename);

        if (!dumpGeneIdsFile.exists()) {
            SolrCore core = null;
            try {
                AtlasSearchService searchService =
                        (AtlasSearchService) getServletContext().getAttribute(Atlas.SEARCH_SERVICE.key());
                core = searchService.getSolrCore("atlas");
                dumpGeneIdentifiers();
            }
            finally {
                if (core != null) {
                    core.close();
                }
            }
        }

        log.info("Gene identifiers dump download request");

        return getDumpGeneIdsFilename();
    }

    /**
     * Generates a special file containing all gene identifiers, for external users to use for linking.
     */
    public void dumpGeneIdentifiers() {
        try {
            AtlasSearchService searchService =
                    (AtlasSearchService) getServletContext().getAttribute(Atlas.SEARCH_SERVICE.key());
            List<AtlasGene> genes = searchService.getAtlasSolrDAO().getGenes();

            XML4dbDumps.Document d1 = new XML4dbDumps.Document();
            d1.setName("Gene Expression Atlas"); //db_name
            d1.setDescription("Impressive Gene Expression Atlas");
            d1.setRelease("1.0");
            d1.setReleaseDate("29-AUG-2006");

            for (AtlasGene g : genes) {
                XML4dbDumps.Document.Entry e1 = new XML4dbDumps.Document.Entry();
                d1.getEntries().add(e1);

                e1.setId(g.getGeneIdentifier());
                e1.setAccessionNumber(g.getGeneName());
                e1.setName(g.getGeneName());
                e1.setDateCreated(AtlasProperties.getProperty("atlas.data.release"));
                e1.setDateModified(AtlasProperties.getProperty("atlas.data.release"));

                e1.setDescription("");
                e1.setAuthors("");
                e1.setKeywords("");

                XML4dbDumps.Document.Entry.Reference ref1 = new XML4dbDumps.Document.Entry.Reference();
                ref1.setDbName("db2");
                ref1.setDbKey("abc123");
                e1.getReferences().add(ref1);

                XML4dbDumps.Document.Entry.Reference ref2 = new XML4dbDumps.Document.Entry.Reference();
                ref2.setDbName("db3");
                ref2.setDbKey("abcdef");
                e1.getReferences().add(ref2);

                XML4dbDumps.Document.Entry.AdditionalField f1 = new XML4dbDumps.Document.Entry.AdditionalField();
                f1.setName("namefield1");
                f1.setValue("value1");
                e1.getAdditionalFields().add(f1);

                XML4dbDumps.Document.Entry.AdditionalField f2 = new XML4dbDumps.Document.Entry.AdditionalField();
                f2.setName("namefield2");
                f2.setValue("value2");
                e1.getAdditionalFields().add(f2);
            }

            String dumpGeneIdsAbsoluteFilename = getBasePath() + File.separator + getDumpGeneIdsFilename();
            //File dumpGeneIdsFile = new File(dumpGeneIdsAbsoluteFilename);
            //RefCounted<SolrIndexSearcher> searcher = core.getSearcher();
            //IndexReader r = searcher.get().getReader();

            log.info("Writing gene ids file from index to " + dumpGeneIdsFilename);

            //BufferedWriter out = new BufferedWriter(new FileWriter(dumpGeneIdsFile));
            //List<String> geneids = Arrays.asList(StringUtils.split(AtlasProperties.getProperty("atlas.dump.geneidentifiers"), ','));
            //ByteArrayOutputStream ostream = new ByteArrayOutputStream();

            FileOutputStream ostream = new FileOutputStream(dumpGeneIdsAbsoluteFilename);

            try {
                ostream.write(XML4dbDumps.Serialize(d1).getBytes());
            }
            catch (Exception Ex) {
                log.error("Failed to dump gene identifiers from index", Ex.getMessage());
            }
            finally {
                ostream.close();
            }

        }
        catch (IOException e) {
            log.error("Failed to dump gene identifiers from index", e);
        }
    }
}
