package uk.ac.ebi.gxa.web;

import junit.framework.TestCase;
import org.json.JSONObject;
import uk.ac.ebi.gxa.netcdf.reader.NetCDFProxy;

import java.io.File;
import java.io.IOException;
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
    private ae3.service.AtlasPlotter oldStylePlotter;

    private NetCDFProxy netCDF;
    private int geneID;
    private String ef;
    private List<String> topFVs;
    private List<Integer> geneIndices;

    @Override
    protected void setUp() throws Exception {
        plotter = new AtlasPlotter();
//        oldStylePlotter = ae3.service.AtlasPlotter.instance();

        // create expression data set
//		ExpressionDataSet ds = DataServerAPI.retrieveExpressionDataSet(geneIdKey, expIdKey, efToPlot);

        netCDF = new NetCDFProxy(new File("/home/tburdett/atlas/netcdfs/567112124_159009398.nc"));
        geneID = netCDF.getGenes()[0];
        ef = netCDF.getFactors()[0];
        topFVs = new ArrayList<String>();
        geneIndices = new ArrayList<Integer>();
    }

    @Override
    protected void tearDown() throws Exception {
        plotter = null;
        oldStylePlotter = null;
    }

    public void testCreateJSON() {
        try {
            JSONObject json = plotter.createJSON(netCDF, ef, topFVs, geneIndices);
            System.out.println(json.toString());
        }
        catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testCreateThumbnailJSON() {

    }

    public void testCreateBigPLotJSON() {

    }
}
