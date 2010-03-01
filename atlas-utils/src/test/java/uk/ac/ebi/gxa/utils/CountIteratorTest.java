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
 * http://ostolop.github.com/gxa/
 */

package uk.ac.ebi.gxa.utils;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Iterator;

/**
 * @author pashky
 */
public class CountIteratorTest {

    @Test
    public void test_zeroTo() {
        int counter = 0;
        Iterator<Integer> counterIter = CountIterator.zeroTo(10);
        while(counterIter.hasNext()) {
            int value = counterIter.next();
            assertEquals(counter, value);
            ++counter;
        }
        assertEquals(10, counter);

        // border conditions
        assertFalse(CountIterator.zeroTo(0).hasNext());
        assertFalse(CountIterator.zeroTo(-1).hasNext());
    }

    @Test
    public void test_oneTo() {
        int counter = 1;
        Iterator<Integer> counterIter = CountIterator.oneTo(10);
        while(counterIter.hasNext()) {
            int value = counterIter.next();
            assertEquals(counter, value);
            ++counter;
        }
        assertEquals(11, counter);

        // border conditions
        assertFalse(CountIterator.oneTo(0).hasNext());
        assertFalse(CountIterator.oneTo(-1).hasNext());
    }
}
