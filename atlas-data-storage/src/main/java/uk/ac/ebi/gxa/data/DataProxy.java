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

import java.util.List;
import java.io.Closeable;

interface DataProxy extends Closeable {
    String getVersion();

    String getExperimentAccession() throws AtlasDataException;
    String getArrayDesignAccession() throws AtlasDataException;
    int[][] getSamplesToAssays() throws AtlasDataException;
    /**
     * Gets the array of gene IDs from this data source
     *
     * @return an long[] representing the one dimensional array of gene identifiers
     */
    long[] getGenes() throws AtlasDataException;
    String[] getDesignElementAccessions() throws AtlasDataException;
    // TODO: remove 'public' modifier
    public String[] getAssayAccessions() throws AtlasDataException;
    String[] getSampleAccessions() throws AtlasDataException;
    String[] getFactors() throws AtlasDataException;
    String[] getCharacteristics() throws AtlasDataException;
    String[] getFactorValues(String factor) throws AtlasDataException;
    /**
     * Returns the whole matrix of factor values for assays (|Assay| X |EF|).
     *
     * @return an array of strings - an array of factor values per assay
     * @throws AtlasDataException if data could not be read form the netCDF file
     */
    String[][] getFactorValues() throws AtlasDataException;
    String[] getCharacteristicValues(String characteristic) throws AtlasDataException;
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
    float[] getExpressionDataForDesignElementAtIndex(int designElementIndex) throws AtlasDataException;
    /**
     * Extracts a matrix of expression values for given design element indices.
     *
     * @param deIndices an array of design element indices to get expression values for
     * @return a float matrix - a list of expressions per design element index
     * @throws AtlasDataException    if the expression data could not be read from the netCDF file
     * @throws AtlasDataException    if the file doesn't contain given deIndices
     */
    FloatMatrixProxy getExpressionValues(int[] deIndices) throws AtlasDataException;
    TwoDFloatArray getAllExpressionData() throws AtlasDataException;

    List<KeyValuePair> getUniqueValues() throws AtlasDataException, StatisticsNotFoundException;
    FloatMatrixProxy getTStatistics(int[] deIndices) throws AtlasDataException, StatisticsNotFoundException;
    float[] getTStatisticsForDesignElement(int designElementIndex) throws AtlasDataException, StatisticsNotFoundException;
    TwoDFloatArray getTStatistics() throws AtlasDataException, StatisticsNotFoundException;

    FloatMatrixProxy getPValues(int[] deIndices) throws AtlasDataException, StatisticsNotFoundException;
    float[] getPValuesForDesignElement(int designElementIndex) throws AtlasDataException, StatisticsNotFoundException;
    TwoDFloatArray getPValues() throws AtlasDataException, StatisticsNotFoundException;
}
