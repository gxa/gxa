package uk.ac.ebi.gxa.loader.handler.sdrf;

import junit.framework.TestCase;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.handler.HandlerPool;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.AccessionHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.AssayHandler;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.gxa.loader.handler.idf.AtlasLoadingAccessionHandler;

/**
 * Javadocs go here.
 *
 * @author Junit Generation Plugin for Maven, written by Tony Burdett
 * @date 07-10-2009
 */
public class TestAtlasLoadingAssayHandler extends TestCase {
    private MAGETABInvestigation investigation;
    private AtlasLoadCache cache;

//  private URL parseURL;

    public void setUp() {
        // now, create an investigation
        investigation = new MAGETABInvestigation();
        cache = new AtlasLoadCache();

        AtlasLoadCacheRegistry.getRegistry().register(investigation, cache);

//    parseURL = this.getClass().getClassLoader().getResource(
//        "E-GEOD-3790.idf.txt");

        HandlerPool pool = HandlerPool.getInstance();
        pool.useDefaultHandlers();
        pool.replaceHandlerClass(
                AssayHandler.class,
                AtlasLoadingAssayHandler.class);

        // person affiliation is also dependent on experiments being created, so replace accession handler too
        pool.replaceHandlerClass(
                AccessionHandler.class,
                AtlasLoadingAccessionHandler.class);
    }

    public void tearDown() throws Exception {
        AtlasLoadCacheRegistry.getRegistry().deregister(investigation);
        investigation = null;
        cache = null;
    }

    public void testWriteValues() {
        // don't have any examples to test Assays (MAGETAB 1.1)
    }
}
