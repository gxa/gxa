package ae3.servlet;

import ae3.dao.AtlasDao;
import ae3.model.AtlasGene;
import ae3.service.XML4dbDumps;
import ae3.util.AtlasProperties;
import ae3.util.FileDownloadServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import uk.ac.ebi.gxa.index.builder.IndexBuilderEventHandler;
import uk.ac.ebi.gxa.index.builder.IndexBuilder;
import uk.ac.ebi.gxa.index.builder.listener.IndexBuilderEvent;

/**
 * Prepares for and allows downloading of wholesale dump of gene identifiers for all genes in Atlas.
 */
public class GeneEbeyeDumpDownloadServlet implements HttpRequestHandler, IndexBuilderEventHandler {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private File dumpGeneIdsFile = new File(System.getProperty("java.io.tmpdir") + File.separator + AtlasProperties.getProperty("atlas.dump.ebeye.filename"));
    private AtlasDao dao;

    public File getDumpGeneIdsFile() {
        return dumpGeneIdsFile;
    }

    public void setDumpGeneIdsFile(File dumpGeneIdsFile) {
        this.dumpGeneIdsFile = dumpGeneIdsFile;
    }

    public AtlasDao getDao() {
        return dao;
    }

    public void setDao(AtlasDao dao) {
        this.dao = dao;
    }

    public void setIndexBuilder(IndexBuilder indexBuilder) {
        indexBuilder.registerIndexBuildEventHandler(this);
    }

    public void handleRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        log.info("Gene ebeye dump download request");
        if(!dumpGeneIdsFile.exists())
            dumpGeneIdentifiers();
        FileDownloadServer.processRequest(dumpGeneIdsFile, "text/xml", httpServletRequest, httpServletResponse);
    }

    public void onIndexBuildFinish(IndexBuilder builder, IndexBuilderEvent event) {
        dumpGeneIdsFile.delete();
        dumpGeneIdentifiers();
    }

    public void onIndexBuildStart(IndexBuilder builder) {

    }

    /**
     * Generates a special file containing all gene identifiers, for external users to use for linking.
     */
    public void dumpGeneIdentifiers() {
        try {
            List<AtlasGene> genes = dao.getGenes();

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

            //File dumpGeneIdsFile = new File(dumpGeneIdsAbsoluteFilename);
            //RefCounted<SolrIndexSearcher> searcher = core.getSearcher();
            //IndexReader r = searcher.get().getReader();

            log.info("Writing ebeye file from index to " + dumpGeneIdsFile);

            //BufferedWriter out = new BufferedWriter(new FileWriter(dumpGeneIdsFile));
            //List<String> geneids = Arrays.asList(StringUtils.split(AtlasProperties.getProperty("atlas.dump.geneidentifiers"), ','));
            //ByteArrayOutputStream ostream = new ByteArrayOutputStream();

            FileOutputStream ostream = new FileOutputStream(dumpGeneIdsFile);

            try {
                ostream.write(XML4dbDumps.Serialize(d1).getBytes());
            }
            catch (Exception Ex) {
                log.error("Failed to dump gene identifiers from index", Ex.getMessage());
            }
            finally {
                ostream.close();
                log.info("Writing ebeye file from index to " + dumpGeneIdsFile + " - done");
            }

        }
        catch (IOException e) {
            log.error("Failed to dump gene identifiers from index", e);
        }
    }
}
