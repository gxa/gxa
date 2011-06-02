/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

import ucar.nc2.Variable;

/**
 * A proxy for double index array to work around missing values in NetCDF files.
 * <p/>
 * Note: we must remove this class as soon as there is a single meaning of missing value in NetCDF files
 * (see ticket #2910).
 *
 * @author Olga Melnichuk
 */
public class FloatMatrixProxy {

    private final NetCDFMissingVal missVal;

    private final float[][] matrix;

    public FloatMatrixProxy(Variable variable, float[][] result) {
        this.missVal = NetCDFMissingVal.forVariable(variable);
        this.matrix = result;
    }

    public float get(int i, int j) {
        float v = matrix[i][j];
        return missVal.isMissVal(v) ? Float.NaN : v;
    }

    public float[][] asMatrix() {
        final int rowNumber = matrix.length;
        final int colNumber = rowNumber != 0 ? matrix[0].length : 0;
        float[][] copy = new float[rowNumber][colNumber];
        for (int r = 0; r < rowNumber; r++) {
            for (int c = 0; c < colNumber; c++) {
                copy[r][c] = get(r, c);
            }
        }
        return copy;
    }
}
