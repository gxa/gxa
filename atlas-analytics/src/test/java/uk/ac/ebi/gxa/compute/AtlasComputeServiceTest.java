package uk.ac.ebi.gxa.compute;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import org.kchine.r.RNumeric;
import org.kchine.r.server.RServices;
import server.DirectJNI;
import uk.ac.ebi.gxa.R.AtlasRFactory;
import uk.ac.ebi.gxa.R.AtlasRFactoryBuilder;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.analytics.compute.ComputeTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Properties;


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
    public void testReadProperties() {
        try {
            // open a stream to the resource
            InputStream in = getClass().getClassLoader().getResourceAsStream("biocep.properties");

            // create a reader to read in code
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            String rCode = sb.toString();
            System.out.println(rCode);

            InputStream in2 = getClass().getClassLoader().getResourceAsStream("biocep.properties");
            Properties props = new Properties();
            props.load(in2);
            for (Map.Entry element : props.entrySet()) {
                System.out.println(element.getKey() + " = " + element.getValue());
            }

        }
        catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testGetJNI() {
        try {
            DirectJNI.getInstance().getRServices();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("Booo! DirectJNI instance fails");
        }
        catch (Error e) {
            e.printStackTrace();
            fail("Booo! DirectJNI instance fails");
        }
    }

    @Test
    public void testComputeTask() {
        ComputeTask<RNumeric> task = new ComputeTask<RNumeric>() {

            public RNumeric compute(RServices R) throws RemoteException {
                return (RNumeric) R.getObject("1 + 3");
            }
        };

        RNumeric i = svc.computeTask(task);
        System.out.println("1 + 3 = " + i.getValue()[0]);
        assertEquals(i.getValue()[0], 4);
    }

}
