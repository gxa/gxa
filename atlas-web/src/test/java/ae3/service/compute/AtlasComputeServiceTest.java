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
import java.util.Properties;


/**
 * This test depends on being able to access an available pool of workers as configured in
 * {@link ae3.util.AtlasProperties}
 */
public class AtlasComputeServiceTest {
    private AtlasComputeService svc;

    @Before
    public void setUp() {
        try {
            svc = new AtlasComputeService();
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
        final SimilarityResultSet simRS = new SimilarityResultSet("226010852", "153094131", "153069949");

        RDataFrame sim = svc.computeTask(new ComputeTask<RDataFrame>() {
            public RDataFrame compute(RServices R) throws RemoteException {
                String callSim = "sim.nc(" + simRS.getTargetDesignElementId() + ",'" + simRS.getSourceNetCDF() + "')";
                return (RDataFrame) R.getObject(callSim);
            }
        });

        if (null != sim) {
            simRS.loadResult(sim);
            ArrayList<String> simGeneIds = simRS.getSimGeneIDs();
            assertEquals(simGeneIds.get(0), "153069988");
        } else {
            fail("Similarity search returned null");
        }
    }

    @Test
    public void testPropertyReading() {
        Properties biocepProps = new Properties();
        biocepProps.setProperty("biocep.naming.mode", "db");
        biocepProps.setProperty("biocep.provider.factory", "org.kchine.rpf.db.ServantsProviderFactoryDB");
        biocepProps.setProperty("biocep.db.type", "oracle");
        biocepProps.setProperty("biocep.db.driver", "oracle.jdbc.OracleDriver");
        biocepProps.setProperty("biocep.db.url", "jdbc:oracle:thin:@apu.ebi.ac.uk:1521:AEDWT");
        biocepProps.setProperty("biocep.db.user", "atlas2");
        biocepProps.setProperty("biocep.db.password", "atlas2");
        biocepProps.setProperty("biocep.defaultpoolname", "ATLAS");
        biocepProps.setProperty("biocep.killused", "false");

        // databaseURL should be something like "jdbc:oracle:thin:@www.myhost.com:1521:MYDATABASE"
        String databaseURL = biocepProps.getProperty("biocep.db.url");

        if (!databaseURL.contains("@")) {
            fail("No '@' found in the database URL - database connection string " +
                    "isn't using JDBC oracle-thin driver?");
        }

        // split the url up on ":" char
        String[] tokens = databaseURL.split(":");

        // host is the token that begins with '@'
        int hostIndex = 0;
        String host = null;
        for (String token : tokens) {
            if (token.startsWith("@")) {
                host = token.replaceFirst("@", "");
                break;
            }
            hostIndex++;
        }
        if (host == null) {
            fail("Could not read host from the database URL");
        }

        // port is the bit immediately after the host (if present - and if not, use 1521)
        String port;
        // last token is database name - so if there are more tokens between host and name, this is the port
        if (tokens.length > hostIndex + 1) {
            port = tokens[hostIndex + 1];
        } else {
            port = "1521";
        }

        // name is the database name - this is the bit at the end
        String name = tokens[tokens.length - 1];

        // customized DB location, parsed from URL string
        System.setProperty(
                "pools.dbmode.host",
                host);
        System.setProperty(
                "pools.dbmode.port",
                port);
        System.setProperty(
                "pools.dbmode.name",
                name);

        // username and password properties, which has to be duplicated in context.xml and in biocep.properties
//            System.setProperty(
//                    "pools.dbmode.user",
//                    biocepProps.getProperty("biocep.db.user"));
//            System.setProperty(
//                    "pools.dbmode.password",
//                    biocepProps.getProperty("biocep.db.password"));
        // fixme: username and password for biocep DWEP schema are hardcoded
        System.setProperty(
                "pools.dbmode.user",
                "DWEP");
        System.setProperty(
                "pools.dbmode.password",
                "DWEP");

        // standard config, probably won't normally change
        System.setProperty(
                "naming.mode",
                biocepProps.getProperty("biocep.naming.mode"));
        System.setProperty(
                "pools.provider.factory",
                biocepProps.getProperty("biocep.provider.factory"));
        System.setProperty(
                "pools.dbmode.type",
                biocepProps.getProperty("biocep.db.type"));
        System.setProperty(
                "pools.dbmode.driver",
                biocepProps.getProperty("biocep.db.driver"));
        System.setProperty(
                "pools.dbmode.defaultpoolname",
                biocepProps.getProperty("biocep.defaultpoolname"));
        System.setProperty(
                "pools.dbmode.killused",
                biocepProps.getProperty("biocep.killused"));
    }
}
