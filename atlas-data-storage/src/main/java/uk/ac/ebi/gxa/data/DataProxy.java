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

import uk.ac.ebi.gxa.utils.Pair;

import java.io.Closeable;
import java.util.List;

/**
 * Common interface for various NetCDF files
 * <p/>
 * This interface encapsulates the differences between v1 and v2 of our NetCDFs,
 * but is still very NetCDF-oriented: everything is in parallel arrays, and if you
 * want to use this interface directly (generally do not use it, we're going to provide
 * proper wrappers hiding parallel structures from developers,
 * see e.g. already-available {@link StatisticsCursor}.
 * <p/>
 * It is highly recommended to build a proper domain-model-based wrapper over the whole
 * data set. {@link ExperimentPart} and {@link ExperimentWithData} are a good start,
 * even though they are not finished yed.
 * <p/>
 * The litmus test for the wrapper is that structure-describing methods
 * (like {@link #getSamplesToAssays()}, {@link #getFactorValues()})
 * should not leave this layer. We'd also prefer to avoid any indices to leave
 * this layer&mdash;unfortunately, so far we cannot replace DE indexes with DE
 * accessions (there might be a similar problem with assays, yet to be investigated).
 */
interface DataProxy extends Closeable {
    String getVersion();

    String getExperimentAccession();

    String getArrayDesignAccession();

    int[][] getSamplesToAssays() throws AtlasDataException;

    /**
     * Gets the array of gene IDs from this data source
     *
     * @return an long[] representing the one dimensional array of gene identifiers
     */
    long[] getGenes() throws AtlasDataException;

    String[] getDesignElementAccessions() throws AtlasDataException;

    String[] getAssayAccessions() throws AtlasDataException;

    String[] getFactors() throws AtlasDataException;

    String[] getCharacteristics() throws AtlasDataException;

    String[] getFactorValues(String factor) throws AtlasDataException;

    /**
     * Returns the whole matrix of factor values for assays (|EF| X |Assay|).
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
     * @throws AtlasDataException if the expression data could not be read from the netCDF file
     *                            or the file doesn't contain given deIndices
     */
    FloatMatrixProxy getExpressionValues(int[] deIndices) throws AtlasDataException;

    FloatMatrixProxy getAllExpressionData() throws AtlasDataException;

    List<Pair<String, String>> getUniqueEFVs() throws AtlasDataException, StatisticsNotFoundException;

    FloatMatrixProxy getTStatistics() throws AtlasDataException, StatisticsNotFoundException;

    FloatMatrixProxy getPValues() throws AtlasDataException, StatisticsNotFoundException;
}
