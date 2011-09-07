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

package uk.ac.ebi.gxa.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Longs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;
import uk.ac.ebi.microarray.atlas.model.UpDownCondition;
import uk.ac.ebi.microarray.atlas.model.UpDownExpression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.lang.Float.isNaN;

/**
 * An object that proxies an Atlas NetCDF file and provides convenience methods for accessing the data from within. This
 * class should be used when trying to read data on-the-fly out of a NetCDF directly.
 * <p/>
 * The NetCDFs for Atlas are structured as follows:
 * <pre>
 *    char  ASacc(AS) ;
 *    char  BSacc(BS) ;
 *    int   BS2AS(BS, AS) ;
 *    char  DEacc(DE) ;
 *    long  GN(GN) ;
 *    int DE2GN(DE, GN) ;
 *    char EF(EF, EFlen) ;
 *    char  EFSC(EFSC, EFSlen) ;
 *    char  EFV(EF, AS, EFlen) ;
 *    char  uVAL(uVAL, EFSClen) ; - uVAL contains all unique ef-efvs and sc-scvs (if ef-efv == sc-scv, it is only stored once)
 *    int   uVALnum(EFSC) ;
 *    char SC(SC, SClen) ;
 *    char  SCV(SC, BS, SClen) ;
 *    float BDC(DE, AS) ;
 *    float PVAL(DE, uVAL) ;
 *    float TSTAT(DE, uVAL) ;
 * </pre>
 *
 * @author Tony Burdett
 */
final class NetCDFProxyV1 extends NetCDFProxy {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final File pathToNetCDF;
    private final NetcdfFile netCDF;

    NetCDFProxyV1(File file) throws IOException, AtlasDataException {
        this.pathToNetCDF = file.getAbsoluteFile();
        this.netCDF = NetcdfDataset.acquireFile(file.getAbsolutePath(), null);
        if (isOutOfDate()) {
            throw new AtlasDataException("ncdf " + pathToNetCDF.getName() + " for experiment: " + getExperimentAccession() + " is out of date - please update it and then recompute its analytics via Atlas administration interface");
        }
    }

    boolean isOutOfDate() {
        final String version = getNcdfVersion();
        // "NetCDF Updater" string was used as version id in NetCDFs created by updater
        // before March 23, 2011
        return !"1.0".equals(version) && !"NetCDF Updater".equals(version);
    }

    @Override
    String getExperimentAccession() {
        return netCDF.findGlobalAttribute("experiment_accession").getStringValue();
    }

    @Override
    String getArrayDesignAccession() {
        return netCDF.findGlobalAttribute("ADaccession").getStringValue();
    }

    private String getNcdfVersion() {
        if (netCDF.findGlobalAttribute("CreateNetCDF_VERSION") == null) {
            return null;
        }
        return netCDF.findGlobalAttribute("CreateNetCDF_VERSION").getStringValue();
    }

    @Override
    int[][] getSamplesToAssays() throws IOException {
        // read BS2AS
        Variable bs2as = netCDF.findVariable("BS2AS");
        if (bs2as == null) {
            return new int[0][0];
        }

        // copy to an int array - BS2AS is 2d array so this should drop out
        return (int[][]) bs2as.read().copyToNDJavaArray();
    }

    /**
     * Gets the array of gene IDs from this NetCDF
     *
     * @return an long[] representing the one dimensional array of gene identifiers
     * @throws IOException if accessing the NetCDF failed
     */
    @Override
    long[] getGenes() throws IOException {
        return getLongArray1(netCDF, "GN");
    }

    private String[] designElementAccessions;
    @Override
    String[] getDesignElementAccessions() throws IOException {
        if (designElementAccessions == null) {
            designElementAccessions = getArrayOfStrings(netCDF, "DEacc");
        }
        return designElementAccessions;
    }

    @Override
    public String[] getAssayAccessions() throws IOException {
        return getArrayOfStrings(netCDF, "ASacc");
    }

    @Override
    String[] getSampleAccessions() throws IOException {
        return getArrayOfStrings(netCDF, "BSacc");
    }

    @Override
    String[] getFactors() throws IOException {
        return getFactorsCharacteristics(netCDF, "EF");
    }

    @Override
    String[] getCharacteristics() throws IOException {
        return getFactorsCharacteristics(netCDF, "SC");
    }

    @Override
    String[] getFactorValues(String factor) throws IOException {
        Integer efIndex = findEfIndex(factor);
        return efIndex == null ? new String[0] : getSlice3D(netCDF, "EFV", efIndex);
    }

    /**
     * Returns the whole matrix of factor values for assays (|Assay| X |EF|).
     *
     * @return an array of strings - an array of factor values per assay
     * @throws IOException if data could not be read form the netCDF file
     */
    @Override
    String[][] getFactorValues() throws IOException {
        Array array = netCDF.findVariable("EFV").read();
        int[] shape = array.getShape();

        String[][] result = new String[shape[0]][shape[1]];
        for (int i = 0; i < shape[0]; i++) {
            ArrayChar s = (ArrayChar) array.slice(0, i);
            Object[] ss = (Object[]) s.make1DStringArray().get1DJavaArray(String.class);
            for (int j = 0; j < ss.length; j++) {
                result[i][j] = (String) ss[j];
            }
        }
        return result;
    }

    private List<KeyValuePair> uniqueValues;
    @Override
    List<KeyValuePair> getUniqueValues() throws IOException, AtlasDataException {
        if (uniqueValues == null) {
            Variable uVALVar = netCDF.findVariable("uVAL");
        
            if (uVALVar == null) {
                // This is to allow for backwards compatibility
                uVALVar = netCDF.findVariable("uEFV");
            }
        
            if (uVALVar == null) {
                uniqueValues = Collections.emptyList();
            } else {
                uniqueValues = new LinkedList<KeyValuePair>();

                ArrayChar uVal = (ArrayChar)uVALVar.read();
                for (Object text : (Object[])uVal.make1DStringArray().get1DJavaArray(String.class)) {
                    final String[] data = ((String) text).split(NCDF_PROP_VAL_SEP_REGEX, -1);
                    if (data.length != 2) {
                        throw new AtlasDataException("Invalid uVAL element: " + text);
                    }
            
                    if (!"".equals(data[1])) {
                        uniqueValues.add(new KeyValuePair(data[0], data[1]));
                    }
                }
            }
        }
        return uniqueValues;
    }

    @Override
    List<KeyValuePair> getUniqueFactorValues() throws IOException, AtlasDataException {
        List<KeyValuePair> uniqueEFVs = new ArrayList<KeyValuePair>();
        List<String> factors = Arrays.asList(getFactors());

        for (KeyValuePair propVal : getUniqueValues()) {
            if (factors.contains(propVal.key)) {
                // Since getUniqueValues() returns both ef-efvs/sc-scvs, filter out scs that aren't also efs
                uniqueEFVs.add(propVal);
            }
        }
        return uniqueEFVs;
    }

    @Override
    String[] getCharacteristicValues(String characteristic) throws IOException {
        Integer scIndex = findScIndex(characteristic);
        return scIndex == null ? new String[0] : getSlice3D(netCDF, "SCV", scIndex);
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
    @Override
    float[] getExpressionDataForDesignElementAtIndex(int designElementIndex) throws IOException, AtlasDataException {
        return readFloatValuesForRowIndex(netCDF, designElementIndex, "BDC");
    }

    /**
     * Extracts a matrix of expression values for given design element indices.
     *
     * @param deIndices an array of design element indices to get expression values for
     * @return a float matrix - a list of expressions per design element index
     * @throws IOException           if the expression data could not be read from the netCDF file
     * @throws InvalidRangeException if the file doesn't contain given deIndices
     */
    @Override
    FloatMatrixProxy getExpressionValues(int[] deIndices) throws IOException, AtlasDataException {
        return readFloatValuesForRowIndices(netCDF, deIndices, "BDC");
    }

    @Override
    TwoDFloatArray getAllExpressionData() throws IOException {
        return readFloatValuesForAllRows(netCDF, "BDC");
    }

    /**
     * Extracts T-statistic matrix for given design element indices.
     *
     * @param deIndices an array of design element indices to extract T-statistic for
     * @return matrix of floats - an array of T-statistic values per each design element index
     * @throws IOException           if the data could not be read from the netCDF file
     * @throws InvalidRangeException if array of design element indices contains out of bound indices
     */
    @Override
    FloatMatrixProxy getTStatistics(int[] deIndices) throws IOException, AtlasDataException {
        return readFloatValuesForRowIndices(netCDF, deIndices, "TSTAT");
    }

    @Override
    float[] getTStatisticsForDesignElement(int designElementIndex) throws IOException, AtlasDataException {
        return readFloatValuesForRowIndex(netCDF, designElementIndex, "TSTAT");
    }

    @Override
    TwoDFloatArray getTStatistics() throws IOException {
        return readFloatValuesForAllRows(netCDF, "TSTAT");
    }

    /**
     * Extracts P-value matrix for given design element indices.
     *
     * @param deIndices an array of design element indices to extract P-values for
     * @return matrix of floats - an array of  P-values per each design element index
     * @throws IOException           if the data could not be read from the netCDF file
     * @throws InvalidRangeException if array of design element indices contains out of bound indices
     */
    @Override
    FloatMatrixProxy getPValues(int[] deIndices) throws IOException, AtlasDataException {
        return readFloatValuesForRowIndices(netCDF, deIndices, "PVAL");
    }

    @Override
    float[] getPValuesForDesignElement(int designElementIndex) throws IOException, AtlasDataException {
        return readFloatValuesForRowIndex(netCDF, designElementIndex, "PVAL");
    }

    @Override
    TwoDFloatArray getPValues() throws IOException {
        return readFloatValuesForAllRows(netCDF, "PVAL");
    }

    /**
     * Closes the proxied NetCDF file
     *
     * @throws java.io.IOException on close errors
     */
    public void close() throws IOException {
        if (netCDF != null) {
            netCDF.close();
        }
    }

    @Override
    public String toString() {
        return "NetCDFProxy{" +
                "pathToNetCDF=" + pathToNetCDF +
                '}';
    }
}
