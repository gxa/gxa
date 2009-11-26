package uk.ac.ebi.gxa.R;

import junit.framework.TestCase;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 19-Nov-2009
 */
public class TestAtlasRFactoryBuilder extends TestCase {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Test
    public void testGetLocalRFactory() {
        try {
            AtlasRFactory rFactory = AtlasRFactoryBuilder.getAtlasRFactoryBuilder().buildAtlasRFactory(RType.LOCAL);
            if (!rFactory.validateEnvironment()) {
                // this is a valid result if no $R_HOME set
                if (System.getenv("R_HOME") == null || System.getenv("R_HOME").equals("")) {
                    log.info("No R_HOME set, so environment is not valid: result is correct");
                }
                else {
                    fail("Unable to validate R remote environment");
                }
            }
        }
        catch (InstantiationException e) {
            e.printStackTrace();
            fail();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testGetRemoteRFactory() {
        try {
            AtlasRFactory rFactory = AtlasRFactoryBuilder.getAtlasRFactoryBuilder().buildAtlasRFactory(RType.REMOTE);
            if (!rFactory.validateEnvironment()) {
                // this is a valid result if no $R.remote.host set
                if (System.getenv("R.remote.host") == null || System.getenv("R.remote.host").equals("")) {
                    log.info("No R.remote.host set, so environment is not valid: result is correct");
                }
                else {
                    fail("Unable to validate R remote environment");
                }
            }
        }
        catch (InstantiationException e) {
            e.printStackTrace();
            fail();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testGetBiocepRFactory() {
        try {
            AtlasRFactory rFactory = AtlasRFactoryBuilder.getAtlasRFactoryBuilder().buildAtlasRFactory(RType.BIOCEP);
            if (!rFactory.validateEnvironment()) {
                // this is a valid result if no biocep properties set - we're just checking one here
                if (System.getProperty("pools.dbmode.name") == null) {
                    log.info("No biocep properties set, so environment is not valid: result is correct");
                }
                else {
                    fail("Unable to validate R remote environment");
                }
            }
        }
        catch (InstantiationException e) {
            e.printStackTrace();
            fail();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

}
