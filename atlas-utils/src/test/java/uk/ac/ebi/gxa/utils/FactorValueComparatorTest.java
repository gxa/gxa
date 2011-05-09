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

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Olga Melnichuk
 */
public class FactorValueComparatorTest {

    private static final Comparator<String> comparator = new FactorValueComparator();
    private static final Random RANDOM = new Random(12345L);

    @Test
    public void testTwoValuesComparison() {
        eq(null, null);
        eq("", null);
        eq("", "");
        eq("abc", "abc");
        eq("abc", "ABc");
        eq("123", "123");

        eq("12abc", "12ABC");
        eq("abc12", "ABC12");

        gt(null, "a");
        lt("a", null);

        gt("", "a");
        lt("a", "");

        gt("10", "9");
        lt("9", "10");

        gt("b", "a");
        lt("a", "b");

        gt("12a", "11b");
        lt("11b", "12a");

        gt("12b", "b11");
        lt("b11", "12b");

        gt("a", "1");
        lt("1", "a");

        gt("a1", "1a");
        lt("1a", "a1");

        gt("a1", "a");
        lt("a", "a1");

        gt("10a", "9a");
        lt("9a", "10a");

        gt("ab", "a");
        lt("a", "ab");

        gt("ab", "a1");
        lt("a1", "ab");

        gt("ab", "1a");
        lt("1a", "ab");
    }

    @Test
    public void testCollectionSort() {
        arrayCheck("10ab", "12abc", "ab", "ABC", "abc122", "abc123", "abcd", "", "");
        arrayCheck("0 abc", "2 abc", "5 abc", "9 abc", "12 abc", "234 abc");
        arrayCheck("abc 0", "abc 2", "abc 5", "abc 9", "abc 12", "abc 234");
        arrayCheck("abc 0", "2 abc", "abc 5", "9 abc", "abc 12", "234 abc");
    }

    private static void eq(String o1, String o2) {
        assertTrue(comparator.compare(o1, o2) == 0);
    }

    private static void gt(String o1, String o2) {
        assertTrue(comparator.compare(o1, o2) > 0);
    }

    private static void lt(String o1, String o2) {
        assertTrue(comparator.compare(o1, o2) < 0);
    }


    private static void arrayCheck(String... s) {
        assertArrayEquals(s, sort(s));
    }

    private static String[] sort(String[] s) {
        List<String> list = new ArrayList<String>();
        list.addAll(Arrays.asList(s));
        Collections.shuffle(list, RANDOM);
        Collections.sort(list, comparator);
        return list.toArray(new String[list.size()]);
    }
}
