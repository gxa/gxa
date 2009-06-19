package ae3.service.compute;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.kchine.r.RNumeric;
import org.kchine.r.server.RServices;

import java.rmi.RemoteException;
import static org.junit.Assert.*;


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
}
