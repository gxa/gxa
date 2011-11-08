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

import com.google.common.collect.Ordering;
import com.google.common.primitives.Longs;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.isNullOrEmpty;
import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

/**
 * Compares two strings by numeric prefix or suffix.
 * <p/>
 * While
 * - An empty string is considered greater than non-empty one.
 * - An empty prefix or suffix considered to be zero.
 * - Strings with equal non-numeric parts are compared by numeric prefixes and suffixes.
 *
 * @author Olga Melnichuk
 */
public class FactorValueOrdering extends Ordering<String> implements Serializable {

    private static final Pattern EFV_PATTERN = java.util.regex.Pattern.compile("^(\\d+)?(.*?)(\\d+)?$");

    @Override
    public int compare(String s1, String s2) {
        boolean isEmptyS1 = isNullOrEmpty(s1);
        boolean isEmptyS2 = isNullOrEmpty(s2);

        if (isEmptyS1 && isEmptyS2) {
            return 0;
        }
        if (isEmptyS1) {
            return 1;
        }
        if (isEmptyS2) {
            return -1;
        }

        Matcher m1 = EFV_PATTERN.matcher(s1);
        Matcher m2 = EFV_PATTERN.matcher(s2);

        if (!m1.matches() || (!m2.matches())) {
            throw createUnexpected("Factor value pattern doesn't match strings: " + s1 + ", " + s2);
        }

        long prefix1 = m1.group(1) == null ? 0 : Long.parseLong(m1.group(1));
        long prefix2 = m2.group(1) == null ? 0 : Long.parseLong(m2.group(1));

        long suffix1 = m1.group(3) == null ? 0 : Long.parseLong(m1.group(3));
        long suffix2 = m2.group(3) == null ? 0 : Long.parseLong(m2.group(3));

        String body1 = m1.group(2).trim();
        String body2 = m2.group(2).trim();

        int cmp = body1.compareTo(body2);
        if (cmp != 0) {
            return cmp;
        }

        if (prefix1 != prefix2) {
            return Longs.compare(prefix1,prefix2);
        }

        return Longs.compare(suffix1, suffix2);
    }
}
