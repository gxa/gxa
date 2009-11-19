package uk.ac.ebi.gxa.R;

import junit.framework.TestCase;
import org.junit.Test;
import server.DirectJNI;

import java.io.File;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 19-Nov-2009
 */
public class TestAtlasRFactoryBuilder extends TestCase {
//    @Test
//    public void testGetJNI() {
//        try {
//            String r_home = System.getenv("R_HOME");
//            String append = ":" + r_home + File.separator + "bin:" + r_home + File.separator + "lib";
//            System.out.println("java.library.path before = " + System.getProperty("java.library.path"));
//            System.out.println("Appending R path...");
//            System.setProperty("java.library.path", System.getProperty("java.library.path") + append);
//            System.out.println("java.library.path after = " + System.getProperty("java.library.path"));
//
//            DirectJNI.getInstance().getRServices();
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//            fail("Booo! DirectJNI instance fails");
//        }
//    }

    @Test
    public void testGetLocalRFactory() {
        try {
            AtlasRFactory rFactory = AtlasRFactoryBuilder.getAtlasRFactoryBuilder().buildAtlasRFactory(RType.LOCAL);
            if (!rFactory.validateEnvironment()) {
                fail("Unable to validate R local environment");
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
                fail("Unable to validate R remote environment");
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
                fail("Unable to validate R biocep environment");
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
