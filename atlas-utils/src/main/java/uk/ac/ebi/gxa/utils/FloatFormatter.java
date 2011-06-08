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

package uk.ac.ebi.gxa.utils;

import static java.lang.Math.*;

public final class FloatFormatter {
    private FloatFormatter() {
    }

    public static String formatFloat(float value, int significantDigits) {
        if (Float.isInfinite(value)) {
            return "null";
        }
        if (Float.isNaN(value)) {
            return "null";
        }
        return Double.toString(trimSignificantDigits(value, significantDigits));
    }

    public static String formatDouble(double value, int significantDigits) {
        if (Double.isInfinite(value)) {
            return "null";
        }
        if (Double.isNaN(value)) {
            return "null";
        }
        return Double.toString(trimSignificantDigits(value, significantDigits));
    }

    private static double trimSignificantDigits(double value, int significantDigits) {
        if (abs(value) < pow(10.0, -significantDigits))
            return 0;

        int order = (int) ceil(log10(abs(value)));
        final double precision = pow(10.0, order - significantDigits);
        return round(value / precision) * precision;
    }
}
