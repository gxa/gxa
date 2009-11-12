package ae3.service;

import junit.framework.TestCase;
import uk.ac.ebi.gxa.web.AtlasSearchService;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 12-Nov-2009
 */
public class TestAtlasPlotter extends TestCase {
    private AtlasPlotter plotter;

    @Override
    protected void setUp() throws Exception {
        plotter = new AtlasPlotter();

        plotter.setAtlasSearchService(new AtlasSearchService() {
            
        });
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
