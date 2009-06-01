package ds.biocep;

import org.kchine.r.server.RServices;
import org.kchine.r.RDataFrame;
import org.kchine.r.RList;
import org.kchine.r.RObject;
import org.kchine.rpf.ServantProviderFactory;
import org.kchine.rpf.TimeoutException;

import ds.server.SimilarityResultSet;

//import ae3.model.SimResult;

import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.io.*;

public class Similarity {
	public static boolean getSimilarDEs(SimilarityResultSet simRS) throws IOException, NotBoundException, TimeoutException{
		RServices R;

        setSystemProperty("naming.mode",                    "db");
        setSystemProperty("pools.dbmode.host",              "bar.ebi.ac.uk" );
        setSystemProperty("pools.provider.factory",         "org.kchine.rpf.db.ServantsProviderFactoryDB");
        setSystemProperty("pools.dbmode.driver",            "org.apache.derby.jdbc.ClientDriver");
        setSystemProperty("pools.dbmode.user",              "DWEP"          );
        setSystemProperty("pools.dbmode.password",          "DWEP"          );
        setSystemProperty("pools.dbmode.defaultpoolname",   "R"             );
        setSystemProperty("pools.dbmode.killused",          "false"         );
        setSystemProperty("node.manager.name",              "NODE_MANAGER"  );
        setSystemProperty("private.servant.node.name",      "N1"            );
        setSystemProperty("server.name",                    "RSERVANT_1"    );


        String simSrc = getRCodeFromResource("/sim.R");
        boolean success=true;

        ServantProviderFactory spFactory = ServantProviderFactory.getFactory();
//        R = (RServices) spFactory.getServantProvider().borrowServantProxyNoWait();
        R = (RServices) spFactory.getServantProvider().getRegistry().lookup(System.getProperty("server.name"));

        if(null == R) {
            System.out.println("No R servant available, bailing out.");
            System.exit(-1);
        }

        R.sourceFromBuffer(simSrc);
        String callSim = "sim.nc("+simRS.getTargetDesignElementId()+",'"+simRS.getSourceNetCDF()+"')";
        RDataFrame sim = (RDataFrame) R.getObject(callSim);
//        "sim.nc(160594082, '/ebi/ArrayExpress-files/NetCDFs.DWDEV/378790366_160588088.nc')"
//        ArrayList<SimResult> simResults
        if(sim != null){
        	success = simRS.loadResult(sim);
//        	RList d = sim.getData();
//            String[] names = d.getNames();
//            RObject[] values = d.getValue();
//            for (int i = 0; i < values.length; i++) {
//                RObject value = values[i];
//                String   name = names[i];
//
//                System.out.println(name);
//                System.out.println(value);
//            }
        }else
        	success = false;
        
        

        spFactory.getServantProvider().returnServantProxy(R);
        return success;
	}
	
	private static String getRCodeFromResource(final String res) throws IOException {
        final char[] buffer = new char[0x10000];
        StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(Similarity.class.getResourceAsStream(res), "UTF-8");
        int read;
        do {
          read = in.read(buffer, 0, buffer.length);
          if (read>0) {
            out.append(buffer, 0, read);
          }
        } while (read>=0);
        
        return out.toString();
    }

    private static void setSystemProperty(String name, String value)
    {
        String val = System.getProperty(name);

        if (val == null || val.equals(""))
        {
            System.setProperty(name, value);
        }
    }

}
