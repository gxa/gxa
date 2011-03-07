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

import java.util.Collection;
import java.util.Collections;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Iterables.partition;
import static java.lang.Character.isUpperCase;


public final class StringUtil {
    private StringUtil() {
        // utility class
    }

    public static String quoteComma(String value) {
        return value.contains(",") ? "\"" + value + "\"" : value;
    }

    public static String pluralize(String value) {
        return value.endsWith("s") ? value : value + "s";
    }

    public static String decapitalise(String value) {
        return value.length() > 1 && !isUpperCase(value.charAt(1)) ? value.toLowerCase() : value;
    }

    public static String replaceLast(String value, String old, String replaceWith) {
        if (value.endsWith(old))
            return value.substring(0, value.lastIndexOf(old)) + replaceWith;
        else
            return value;
    }

    public static String upcaseFirst(String string) {
        if (isNullOrEmpty(string) || isUpperCase(string.charAt(0))) {
            return string;
        }

        return string.substring(0, 1).toUpperCase() + (string.length() > 1 ? string.substring(1) : "");
    }

    public static String limitedJoin(Collection<String> strings, int num, String separator, String etc) {
        final String joined = on(separator).join(getFirst(partition(strings, num), Collections.<String>emptyList()));
        return strings.size() > num ? joined + etc : joined;
    }
}
