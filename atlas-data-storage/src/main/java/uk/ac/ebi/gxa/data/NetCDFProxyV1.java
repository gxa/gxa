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

import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import uk.ac.ebi.gxa.utils.Pair;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
    private final NetcdfFile netCDF;

    NetCDFProxyV1(File file) throws AtlasDataException {
        try {
            final String path = file.getAbsolutePath();
            this.netCDF = NetcdfDataset.acquireFile(path, null);
            if (isOutOfDate()) {
                close();
                throw new AtlasDataException("ncdf " + path + " is out of date - " +
                        "please update it and then recompute its analytics via Atlas administration interface");
            }
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    boolean isOutOfDate() {
        final String version = getVersion();
        // "NetCDF Updater" string was used as version id in NetCDFs created by updater
        // before March 23, 2011
        return !"1.0".equals(version) && !"NetCDF Updater".equals(version);
    }

    public String getExperimentAccession() {
        return netCDF.findGlobalAttribute("experiment_accession").getStringValue();
    }

    public String getArrayDesignAccession() {
        return netCDF.findGlobalAttribute("ADaccession").getStringValue();
    }

    public String getVersion() {
        if (netCDF.findGlobalAttribute("CreateNetCDF_VERSION") == null) {
            return null;
        }
        return netCDF.findGlobalAttribute("CreateNetCDF_VERSION").getStringValue();
    }

    public int[][] getSamplesToAssays() throws AtlasDataException {
        try {
            // read BS2AS
            Variable bs2as = netCDF.findVariable("BS2AS");
            if (bs2as == null) {
                return new int[0][0];
            }

            // copy to an int array - BS2AS is 2d array so this should drop out
            return (int[][]) bs2as.read().copyToNDJavaArray();
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    /**
     * Gets the array of gene IDs from this NetCDF
     *
     * @return an long[] representing the one dimensional array of gene identifiers
     * @throws AtlasDataException if accessing the NetCDF failed
     */
    public long[] getGenes() throws AtlasDataException {
        return getLongArray1(netCDF, "GN");
    }

    private String[] designElementAccessions;

    public String[] getDesignElementAccessions() throws AtlasDataException {
        if (designElementAccessions == null) {
            designElementAccessions = getArrayOfStrings(netCDF, "DEacc");
        }
        return designElementAccessions;
    }

    public String[] getAssayAccessions() throws AtlasDataException {
        return getArrayOfStrings(netCDF, "ASacc");
    }

    public String[] getFactors() throws AtlasDataException {
        return getFactorsCharacteristics(netCDF, "EF");
    }

    public String[] getCharacteristics() throws AtlasDataException {
        return getFactorsCharacteristics(netCDF, "SC");
    }

    public String[] getFactorValues(String factor) throws AtlasDataException {
        Integer efIndex = findEfIndex(factor);
        return efIndex == null ? new String[0] : getSlice3D(netCDF, "EFV", efIndex);
    }

    /**
     * Returns the whole matrix of factor values for assays (|Assay| X |EF|).
     *
     * @return an array of strings - an array of factor values per assay
     * @throws AtlasDataException if data could not be read form the netCDF file
     */
    public String[][] getFactorValues() throws AtlasDataException {
        try {
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
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    public String[] getCharacteristicValues(String characteristic) throws AtlasDataException {
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
     * @throws AtlasDataException if the NetCDF could not be accessed
     */
    public float[] getExpressionDataForDesignElementAtIndex(int designElementIndex) throws AtlasDataException {
        return readFloatValuesForRowIndex(netCDF, designElementIndex, "BDC");
    }

    /**
     * Extracts a matrix of expression values for given design element indices.
     *
     * @param deIndices an array of design element indices to get expression values for
     * @return a float matrix - a list of expressions per design element index
     * @throws AtlasDataException if the expression data could not be read from the netCDF file,
     *                            or if the file doesn't contain given deIndices
     */
    public FloatMatrixProxy getExpressionData(int[] deIndices) throws AtlasDataException {
        return readFloatValuesForRowIndices(netCDF, deIndices, "BDC");
    }

    public FloatMatrixProxy getAllExpressionData() throws AtlasDataException {
        return readFloatValuesForAllRows(netCDF, "BDC");
    }


    private List<Pair<String, String>> uniqueValues;

    public List<Pair<String, String>> getUniqueEFVs() throws AtlasDataException {
        try {
            if (uniqueValues == null) {
                Variable uVALVar = netCDF.findVariable("uVAL");

                if (uVALVar == null) {
                    // This is to allow for backwards compatibility
                    uVALVar = netCDF.findVariable("uEFV");
                }

                if (uVALVar == null) {
                    uniqueValues = Collections.emptyList();
                } else {
                    uniqueValues = new LinkedList<Pair<String, String>>();

                    ArrayChar uVal = (ArrayChar) uVALVar.read();
                    for (Object text : (Object[]) uVal.make1DStringArray().get1DJavaArray(String.class)) {
                        final String[] data = ((String) text).split(NCDF_PROP_VAL_SEP_REGEX, -1);
                        if (data.length != 2) {
                            throw new AtlasDataException("Invalid uVAL element: " + text);
                        }

                        if (!"".equals(data[1])) {
                            uniqueValues.add(Pair.create(data[0], data[1]));
                        }
                    }
                }
            }
            return uniqueValues;
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    public FloatMatrixProxy getTStatistics(int[] des) throws AtlasDataException {
        return readFloatValuesForRowIndices(netCDF, des, "TSTAT");
    }

    public FloatMatrixProxy getPValues(int[] des) throws AtlasDataException {
        return readFloatValuesForRowIndices(netCDF, des, "PVAL");
    }

    @Override
    public FloatMatrixProxy getAllTStatistics() throws AtlasDataException {
        return readFloatValuesForAllRows(netCDF, "TSTAT");
    }

    @Override
    public FloatMatrixProxy getAllPValues() throws AtlasDataException {
        return readFloatValuesForAllRows(netCDF, "PVAL");
    }

    /**
     * Closes the proxied NetCDF file
     */
    public void close() {
        closeQuietly(netCDF);
    }

    @Override
    public String toString() {
        return "NetCDFProxyV1{" +
                "netCDF=" + netCDF.getLocation() +
                '}';
    }
}
