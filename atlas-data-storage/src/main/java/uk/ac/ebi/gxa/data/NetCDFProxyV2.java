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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * An object that proxies an Atlas NetCDF file and provides convenience methods for accessing the data from within. This
 * class should be used when trying to read data on-the-fly out of a NetCDF directly.
 * <p/>
 * Data NetCDFs for Atlas are structured as follows:
 * <pre>
 *    char  ASacc(AS) ;
 *    char  BSacc(BS) ;
 *    int   BS2AS(BS, AS) ;
 *    float BDC(DE, AS) ;
 *    char  DEacc(DE) ;
 *    long  GN(GN) ;
 *    char  EF(EF, EFlen) ;
 *    char  EFV(EF, AS, EFlen) ;
 *    char  SC(SC, SClen) ;
 *    char  SCV(SC, BS, SClen) ;
 * </pre>
 *
 * Statistics NetCDFs for Atlas are structured as follows:
 * <pre>
 *    char  propertyNAME(uVAL, propertyNAMElen)
 *    char  propertyVALUE(uVAL, propertyVALUElen)
 *    int   ORDER_ANY(DE, uVAL) ;
 *    int   ORDER_DOWN(DE, uVAL) ;
 *    int   ORDER_NON_D_E(DE, uVAL) ;
 *    int   ORDER_UP(DE, uVAL) ;
 *    int   ORDER_UP_DOWN(DE, uVAL) ;
 *    float PVAL(DE, uVAL) ;
 *    float TSTAT(DE, uVAL) ;
 * </pre>
 *
 * @author Tony Burdett
 */
final class NetCDFProxyV2 extends NetCDFProxy {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final NetcdfFile dataNetCDF;
    private NetcdfFile statisticsNetCDF;

    NetCDFProxyV2(File dataFile, File statisticsFile) throws AtlasDataException {
        try {
            this.dataNetCDF = NetcdfDataset.acquireFile(dataFile.getAbsolutePath(), null);
            try {
                this.statisticsNetCDF = NetcdfDataset.acquireFile(statisticsFile.getAbsolutePath(), null);
            } catch (IOException e) {
            }
        } catch (IOException e) {
            try {
                close();
            } catch (IOException ioe) {
            }
            throw new AtlasDataException(e);
        }
    }

    @Override
    String getExperimentAccession() {
        return dataNetCDF.findGlobalAttribute("experiment_accession").getStringValue();
    }

    @Override
    String getArrayDesignAccession() {
        return dataNetCDF.findGlobalAttribute("ADaccession").getStringValue();
    }

    @Override
    String getVersion() {
        if (dataNetCDF.findGlobalAttribute("CreateNetCDF_VERSION") == null) {
            return null;
        }
        return dataNetCDF.findGlobalAttribute("CreateNetCDF_VERSION").getStringValue();
    }

    @Override
    int[][] getSamplesToAssays() throws AtlasDataException {
        try {
            // read BS2AS
            Variable bs2as = dataNetCDF.findVariable("BS2AS");
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
     * @throws IOException if accessing the NetCDF failed
     */
    @Override
    long[] getGenes() throws AtlasDataException {
        return getLongArray1(dataNetCDF, "GN");
    }

    private String[] designElementAccessions;
    @Override
    String[] getDesignElementAccessions() throws AtlasDataException {
        if (designElementAccessions == null) {
            designElementAccessions = getArrayOfStrings(dataNetCDF, "DEacc");
        }
        return designElementAccessions;
    }

    @Override
    public String[] getAssayAccessions() throws AtlasDataException {
        return getArrayOfStrings(dataNetCDF, "ASacc");
    }

    @Override
    String[] getSampleAccessions() throws AtlasDataException {
        return getArrayOfStrings(dataNetCDF, "BSacc");
    }

    @Override
    String[] getFactors() throws AtlasDataException {
        return getFactorsCharacteristics(dataNetCDF, "EF");
    }

    @Override
    String[] getCharacteristics() throws AtlasDataException {
        return getFactorsCharacteristics(dataNetCDF, "SC");
    }

    @Override
    String[] getFactorValues(String factor) throws AtlasDataException {
        Integer efIndex = findEfIndex(factor);
        return efIndex == null ? new String[0] : getSlice3D(dataNetCDF, "EFV", efIndex);
    }

    /**
     * Returns the whole matrix of factor values for assays (|Assay| X |EF|).
     *
     * @return an array of strings - an array of factor values per assay
     * @throws AtlasDataException if data could not be read form the dataNetCDF file
     */
    @Override
    String[][] getFactorValues() throws AtlasDataException {
        try {
            Array array = dataNetCDF.findVariable("EFV").read();
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

    private List<KeyValuePair> uniqueValues;
    @Override
    List<KeyValuePair> getUniqueValues() throws AtlasDataException, StatisticsNotFoundException {
        if (statisticsNetCDF == null) {
            throw new StatisticsNotFoundException("Statistics file does not exist");
        }

        if (uniqueValues == null) {
            final String[] names = getArrayOfStrings(statisticsNetCDF, "propertyNAME");
            final String[] values = getArrayOfStrings(statisticsNetCDF, "propertyVALUE");
            if (names.length != values.length) {
                throw new AtlasDataException("Inconsistent names/values data in " + this);
            }

            uniqueValues = new ArrayList<KeyValuePair>(names.length);
            for (int i = 0; i < names.length; ++i) {
                uniqueValues.add(new KeyValuePair(names[i], values[i]));
            }
        }
        return uniqueValues;
    }

    @Override
    String[] getCharacteristicValues(String characteristic) throws AtlasDataException {
        Integer scIndex = findScIndex(characteristic);
        return scIndex == null ? new String[0] : getSlice3D(dataNetCDF, "SCV", scIndex);
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
    @Override
    float[] getExpressionDataForDesignElementAtIndex(int designElementIndex) throws AtlasDataException {
        return readFloatValuesForRowIndex(dataNetCDF, designElementIndex, "BDC");
    }

    /**
     * Extracts a matrix of expression values for given design element indices.
     *
     * @param deIndices an array of design element indices to get expression values for
     * @return a float matrix - a list of expressions per design element index
     * @throws AtlasDataException if the expression data could not be read from the dataNetCDF file
     * @throws AtlasDataException if the file doesn't contain given deIndices
     */
    @Override
    FloatMatrixProxy getExpressionValues(int[] deIndices) throws AtlasDataException {
        return readFloatValuesForRowIndices(dataNetCDF, deIndices, "BDC");
    }

    @Override
    TwoDFloatArray getAllExpressionData() throws AtlasDataException {
        return readFloatValuesForAllRows(dataNetCDF, "BDC");
    }

    /**
     * Extracts T-statistic matrix for given design element indices.
     *
     * @param deIndices an array of design element indices to extract T-statistic for
     * @return matrix of floats - an array of T-statistic values per each design element index
     * @throws StatisticsNotFoundException  if statisticsNetCDF does not exist
     * @throws AtlasDataException           if the data could not be read from the dataNetCDF file
     * @throws AtlasDataException           if array of design element indices contains out of bound indices
     */
    @Override
    FloatMatrixProxy getTStatistics(int[] deIndices) throws AtlasDataException, StatisticsNotFoundException {
        if (statisticsNetCDF == null) {
            throw new StatisticsNotFoundException("Statistics file does not exist");
        }
        return readFloatValuesForRowIndices(statisticsNetCDF, deIndices, "TSTAT");
    }

    @Override
    float[] getTStatisticsForDesignElement(int designElementIndex) throws AtlasDataException, StatisticsNotFoundException {
        if (statisticsNetCDF == null) {
            throw new StatisticsNotFoundException("Statistics file does not exist");
        }
        return readFloatValuesForRowIndex(statisticsNetCDF, designElementIndex, "TSTAT");
    }

    @Override
    TwoDFloatArray getTStatistics() throws AtlasDataException, StatisticsNotFoundException {
        if (statisticsNetCDF == null) {
            throw new StatisticsNotFoundException("Statistics file does not exist");
        }
        return readFloatValuesForAllRows(statisticsNetCDF, "TSTAT");
    }

    /**
     * Extracts P-value matrix for given design element indices.
     *
     * @param deIndices an array of design element indices to extract P-values for
     * @return matrix of floats - an array of  P-values per each design element index
     * @throws StatisticsNotFoundException  if statisticsNetCDF does not exist
     * @throws AtlasDataException           if the data could not be read from the statisticsNetCDF file
     * @throws AtlasDataException           if array of design element indices contains out of bound indices
     */
    @Override
    FloatMatrixProxy getPValues(int[] deIndices) throws AtlasDataException, StatisticsNotFoundException {
        if (statisticsNetCDF == null) {
            throw new StatisticsNotFoundException("Statistics file does not exist");
        }
        return readFloatValuesForRowIndices(statisticsNetCDF, deIndices, "PVAL");
    }

    @Override
    float[] getPValuesForDesignElement(int designElementIndex) throws AtlasDataException, StatisticsNotFoundException {
        if (statisticsNetCDF == null) {
            throw new StatisticsNotFoundException("Statistics file does not exist");
        }
        return readFloatValuesForRowIndex(statisticsNetCDF, designElementIndex, "PVAL");
    }

    @Override
    TwoDFloatArray getPValues() throws AtlasDataException, StatisticsNotFoundException {
        if (statisticsNetCDF == null) {
            throw new StatisticsNotFoundException("Statistics file does not exist");
        }
        return readFloatValuesForAllRows(statisticsNetCDF, "PVAL");
    }

    /**
     * Closes the proxied NetCDF file
     *
     * @throws java.io.IOException on close errors
     */
    public void close() throws IOException {
        if (dataNetCDF != null) {
            dataNetCDF.close();
        }
        if (statisticsNetCDF != null) {
            statisticsNetCDF.close();
        }
    }
}
