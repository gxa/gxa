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

package uk.ac.ebi.gxa.netcdf;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;

public final class TwoDFloatArray {
    private final Array array;
    private final int[] shape;

    TwoDFloatArray(Array array) {
        this.array = array;
        this.shape = new int[] {1, array.getShape()[1]};
    }

    public float[] getRow(int index) {
        final int[] origin = {index, 0};
        try {
            return (float[])array.section(origin, shape).get1DJavaArray(float.class);
        } catch (InvalidRangeException e) {
            return new float[0];
        }
    }
}
