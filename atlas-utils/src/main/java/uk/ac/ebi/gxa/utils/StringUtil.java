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

import com.google.common.base.Strings;

import java.util.Collection;


public final class StringUtil {
    private StringUtil() {
        // utility class
    }

    public static String quoteComma(String value) {
        if (value.contains(","))
            return "\"" + value + "\"";
        else
            return value;
    }

    public static String pluralize(String value) {
        if (!value.endsWith("s"))
            return value + "s";
        else
            return value;
    }

    public static String decapitalise(String value) {

        if (value.length() > 1)
            if (!Character.isUpperCase(value.charAt(1)))
                value = value.toLowerCase();

        return value;
    }

    public static String replaceLast(String value, String OldValue, String NewValue) {
        if (value.endsWith(OldValue))
            return value.substring(0, value.lastIndexOf(OldValue)) + NewValue;
        else
            return value;
    }

    public static String upcaseFirst(String string) {
        if (string.length() > 1)
            return string.substring(0, 1).toUpperCase() + string.substring(1, string.length()).toLowerCase();
        else
            return string.toUpperCase();
    }

    public static String limitedJoin(Collection objs, int num, String separator, String etc) {
        StringBuilder sb = new StringBuilder();
        for (Object o : objs) {
            if (sb.length() > 0)
                sb.append(separator);
            if (num == 0) {
                sb.append(etc);
                break;
            }
            sb.append(o);
            --num;
        }
        return sb.toString();
    }
}
