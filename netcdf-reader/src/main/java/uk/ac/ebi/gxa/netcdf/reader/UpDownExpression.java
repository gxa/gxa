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

/**
 * This is the list of gene expression levels based on P and T statistic that could be read from the netCDF files.
 *
 * @author Olga Melnichuk
 *         Date: 21/04/2011
 */
public enum UpDownExpression {
    UP,
    DOWN,
    NON_D_E,
    NA;

    public boolean isUp() {
        return this == UP;
    }

    public boolean isDown() {
        return this == DOWN;
    }

    public static boolean isUp(float p, float t) {
        return valueOf(p, t).isUp();
    }

    public static boolean isDown(float p, float t) {
        return valueOf(p, t).isDown();
    }

    public static UpDownExpression valueOf(float p, float t) {
        if (Float.isNaN(p) || Float.isNaN(t)) {
            return NA;
        }

        boolean passesPValueCutoff = p <=  0.05;
        boolean hasPositiveTstat = t > 0;
        boolean hasNegativeTstat = t < 0;

        if (passesPValueCutoff && hasPositiveTstat) {
            return UP;
        }

        if (passesPValueCutoff && hasNegativeTstat) {
            return DOWN;
        }

        return NON_D_E;
    }

}
