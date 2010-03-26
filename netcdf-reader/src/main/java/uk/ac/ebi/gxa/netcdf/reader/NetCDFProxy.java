/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

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
    // this is false if opening a connection to the netcdf file failed
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

    public String getArrayDesignAccession() throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        return netCDF.findGlobalAttribute("ADaccession").getStringValue();
    }

    public int getArrayDesignID() throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        if (netCDF.findGlobalAttribute("ADid") != null) {
            Number value = netCDF.findGlobalAttribute("ADid").getNumericValue();
            if(value != null)
                return value.intValue();
            return -1;
        }

        return -1;
    }

    public int[] getAssays() throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        if (netCDF.findVariable("AS") == null) {
            return new int[0];
        }
        else {
            return (int[]) netCDF.findVariable("AS").read().copyTo1DJavaArray();
        }
    }

    public int[] getSamples() throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        if (netCDF.findVariable("BS") == null) {
            return new int[0];
        }
        else {
            return (int[]) netCDF.findVariable("BS").read().copyTo1DJavaArray();
        }
    }

    public int[][] getSamplesToAssays() throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        // read BS2AS
        if (netCDF.findVariable("BS2AS") == null) {
            return new int[0][0];
        }
        else {

            Array bs2as = netCDF.findVariable("BS2AS").read();
            // copy to an int array - BS2AS is 2d array so this should drop out
            return (int[][]) bs2as.copyToNDJavaArray();
        }
    }

    public int[] getDesignElements() throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        if (netCDF.findVariable("DE") == null) {
            return new int[0];
        }
        else {
            return (int[]) netCDF.findVariable("DE").read().copyTo1DJavaArray();
        }
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

        if (netCDF.findVariable("GN") == null) {
            return new int[0];
        }
        else {
            return (int[]) netCDF.findVariable("GN").read().copyTo1DJavaArray();
        }
    }

    public String[] getFactors() throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        if (netCDF.findVariable("EF") == null) {
            return new String[0];
        }
        else {
            // create a array of characters from the "EF" dimension
            ArrayChar efs = (ArrayChar) netCDF.findVariable("EF").read();
            // convert to a string array and return
            Object[] efsArray = (Object[]) efs.make1DStringArray().get1DJavaArray(String.class);
            String[] result = new String[efsArray.length];
            for (int i = 0; i < efsArray.length; i++) {
                result[i] = (String) efsArray[i];
                if(result[i].startsWith("ba_"))
                    result[i] = result[i].substring(3);
            }
            return result;
        }
    }

    public String[] getFactorValues(String factor) throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        // get all factors
        String[] efs = getFactors();

        // iterate over factors to find the index of the one we're interested in
        int efIndex = 0;
        boolean efFound = false;
        for (String ef : efs) {
            // todo: note flexible matching for ba_<factor> or <factor> - this is hack to work around old style netcdfs
            if (factor.matches("(ba_)?" + ef)) {
                efFound = true;
                break;
            }
            else {
                efIndex++;
            }
        }

        // if we couldn't match the factor we're looking for, return empty array
        if (!efFound) {
            log.warn("Couldn't locate index of " + factor + " in " + pathToNetCDF);
            return new String[0];
        }

        // if the EFV variable is empty
        if (netCDF.findVariable("EFV") == null) {
            return new String[0];
        }
        else {
            // now we have index of our ef, so take a read from efv for this index
            Array efvs = netCDF.findVariable("EFV").read();
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
    }

    public String[] getUniqueFactorValues() throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        // create a array of characters from the "SC" dimension
        if (netCDF.findVariable("uEFV") == null) {
            return new String[0];
        }
        else {
            ArrayChar uefv = (ArrayChar) netCDF.findVariable("uEFV").read();

            // convert to a string array and return
            Object[] uefvArray = (Object[]) uefv.make1DStringArray().get1DJavaArray(String.class);
            String[] result = new String[uefvArray.length];
            for (int i = 0; i < uefvArray.length; i++) {
                result[i] = (String) uefvArray[i];
            }
            return result;
        }
    }

    public String[] getCharacteristics() throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        if (netCDF.findVariable("SC") == null) {
            return new String[0];
        }
        else {
            // create a array of characters from the "SC" dimension
            ArrayChar scs = (ArrayChar) netCDF.findVariable("SC").read();
            // convert to a string array and return
            Object[] scsArray = (Object[]) scs.make1DStringArray().get1DJavaArray(String.class);
            String[] result = new String[scsArray.length];
            for (int i = 0; i < scsArray.length; i++) {
                result[i] = (String) scsArray[i];
                if(result[i].startsWith("bs_"))
                    result[i] = result[i].substring(3);
            }
            return result;
        }
    }

    public String[] getCharacteristicValues(String characteristic) throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        // get all characteristics
        String[] scs = getCharacteristics();

        // iterate over factors to find the index of the one we're interested in
        int scIndex = 0;
        boolean scFound = false;
        for (String sc : scs) {
            // todo: note flexible matching for ba_<factor> or <factor> - this is hack to work around old style netcdfs
            if (characteristic.matches("(bs_)?" + sc)) {
                scFound = true;
                break;
            }
            else {
                scIndex++;
            }
        }

        // if we couldn't match the characteristic we're looking for, return empty array
        if (!scFound) {
            log.error("Couldn't locate index of " + characteristic + " in " + pathToNetCDF);
            return new String[0];
        }

        if (netCDF.findVariable("SCV") == null) {
            return new String[0];
        }
        else {
            // now we have index of our sc, so take a read from scv for this index
            ArrayChar scvs = (ArrayChar) netCDF.findVariable("SCV").read();
            // slice this array on dimension '0' (this is SC dimension), retaining only these scvs ordered by sample
            ArrayChar sc_scv = (ArrayChar) scvs.slice(0, scIndex);
            // convert to a string array and return
            Object[] sc_scvArray = (Object[]) sc_scv.make1DStringArray().get1DJavaArray(String.class);
            String[] result = new String[sc_scvArray.length];
            for (int i = 0; i < sc_scvArray.length; i++) {
                result[i] = (String) sc_scvArray[i];
            }
            return result;
        }
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
    public float[] getExpressionDataForDesignElement(int designElementIndex) throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        Variable bdcVariable = netCDF.findVariable("BDC");
        if (bdcVariable == null) {
            return new float[0];
        }
        else {
            int[] bdcShape = bdcVariable.getShape();
            int[] origin = {designElementIndex, 0};
            int[] size = new int[]{1, bdcShape[1]};
            try {
                return (float[]) bdcVariable.read(origin, size).get1DJavaArray(float.class);
            }
            catch (InvalidRangeException e) {
                log.error("Error reading from NetCDF - invalid range at " + designElementIndex + ": " + e.getMessage());
                throw new IOException("Failed to read expression data for design element at " + designElementIndex +
                        ": caused by " + e.getClass().getSimpleName() + " [" + e.getMessage() + "]");
            }
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
    public float[] getExpressionDataForAssay(int assayIndex) throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        Variable bdcVariable = netCDF.findVariable("BDC");

        if (bdcVariable == null) {
            return new float[0];
        }
        else {
            int[] bdcShape = bdcVariable.getShape();
            int[] origin = {0, assayIndex};
            int[] size = new int[]{bdcShape[0], 1};
            try {
                return (float[]) bdcVariable.read(origin, size).get1DJavaArray(float.class);
            }
            catch (InvalidRangeException e) {
                log.error("Error reading from NetCDF - invalid range at " + assayIndex + ": " + e.getMessage());
                throw new IOException("Failed to read expression data for assay at " + assayIndex +
                        ": caused by " + e.getClass().getSimpleName() + " [" + e.getMessage() + "]");
            }
        }
    }

    public double[] getPValuesForDesignElement(int designElementIndex) throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        Variable pValVariable = netCDF.findVariable("PVAL");
        if (pValVariable == null) {
            return new double[0];
        }
        else {
            int[] pValShape = pValVariable.getShape();
            int[] origin = {designElementIndex, 0};
            int[] size = new int[]{1, pValShape[1]};
            try {
                return (double[]) pValVariable.read(origin, size).copyTo1DJavaArray();
            }
            catch (InvalidRangeException e) {
                log.error("Error reading from NetCDF - invalid range at " + designElementIndex + ": " + e.getMessage());
                throw new IOException("Failed to read p-value data for design element at " + designElementIndex +
                        ": caused by " + e.getClass().getSimpleName() + " [" + e.getMessage() + "]");
            }
        }
    }

    public float[] getPValuesForUniqueFactorValue(int uniqueFactorValueIndex) throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        Variable pValVariable = netCDF.findVariable("PVAL");

        if (pValVariable == null) {
            return new float[0];
        }
        else {
            int[] pValShape = pValVariable.getShape();
            int[] origin = {0, uniqueFactorValueIndex};
            int[] size = new int[]{pValShape[0], 1};
            try {
                return (float[]) pValVariable.read(origin, size).copyTo1DJavaArray();
            }
            catch (InvalidRangeException e) {
                log.error("Error reading from NetCDF - invalid range at " + uniqueFactorValueIndex + ": " +
                        e.getMessage());
                throw new IOException("Failed to read p-value data for unique factor value at " +
                        uniqueFactorValueIndex + ": caused by " + e.getClass().getSimpleName() + " " +
                        "[" + e.getMessage() + "]");
            }
        }
    }

    public float[] getTStatisticsForDesignElement(int designElementIndex) throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        Variable tStatVariable = netCDF.findVariable("TSTAT");
        if (tStatVariable == null) {
            return new float[0];
        }
        else {
            int[] tStatShape = tStatVariable.getShape();
            int[] origin = {designElementIndex, 0};
            int[] size = new int[]{1, tStatShape[1]};
            try {
                return (float[]) tStatVariable.read(origin, size).copyTo1DJavaArray();
            }
            catch (InvalidRangeException e) {
                log.error("Error reading from NetCDF - invalid range at " + designElementIndex + ": " + e.getMessage());
                throw new IOException("Failed to read t-statistic data for design element at " + designElementIndex +
                        ": caused by " + e.getClass().getSimpleName() + " [" + e.getMessage() + "]");
            }
        }
    }

    public float[] getTStatisticsForUniqueFactorValue(int uniqueFactorValueIndex) throws IOException {
        if (!proxied) {
            throw new IOException("Unable to open NetCDF file at " + pathToNetCDF);
        }

        Variable tStatVariable = netCDF.findVariable("TSTAT");

        if (tStatVariable == null) {
            return new float[0];
        }
        else {
            int[] tStatShape = tStatVariable.getShape();
            int[] origin = {0, uniqueFactorValueIndex};
            int[] size = new int[]{tStatShape[0], 1};
            try {
                return (float[]) tStatVariable.read(origin, size).copyTo1DJavaArray();
            }
            catch (InvalidRangeException e) {
                log.error("Error reading from NetCDF - invalid range at " + uniqueFactorValueIndex + ": " +
                        e.getMessage());
                throw new IOException("Failed to read t-statistic data for unique factor value at " + 
                        uniqueFactorValueIndex + ": caused by " + e.getClass().getSimpleName() + " " +
                        "[" + e.getMessage() + "]");
            }
        }
    }

    /**
     * Closes the proxied NetCDF file
     * @throws IOException
     */
    public void close() throws IOException {
        if(this.netCDF != null)
            this.netCDF.close();
    }
}
