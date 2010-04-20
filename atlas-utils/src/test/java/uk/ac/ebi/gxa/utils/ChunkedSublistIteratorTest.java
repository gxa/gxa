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

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ChunkedSublistIteratorTest {
    @Test
    public void test_SublistIterator() {
        List<Integer> list = Arrays.asList(1,2,3,4,5, 6,7,8,9,10, 11);
        ChunkedSublistIterator<List<Integer>> iterator = new ChunkedSublistIterator<List<Integer>>(list, 5);

        int counter = 0;

        while(iterator.hasNext()) {
            List<Integer> sublist = iterator.next();
            if(counter < 2)
                assertEquals(sublist.size(), 5);
            if(counter == 2)
                assertEquals(sublist.size(), 1);

            counter++;
        }

        assertEquals(3, counter);
    }

}
