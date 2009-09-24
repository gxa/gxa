package ae3.service.compute;

import ds.server.SimilarityResultSet;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.kchine.r.RDataFrame;
import org.kchine.r.RNumeric;
import org.kchine.r.server.RServices;

import java.rmi.RemoteException;
import java.util.ArrayList;


/**
 * This test depends on being able to access an available pool of workers as configured in 
 * {@link ae3.util.AtlasProperties}
 */
public class AtlasComputeServiceTest {
    private AtlasComputeService svc;

    @Before
    public void setUp() {
        svc = new AtlasComputeService();
    }

    @After
    public void tearDown() {
        svc.shutdown();
    }

    @Test
    public void testComputeTask() {
        ComputeTask<RNumeric> task = new ComputeTask<RNumeric>() {

            public RNumeric compute(RServices R) throws RemoteException {
                RNumeric i = (RNumeric) R.getObject("1 + 3");

                return i;
            }
        };

        RNumeric i = svc.computeTask(task);
        assertEquals(i.getValue()[0], 4);
    }

    @Test
    public void testComputeSimilarityTask() {
        // do a similarity over E-AFMX-5 for an arbitrary design element/array design
        final SimilarityResultSet simRS = new SimilarityResultSet("226010852","153094131","153069949");

        RDataFrame sim = svc.computeTask(new ComputeTask<RDataFrame>() {
            public RDataFrame compute(RServices R) throws RemoteException {
                String callSim = "sim.nc(" + simRS.getTargetDesignElementId() + ",'" + simRS.getSourceNetCDF() + "')";
                return (RDataFrame) R.getObject(callSim);
            }
        });

        if(null != sim) {
            simRS.loadResult(sim);
            ArrayList<String> simGeneIds = simRS.getSimGeneIDs();
            assertEquals(simGeneIds.get(0), "153069988");
        } else {
            fail("Similarity search returned null");
        }
    }
}
