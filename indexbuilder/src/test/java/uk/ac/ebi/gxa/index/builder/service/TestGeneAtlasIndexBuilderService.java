package uk.ac.ebi.gxa.index.builder.service;

import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.utils.FileUtil;

/**
 * Javadocs go here.
 *
 * @author Junit Generation Plugin for Maven, written by Tony Burdett
 * @date 07-10-2009
 */
public class TestGeneAtlasIndexBuilderService
        extends IndexBuilderServiceTestCase {
    private GeneAtlasIndexBuilderService gaibs;

    public void setUp() throws Exception {
        super.setUp();

        Efo efo = new Efo();
        efo.setIndexFile(FileUtil.createTempDirectory("efoindex"));

        gaibs = new GeneAtlasIndexBuilderService();
        gaibs.setEfo(efo);
        gaibs.setAtlasDAO(getAtlasDAO());
        gaibs.setSolrServer(getAtlasSolrServer());
    }

    public void tearDown() throws Exception {
        super.tearDown();

        gaibs = null;
    }

    public void testCreateIndexDocs() {
        try {
            // create the docs
            gaibs.createIndexDocs(false);
            // commit the results
            gaibs.getSolrServer().commit();

            // todo - now test that all the docs we'd expect were created


        }
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}
