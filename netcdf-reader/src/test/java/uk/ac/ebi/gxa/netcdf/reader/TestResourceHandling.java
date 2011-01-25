package uk.ac.ebi.gxa.netcdf.reader;

import com.google.common.io.Closeables;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;

public class TestResourceHandling {
    @Test
    public void testOpenClose() throws IOException, URISyntaxException {
        File netCDFfile = new File(getClass().getClassLoader().getResource("MEXP/1500/E-MEXP-1586/1036804667_160588088.nc").toURI());
        for (int i = 0; i < 20000; i++) {
            NetCDFProxy netCDF = null;
            try {
                netCDF = new NetCDFProxy(netCDFfile);
                netCDF.getArrayDesignID();
            } catch (FileNotFoundException e) {
                System.out.println("i = " + i);
                throw e;
            } finally {
                Closeables.closeQuietly(netCDF);
            }
        }
    }
}
