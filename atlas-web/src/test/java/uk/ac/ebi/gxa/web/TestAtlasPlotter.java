package uk.ac.ebi.gxa.web;

import junit.framework.TestCase;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 12-Nov-2009
 */
public class TestAtlasPlotter extends TestCase {
    private AtlasPlotter plotter;

    private NetCDFProxy netCDF;
    private int geneID;
    private String ef;
    private List<String> topFVs;
    private List<Integer> geneIndices;

    @Override
    protected void setUp() throws Exception {
        plotter = new AtlasPlotter();

        netCDF = new NetCDFProxy(new File(getClass().getClassLoader().getResource("645932669_159274783.nc").toURI()));
        geneID = netCDF.getGenes()[0];
        ef = netCDF.getFactors()[0];
        topFVs = new ArrayList<String>();
        geneIndices = new ArrayList<Integer>();
    }

    @Override
    protected void tearDown() throws Exception {
        plotter = null;
    }

}
