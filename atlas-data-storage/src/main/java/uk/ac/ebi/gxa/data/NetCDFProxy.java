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
 * interface class for read access to NetCDF files
 *   has different implementations for NetCDF v1 (1 file for data and statistics) and
 *   NetCDF v2 (separate files for data and statistics)
 */

public abstract class NetCDFProxy implements Closeable {
    public static final String NCDF_PROP_VAL_SEP_REGEX = "\\|\\|";

    // N/A value in netCDFs
    public static final float NA_PVAL_TSTAT = 1e+30f;

    // methods to be implemented in all implementations
    abstract String getExperimentAccession();
    abstract String getArrayDesignAccession();
    abstract int[][] getSamplesToAssays() throws IOException;
    /**
     * @param iAssay
     * @return List of sample indexes corresponding to assay at index iAssay
     * @throws IOException
     */
    abstract List<Integer> getSamplesForAssay(int iAssay) throws IOException;
    /**
     * Gets the array of gene IDs from this NetCDF
     *
     * @return an long[] representing the one dimensional array of gene identifiers
     * @throws IOException if accessing the NetCDF failed
     */
    abstract long[] getGenes() throws IOException;
    abstract String[] getDesignElementAccessions() throws IOException;
    // TODO: remove 'public' modifier
    public abstract String[] getAssayAccessions() throws IOException;
    abstract String[] getSampleAccessions() throws IOException;
    abstract String[] getFactors() throws IOException;
    abstract String[] getCharacteristics() throws IOException;
    abstract String[] getFactorValues(String factor) throws IOException;
    /**
     * Returns the whole matrix of factor values for assays (|Assay| X |EF|).
     *
     * @return an array of strings - an array of factor values per assay
     * @throws IOException if data could not be read form the netCDF file
     */
    abstract String[][] getFactorValues() throws IOException;
    abstract List<KeyValuePair> getUniqueValues() throws IOException, AtlasDataException;
    abstract List<KeyValuePair> getUniqueFactorValues() throws IOException, AtlasDataException;
    abstract String[] getCharacteristicValues(String characteristic) throws IOException;
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
    abstract float[] getExpressionDataForDesignElementAtIndex(int designElementIndex) throws IOException, AtlasDataException;
    /**
     * Extracts a matrix of expression values for given design element indices.
     *
     * @param deIndices an array of design element indices to get expression values for
     * @return a float matrix - a list of expressions per design element index
     * @throws IOException           if the expression data could not be read from the netCDF file
     * @throws InvalidRangeException if the file doesn't contain given deIndices
     */
    abstract FloatMatrixProxy getExpressionValues(int[] deIndices) throws IOException, AtlasDataException;
    abstract TwoDFloatArray getAllExpressionData() throws IOException;
    /**
     * @return List of genes found in the proxy
     * @throws java.io.IOException in case of I/O errors during reading
     */
    @Nonnull
    abstract List<Long> getGeneIds() throws IOException;
    /**
     * Extracts T-statistic matrix for given design element indices.
     *
     * @param deIndices an array of design element indices to extract T-statistic for
     * @return matrix of floats - an array of T-statistic values per each design element index
     * @throws IOException           if the data could not be read from the netCDF file
     * @throws InvalidRangeException if array of design element indices contains out of bound indices
     */
    abstract FloatMatrixProxy getTStatistics(int[] deIndices) throws IOException, AtlasDataException;
    abstract float[] getTStatisticsForDesignElement(int designElementIndex) throws IOException, AtlasDataException;
    abstract TwoDFloatArray getTStatistics() throws IOException;
    /**
     * Extracts P-value matrix for given design element indices.
     *
     * @param deIndices an array of design element indices to extract P-values for
     * @return matrix of floats - an array of  P-values per each design element index
     * @throws IOException           if the data could not be read from the netCDF file
     * @throws InvalidRangeException if array of design element indices contains out of bound indices
     */
    abstract FloatMatrixProxy getPValues(int[] deIndices) throws IOException, AtlasDataException;
    abstract float[] getPValuesForDesignElement(int designElementIndex) throws IOException, AtlasDataException;
    abstract TwoDFloatArray getPValues() throws IOException;

    // utility methods to be used in implementations
    protected long[] getLongArray1(NetcdfFile netCDF, String variableName) throws IOException {
        final Variable var = netCDF.findVariable(variableName);
        if (var == null) {
            return new long[0];
        } else {
            return (long[])var.read().get1DJavaArray(long.class);
        }
    }

    protected float[] readFloatValuesForRowIndex(NetcdfFile netCDF, int rowIndex, String variableName) throws IOException, AtlasDataException {
        Variable variable = netCDF.findVariable(variableName);
        if (variable == null) {
            return new float[0];
        }

        int[] shape = variable.getShape();
        int[] origin = {rowIndex, 0};
        int[] size = new int[]{1, shape[1]};
        try {
            return (float[]) variable.read(origin, size).get1DJavaArray(float.class);
        } catch (InvalidRangeException e) {
            throw new AtlasDataException(e);
        }
    }

    protected FloatMatrixProxy readFloatValuesForRowIndices(NetcdfFile netCDF, int[] rowIndices, String varName) throws IOException, AtlasDataException {
        try {
            Variable variable = netCDF.findVariable(varName);
            int[] shape = variable.getShape();

            float[][] result = new float[rowIndices.length][shape[1]];

            for (int i = 0; i < rowIndices.length; i++) {
                int[] origin = {rowIndices[i], 0};
                int[] size = new int[]{1, shape[1]};
                result[i] = (float[]) variable.read(origin, size).get1DJavaArray(float.class);
            }
            return new FloatMatrixProxy(variable, result);
        } catch (InvalidRangeException e) {
            throw new AtlasDataException(e);
        }
    }

    protected String getGlobalAttribute(NetcdfFile netCDF, String attribute) {
        ucar.nc2.Attribute a = netCDF.findGlobalAttribute(attribute);
        return null == a ? null : a.getStringValue();
    }

    protected String[] getArrayOfStrings(NetcdfFile netCDF, String variable) throws IOException {
        if (netCDF.findVariable(variable) == null) {
            return new String[0];
        }
        ArrayChar deacc = (ArrayChar) netCDF.findVariable(variable).read();
        ArrayChar.StringIterator si = deacc.getStringIterator();
        String[] result = new String[deacc.getShape()[0]];
        for (int i = 0; i < result.length && si.hasNext(); ++i)
            result[i] = si.next();
        return result;
    }

    protected String[] getFactorsCharacteristics(NetcdfFile netCDF, String varName) throws IOException {
        if (netCDF.findVariable(varName) == null) {
            return new String[0];
        }

        // create a array of characters from the varName dimension
        ArrayChar efs = (ArrayChar) netCDF.findVariable(varName).read();
        // convert to a string array and return
        Object[] efsArray = (Object[]) efs.make1DStringArray().get1DJavaArray(String.class);
        String[] result = new String[efsArray.length];
        for (int i = 0; i < efsArray.length; i++) {
            result[i] = (String) efsArray[i];
            if (result[i].startsWith("ba_"))
                result[i] = result[i].substring(3);
        }
        return result;
    }

    protected Integer findEfIndex(String factor) throws IllegalArgumentException, IOException {
        String[] efs = getFactors();
        for (int i = 0; i < efs.length; i++) {
            // todo: note flexible matching for ba_<factor> or <factor> - this is hack to work around old style netcdfs
            if (factor.matches("(ba_)?" + efs[i])) {
                return i;
            }
        }
        return null;
    }

    protected Integer findScIndex(String factor) throws IOException {
        String[] scs = getCharacteristics();
        for (int i = 0; i < scs.length; i++) {
            // todo: note flexible matching for bs_<factor> or <factor> - this is hack to work around old style netcdfs
            if (factor.matches("(bs_)?" + scs[i])) {
                return i;
            }
        }
        return null;
    }

    //read variable as 3D array of chars, and return
    //slice (by dimension = 0) at index as array of strings
    protected String[] getSlice3D(NetcdfFile netCDF, String variable, int index) throws IOException {
        // if the EFV variable is empty
        if (netCDF.findVariable(variable) == null) {
            return new String[0];
        }
        // now we have index of our ef, so take a read from efv for this index
        Array efvs = netCDF.findVariable(variable).read();
        // slice this array on dimension '0' (this is EF dimension), retaining only these efvs ordered by assay
        ArrayChar ef_efv = (ArrayChar) efvs.slice(0, index);

        // convert to a string array and return
        Object[] ef_efvArray = (Object[]) ef_efv.make1DStringArray().get1DJavaArray(String.class);
        String[] result = new String[ef_efvArray.length];
        for (int i = 0; i < ef_efvArray.length; i++) {
            result[i] = (String) ef_efvArray[i];
        }
        return result;
    }

    protected TwoDFloatArray readFloatValuesForAllRows(NetcdfFile netCDF, String varName) throws IOException {
        final Variable variable = netCDF.findVariable(varName);
        return new TwoDFloatArray(variable != null ? (ArrayFloat.D2)variable.read() : new ArrayFloat.D2(0, 0));
    }
}
