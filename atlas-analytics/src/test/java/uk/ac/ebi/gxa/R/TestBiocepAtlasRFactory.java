package uk.ac.ebi.gxa.R;

import junit.framework.TestCase;
import org.kchine.r.server.RServices;

import java.util.ArrayList;
import java.util.List;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 05-Feb-2010
 */
public class TestBiocepAtlasRFactory extends TestCase {
    private AtlasRFactory rFactory;
    private List<RServices> rServicesList;

    public void setUp() {
        rServicesList = new ArrayList<RServices>();
        try {
            rFactory = AtlasRFactoryBuilder.getAtlasRFactoryBuilder().buildAtlasRFactory(RType.BIOCEP);
        }
        catch (InstantiationException e) {
            e.printStackTrace();
            fail();
        }
    }

    public void tearDown() {
        for (RServices rServices : rServicesList) {
            try {
                rFactory.recycleRServices(rServices);
            }
            catch (AtlasRServicesException e) {
                e.printStackTrace();
            }
        }
        rServicesList.clear();
        rFactory.releaseResources();
    }

    public void testMultipleCreateRServices() {
        // test 8 iterations
        for (int i = 0; i < 8; i++) {
            try {
                rServicesList.add(rFactory.createRServices());
            }
            catch (AtlasRServicesException e) {
                e.printStackTrace();
                fail("Creating the " + i + "-th rServices object threw an exception");
            }
        }
    }
}
