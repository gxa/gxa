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

package uk.ac.ebi.gxa.plot;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Olga Melnichuk
 *         Date: 27/04/2011
 */
public class DoubleIndexIteratorTest {

    @Test
    public void emptyListTest() {
        assertFalse((makeDoubleList(new String[0])).hasNext());
    }

    @Test
    public void simpleListTest() {
        hasExpectedValues(4, makeDoubleList(new String[]{"0:0", "0:1"}, new String[]{"1:0", "1:1"}));
    }

    @Test
    public void moreAdvancedListTest() {
        hasExpectedValues(4, makeDoubleList(new String[]{"0:0", "0:1"}, new String[0], new String[]{"2:0", "2:1"}));
    }

    private void hasExpectedValues(int n, DoubleIndexIterator<String> iter) {
        int counter = 0;
        while (iter.hasNext()) {
            DoubleIndexIterator.Entry<String> entry = iter.next();
            String e = entry.getEntry();
            assertNotNull(e);

            String[] ar = e.split(":");
            assertEquals(ar.length, 2);

            assertEquals(ar[0], "" + entry.getI());
            assertEquals(ar[1], "" + entry.getJ());
            counter++;
        }

        assertEquals(counter, n);
    }

    private DoubleIndexIterator<String> makeDoubleList(String[]... arrays) {
        List<List<String>> list = new ArrayList<List<String>>();
        for (String[] array : arrays) {
            list.add(Arrays.asList(array));
        }
        return new DoubleIndexIterator<String>(list);
    }
}
