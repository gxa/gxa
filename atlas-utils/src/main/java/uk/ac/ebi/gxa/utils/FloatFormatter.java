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

import java.text.DecimalFormat;

public abstract class FloatFormatter {
    private FloatFormatter() {
    }

    private static DecimalFormat[] formatsCache = new DecimalFormat[10];

    private static DecimalFormat decimalFormat(int numberOfDigits) {
        DecimalFormat format = null;
        if (numberOfDigits < 10) {
            format = formatsCache[numberOfDigits];
        }
        if (format == null) {
            final StringBuilder s = new StringBuilder("#.");
            for (int i = 0; i < numberOfDigits; ++i) {
                s.append("#");
            }
            format = new DecimalFormat(s.toString());
            if (numberOfDigits < 10) {
                formatsCache[numberOfDigits] = format;
            }
        }
        return format;
    }
 
    public static String formatFloat(float value, int significantDigits) {
        if (Float.isInfinite(value)) {
            return "null";
        }
        if (Float.isNaN(value)) {
            return "null";
        }
        return decimalFormat(
            Math.max(0, significantDigits - (int)Math.ceil(Math.log10(Math.abs(value))))
        ).format(value);
    }

    public static String formatDouble(double value, int significantDigits) {
        if (Double.isInfinite(value)) {
            return "null";
        }
        if (Double.isNaN(value)) {
            return "null";
        }
        return decimalFormat(
            Math.max(0, significantDigits - (int)Math.ceil(Math.log10(Math.abs(value))))
        ).format(value);
    }
}
