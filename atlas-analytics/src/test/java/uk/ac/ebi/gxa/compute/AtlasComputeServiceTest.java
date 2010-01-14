package uk.ac.ebi.gxa.compute;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kchine.r.RNumeric;
import org.kchine.r.server.RServices;
import uk.ac.ebi.gxa.R.AtlasRFactory;
import uk.ac.ebi.gxa.R.AtlasRFactoryBuilder;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.analytics.compute.ComputeException;
import uk.ac.ebi.gxa.analytics.compute.ComputeTask;

import java.rmi.RemoteException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class AtlasComputeServiceTest {
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
    public void testComputeTask() {
        ComputeTask<RNumeric> task = new ComputeTask<RNumeric>() {

            public RNumeric compute(RServices R) throws RemoteException {
                return (RNumeric) R.getObject("1 + 3");
            }
        };

        try {
            RNumeric i = svc.computeTask(task);
            System.out.println("1 + 3 = " + i.getValue()[0]);
            assertEquals(i.getValue()[0], 4);
        }
        catch (ComputeException e) {
            e.printStackTrace();
            fail();
        }
    }
}
