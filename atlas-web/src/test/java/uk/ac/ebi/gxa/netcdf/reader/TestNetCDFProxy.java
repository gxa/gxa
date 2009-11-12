package uk.ac.ebi.gxa.netcdf.reader;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 12-Nov-2009
 */
public class TestNetCDFProxy extends TestCase {
    private File netCDFfile;

    private NetCDFProxy netCDF;

    @Override
    protected void setUp() throws Exception {
        netCDFfile = new File("/home/tburdett/atlas/netcdfs/567112124_159009398.nc");
        netCDF = new NetCDFProxy(netCDFfile);
    }

    @Override
    protected void tearDown() throws Exception {
        netCDFfile = null;
        netCDF = null;
    }

    public void testGetExperiment() throws IOException {
        System.out.println("Experiment: " + netCDF.getExperiment());
    }

    public void testGetArrayDesign() throws IOException {
        System.out.println("ArrayDesign: " + netCDF.getArrayDesign());
    }

    public void testGetAssays() throws IOException {
        System.out.print("Assays: {");
        for (int assay : netCDF.getAssays()) {
            System.out.print(assay + ", ");
        }
        System.out.println("}");
    }

    public void testGetSamples() throws IOException {
        System.out.print("Samples: {");
        for (int sample : netCDF.getSamples()) {
            System.out.print(sample + ", ");
        }
        System.out.println("}");
    }

    public void testGetAssaysToSamples() throws IOException {
    }

    public void testGetDesignElements() throws IOException {
        System.out.print("Design Elements: {");
        for (int de : netCDF.getDesignElements()) {
            System.out.print(de + ", ");
        }
        System.out.println("}");
    }

    public void testGetGenes() throws IOException {
        System.out.print("Genes: {");
        for (int gene : netCDF.getGenes()) {
            System.out.print(gene + ", ");
        }
        System.out.println("}");
    }

    public void testGetFactors() throws IOException {
        System.out.print("EFs: {");
        for (String factor : netCDF.getFactors()) {
            System.out.print(factor + ", ");
        }
        System.out.println("}");
    }

    public void testGetFactorValues() throws IOException {
        String factor = netCDF.getFactors()[0];

        System.out.print("EFVs: {");
        for (String efv : netCDF.getFactorValues(factor)) {
            System.out.print(efv + ", ");
        }
        System.out.println("}");
    }

    public void testGetUniqueFactorValues() throws IOException {
    }

    public void testGetCharacteristics() throws IOException {
    }

    public void testGetCharacteristicValues(String characteristic) throws IOException {
    }

    public void testGetExpressionMatrix() throws IOException {
    }

    public void testGetExpressionDataForDesignElement(int designElementIndex) throws IOException {
    }

    public void testGetExpressionDataForAssay(int assayIndex) throws IOException {
    }
}
