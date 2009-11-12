package uk.ac.ebi.gxa.netcdf.reader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;

/**
 * An object that proxies an Atlas NetCDF file and provides convenience methods for accessing the data from within. This
 * class should be used when trying to read data on-the-fly out of a NetCDF directly.
 * <p/>
 * The NetCDFs for Atlas are structured as follows:
 * <pre>
 *    int AS(AS) ;
 *    int BS(BS) ;
 *    int BS2AS(BS, AS) ;
 *    int DE(DE) ;
 *    int GN(GN) ;
 *    int DE2GN(DE, GN) ;
 *    char EF(EF, EFlen) ;
 *    char EFV(EF, AS, EFlen) ;
 *    char uEFV(uEFV, EFlen) ;
 *    int uEFVnum(EF) ;
 *    char SC(SC, SClen) ;
 *    char SCV(SC, BS, SClen) ;
 *    double BDC(DE, AS) ;
 *    double PVAL(DE, uEFV) ;
 *    double TSTAT(DE, uEFV) ;
 * </pre>
 *
 * @author Tony Burdett
 * @date 11-Nov-2009
 */
public class NetCDFProxy {
    // this is false if oeping a conneciton to the netcdf file failed
    private boolean proxied;
    private String pathToNetCDF;

    private NetcdfFile netCDF;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public NetCDFProxy(File netCDF) {
        this.pathToNetCDF = netCDF.getAbsolutePath();
        try {
            this.netCDF = NetcdfFile.open(netCDF.getAbsolutePath());
            proxied = true;
        }
        catch (IOException e) {
            proxied = false;
        }
    }

    public String getExperiment() throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        return netCDF.findGlobalAttribute("experiment_accession").getStringValue();
    }

    public String getArrayDesign() throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        return netCDF.findGlobalAttribute("ADaccession").getStringValue();
    }

    public int[] getAssays() throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        return (int[]) netCDF.findVariable("AS").read().copyTo1DJavaArray();
    }

    public int[] getSamples() throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        return (int[]) netCDF.findVariable("BS").read().copyTo1DJavaArray();
    }

    public int[][] getAssaysToSamples() throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        // read BS2AS
        Array bs2as = netCDF.findVariable("BS2AS").read();
        // copy to an int array - BS2AS is 2d array so this should drop out
        return (int[][]) bs2as.copyToNDJavaArray();
    }

    public int[] getDesignElements() throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        return (int[]) netCDF.findVariable("DE").read().copyTo1DJavaArray();
    }

    /**
     * Gets the array of gene ID integers from this NetCDF
     *
     * @return an int[] representing the one dimensional array of gene identifiers
     * @throws IOException if accessing the NetCDF failed
     */
    public int[] getGenes() throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        return (int[]) netCDF.findVariable("GN").read().copyTo1DJavaArray();
    }

    public String[] getFactors() throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        // create a array of characters from the "EF" dimension
        ArrayChar efs = (ArrayChar) netCDF.findVariable("EF").read();
        // convert to a string array and return
        Object[] efsArray = (Object[]) efs.make1DStringArray().get1DJavaArray(String.class);
        String[] result = new String[efsArray.length];
        for (int i = 0; i < efsArray.length; i++) {
            result[i] = (String) efsArray[i];
        }
        return result;
    }

    public String[] getFactorValues(String factor) throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        // get all factors
        String[] efs = getFactors();

        // iterate over factors to find the index of the one we're interested in
        int efIndex = 0;
        for (String ef : efs) {
            if (ef.equals(factor)) {
                break;
            }
            else {
                efIndex++;
            }
        }

        // now we have index of our ef, so take a read from efv for this index
        ArrayChar efvs = (ArrayChar) netCDF.findVariable("EFV").read();
        // slice this array on dimension '0' (this is EF dimension), retaining only these efvs ordered by assay
        ArrayChar ef_efv = (ArrayChar) efvs.slice(0, efIndex);

        // convert to a string array and return
        Object[] ef_efvArray = (Object[]) ef_efv.make1DStringArray().get1DJavaArray(String.class);
        String[] result = new String[ef_efvArray.length];
        for (int i = 0; i < ef_efvArray.length; i++) {
            result[i] = (String) ef_efvArray[i];
        }
        return result;
    }

    public String[] getUniqueFactorValues() throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        // create a array of characters from the "SC" dimension
        ArrayChar uefv = (ArrayChar) netCDF.findVariable("uEFV").read();
        // convert to a string array and return
        return (String[]) uefv.make1DStringArray().get1DJavaArray(String.class);
    }

    public String[] getCharacteristics() throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        // create a array of characters from the "SC" dimension
        ArrayChar scs = (ArrayChar) netCDF.findVariable("SC").read();
        // convert to a string array and return
        return (String[]) scs.make1DStringArray().get1DJavaArray(String.class);
    }

    public String[] getCharacteristicValues(String characteristic) throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        // get all characteristics
        String[] scs = getFactors();

        // iterate over factors to find the index of the one we're interested in
        int scIndex = 0;
        for (String sc : scs) {
            if (sc.equals(characteristic)) {
                break;
            }
            else {
                scIndex++;
            }
        }

        // now we have index of our sc, so take a read from scv for this index
        ArrayChar scvs = (ArrayChar) netCDF.findVariable("SCV").read();
        // slice this array on dimension '0' (this is SC dimension), retaining only these scvs ordered by sample
        ArrayChar sc_scv = (ArrayChar) scvs.slice(0, scIndex);
        return (String[]) sc_scv.make1DStringArray().get1DJavaArray(String.class);
    }

    /**
     * Gets all expression data from this NetCDF, in a two dimensional double array.  This array is indexed by assays
     * and design elements.
     *
     * @return a 2-D double array representing the expression matrix
     * @throws IOException if something went wrong accessing the NetCDF
     */
    public double[][] getExpressionMatrix() throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        // read bdc
        Array bdc = netCDF.findVariable("BDC").read();
        // copy to a double array - BDC is 2d array so this should drop out
        return (double[][]) bdc.copyToNDJavaArray();
    }

    /**
     * Gets a single row from the expression data matrix representing all expression data for a single design element.
     * This is obtained by retrieving all data from the given row in the expression matrix, where the design element
     * index supplied is the row number.  As the expression value matrix has the same ordering as the design element
     * array, you can iterate over the design element array to retrieve the index of the row you want to fetch.
     *
     * @param designElementIndex the index of the design element which we're interested in fetching data for
     * @return the double array representing expression values for this design element
     * @throws IOException if the NetCDF could not be accessed
     */
    public double[] getExpressionDataForDesignElement(int designElementIndex) throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        Variable bdcVariable = netCDF.findVariable("BDC");

        int[] bdcShape = bdcVariable.getShape();
        int[] origin = {designElementIndex, 0};
        int[] size = new int[]{1, bdcShape[1]};
        try {
            return (double[]) bdcVariable.read(origin, size).copyTo1DJavaArray();
        }
        catch (InvalidRangeException e) {
            log.error("Error reading from NetCDF - invalid range at " + designElementIndex + ": " + e.getMessage());
            throw new IOException("Failed to read expression data for design element at " + designElementIndex +
                    ": caused by " + e.getClass().getSimpleName() + " [" + e.getMessage() + "]");
        }
    }

    /**
     * Gets a single column from the expression data matrix representing all expression data for a single assay. This is
     * obtained by retrieving all data from the given column in the expression matrix, where the assay index supplied is
     * the column number.  As the expression value matrix has the same ordering as the assay array, you can iterate over
     * the assay array to retrieve the index of the column you want to fetch.
     *
     * @param assayIndex the index of the assay which we're interested in fetching data for
     * @return the double array representing expression values for this assay
     * @throws IOException if the NetCDF could not be accessed
     */
    public double[] getExpressionDataForAssay(int assayIndex) throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        Variable bdcVariable = netCDF.findVariable("BDC");

        int[] bdcShape = bdcVariable.getShape();
        int[] origin = {0, assayIndex};
        int[] size = new int[]{bdcShape[0], 1};
        try {
            return (double[]) bdcVariable.read(origin, size).copyTo1DJavaArray();
        }
        catch (InvalidRangeException e) {
            log.error("Error reading from NetCDF - invalid range at " + assayIndex + ": " + e.getMessage());
            throw new IOException("Failed to read expression data for assay at " + assayIndex +
                    ": caused by " + e.getClass().getSimpleName() + " [" + e.getMessage() + "]");
        }
    }
}
