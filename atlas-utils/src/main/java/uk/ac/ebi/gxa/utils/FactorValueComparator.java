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

package uk.ac.ebi.gxa.utils;

import com.google.common.base.Strings;

import java.io.Serializable;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compares two strings by numeric prefix or suffix.
 * <p/>
 * While
 * - An empty string is considered greater than non-empty one.
 * - If the numeric parts are equals the strings are compared alphabetically ignoring the case.
 * - If at list one string doesn't contain a numeric part then both strings are compared alphabetically ignoring the case.
 *
 * @author Olga Melnichuk
 */
public class FactorValueComparator implements Serializable, Comparator<String> {

    private static final Pattern STARTS_OR_ENDS_WITH_DIGITS = Pattern.compile("^\\d+|\\d+$");

    @Override
    public int compare(String s1, String s2) {
        boolean isEmptyS1 = Strings.isNullOrEmpty(s1);
        boolean isEmptyS2 = Strings.isNullOrEmpty(s2);

        if (isEmptyS1 && isEmptyS2) {
            return 0;
        }

        if (isEmptyS1) {
            return 1;
        }

        if (isEmptyS2) {
            return -1;
        }

        Matcher m1 = STARTS_OR_ENDS_WITH_DIGITS.matcher(s1);
        Matcher m2 = STARTS_OR_ENDS_WITH_DIGITS.matcher(s2);

        if (m1.find() && m2.find()) {
            Long i1 = new Long(m1.group(0));
            Long i2 = new Long(m2.group(0));

            int compareRes = i1.compareTo(i2);
            if (compareRes != 0) {
                return compareRes;
            }
        }

        return s1.compareToIgnoreCase(s2);
    }
}
