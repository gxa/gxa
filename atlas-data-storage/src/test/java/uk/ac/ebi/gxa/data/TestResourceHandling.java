package uk.ac.ebi.gxa.data;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;

import static com.google.common.io.Closeables.close;

public class TestResourceHandling {
    private static final Logger log = LoggerFactory.getLogger(TestResourceHandling.class);

    @Test
    public void testOpenClose() throws IOException, URISyntaxException, AtlasDataException {
        File netCDFfile = new File(getClass().getClassLoader().getResource("MEXP/1500/E-MEXP-1586/E-MEXP-1586_A-AFFY-44.nc").toURI());
        for (int i = 0; i < 20000; i++) {
            NetCDFProxy netCDF = null;
            try {
                netCDF = new NetCDFProxyV1(netCDFfile);
                netCDF.getArrayDesignAccession();
            } finally {
                close(netCDF, false);
            }
        }
    }
}
