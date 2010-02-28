package uk.ac.ebi.gxa.requesthandlers.experimentpage;

import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kchine.r.RDataFrame;
import org.kchine.r.server.RServices;
import uk.ac.ebi.gxa.R.AtlasRFactory;
import uk.ac.ebi.gxa.R.AtlasRFactoryBuilder;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.analytics.compute.ComputeException;
import uk.ac.ebi.gxa.analytics.compute.ComputeTask;
import uk.ac.ebi.gxa.requesthandlers.experimentpage.result.SimilarityResultSet;

import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 17-Nov-2009
 */
public class SimilarGeneListTest extends TestCase {
    private AtlasComputeService svc;

    @Before
    public void setUp() {
        try {
            // build default rFactory - reads R.properties from classpath
            AtlasRFactory rFactory = AtlasRFactoryBuilder.getAtlasRFactoryBuilder().buildAtlasRFactory();
            // build service
            svc = new AtlasComputeService();
            svc.setAtlasRFactory(rFactory);
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("Caught exception whilst setting up");
        }
    }

    @After
    public void tearDown() {
        svc.shutdown();
    }

    @Test
    public void testComputeSimilarityTask() {
        // do a similarity over E-AFMX-5 for an arbitrary design element/array design
        final SimilarityResultSet simRS = new SimilarityResultSet("226010852", "153094131", "153069949");
        final String callSim = "sim.nc(" + simRS.getTargetDesignElementId() + ",'" + simRS.getSourceNetCDF() + "')";

        RDataFrame sim = null;
        try {
            sim = svc.computeTask(new ComputeTask<RDataFrame>() {
                public RDataFrame compute(RServices R) throws RemoteException {
                    R.sourceFromResource("sim.R");
                    return (RDataFrame) R.getObject(callSim);
                }
            });
        }
        catch (ComputeException e) {
            fail("Failed calling: " + callSim + "\n" + e.getMessage());
            e.printStackTrace();
        }

        if (null != sim) {
            simRS.loadResult(sim);
            ArrayList<String> simGeneIds = simRS.getSimGeneIDs();
            assertEquals(simGeneIds.get(0), "153069988");
        }
        else {
            fail("Similarity search returned null");
        }
    }
}
