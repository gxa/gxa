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
import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;

/**
 * interface class for read access to NetCDF files
 * has different implementations for NetCDF v1 (1 file for data and statistics) and
 * NetCDF v2 (separate files for data and statistics)
 */

abstract class NetCDFProxy implements DataProxy {
    static final String NCDF_PROP_VAL_SEP_REGEX = "\\|\\|";

    // utility methods to be used in implementations
    long[] getLongArray1(NetcdfFile netCDF, String variableName) throws AtlasDataException {
        try {
            final Variable var = netCDF.findVariable(variableName);
            if (var == null) {
                return new long[0];
            } else {
                return (long[]) var.read().get1DJavaArray(long.class);
            }
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    float[] readFloatValuesForRowIndex(NetcdfFile netCDF, int rowIndex,
                                       String variableName) throws AtlasDataException {
        try {
            Variable variable = netCDF.findVariable(variableName);
            if (variable == null) {
                return new float[0];
            }

            int[] shape = variable.getShape();
            int[] origin = {rowIndex, 0};
            int[] size = new int[]{1, shape[1]};
            return (float[]) variable.read(origin, size).get1DJavaArray(float.class);
        } catch (IOException e) {
            throw new AtlasDataException(e);
        } catch (InvalidRangeException e) {
            throw new AtlasDataException(e);
        }
    }

    FloatMatrixProxy readFloatValuesForRowIndices(NetcdfFile netCDF, int[] rowIndices,
                                                  String varName) throws AtlasDataException {
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
        } catch (IOException e) {
            throw new AtlasDataException(e);
        } catch (InvalidRangeException e) {
            throw new AtlasDataException(e);
        }
    }

    String[] getArrayOfStrings(NetcdfFile netCDF, String variable) throws AtlasDataException {
        try {
            if (netCDF.findVariable(variable) == null) {
                return new String[0];
            }
            ArrayChar deacc = (ArrayChar) netCDF.findVariable(variable).read();
            ArrayChar.StringIterator si = deacc.getStringIterator();
            String[] result = new String[deacc.getShape()[0]];
            for (int i = 0; i < result.length && si.hasNext(); ++i)
                result[i] = si.next();
            return result;
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    String[] getFactorsCharacteristics(NetcdfFile netCDF, String varName) throws AtlasDataException {
        try {
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
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    Integer findEfIndex(String factor) throws AtlasDataException {
        String[] efs = getFactors();
        for (int i = 0; i < efs.length; i++) {
            // todo: note flexible matching for ba_<factor> or <factor> - this is hack to work around old style netcdfs
            if (factor.matches("(ba_)?" + efs[i])) {
                return i;
            }
        }
        return null;
    }

    Integer findScIndex(String factor) throws AtlasDataException {
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
    String[] getSlice3D(NetcdfFile netCDF, String variableName, int index) throws AtlasDataException {
        try {
            final Variable var = netCDF.findVariable(variableName);
            // if the EFV variable is empty
            if (var == null) {
                return new String[0];
            }
            // now we have index of our ef, so take a read from efv for this index
            Array efvs = var.read();
            // slice this array on dimension '0' (this is EF dimension), retaining only these efvs ordered by assay
            ArrayChar ef_efv = (ArrayChar) efvs.slice(0, index);

            // convert to a string array and return
            Object[] ef_efvArray = (Object[]) ef_efv.make1DStringArray().get1DJavaArray(String.class);
            String[] result = new String[ef_efvArray.length];
            for (int i = 0; i < ef_efvArray.length; i++) {
                result[i] = (String) ef_efvArray[i];
            }
            return result;
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }

    TwoDFloatArray readFloatValuesForAllRows(NetcdfFile netCDF, String varName) throws AtlasDataException {
        try {
            final Variable variable = netCDF.findVariable(varName);
            return new TwoDFloatArray(variable != null ? (ArrayFloat.D2) variable.read() : new ArrayFloat.D2(0, 0));
        } catch (IOException e) {
            throw new AtlasDataException(e);
        }
    }
}
