/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.utils.dsv;

import com.google.common.base.Function;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Olga Melnichuk
 */
public class DsvRowIteratorTest {

    @Test
    public void testNoColumns() {
        final int n = 1;
        final String[] inVals = new String[]{"a", "b", "c"};

        DsvRowIterator<String[]> iter = new DsvRowIterator<String[]>(newIterator(n, inVals), 1);
        assertEquals(n, iter.getTotalRowCount());
        assertTrue(iter.getColumnNames().isEmpty());
        assertTrue(iter.getColumnDescriptions().isEmpty());
        assertTrue(iter.hasNext());
        assertTrue(iter.next().isEmpty());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testColumnWithNullNameOrDescription() {
        DsvRowIterator<String[]> iter = new DsvRowIterator<String[]>(newIterator(0, ""), 0);
        iter.addColumn(null, null, new Function<String[], String>() {
            @Override
            public String apply(@Nullable String[] input) {
                return input[0];
            }
        });

        List<String>names = iter.getColumnNames();
        assertArrayEquals(new String[]{""}, names.toArray(new String[names.size()]));

        List<String>descriptions = iter.getColumnDescriptions();
        assertArrayEquals(new String[]{""}, descriptions.toArray(new String[descriptions.size()]));
    }

    @Test
    public void testColumns() {
        final int n = 1;
        final String[] inVals = new String[]{"a", "b", "c"};

        DsvRowIterator<String[]> iter = new DsvRowIterator<String[]>(newIterator(n, inVals), 1);
        iter.addColumn("a", "", new Function<String[], String>() {
            @Override
            public String apply(@Nullable String[] input) {
                return input[0];
            }
        });
        iter.addColumn("b", "", new Function<String[], String>() {
            @Override
            public String apply(@Nullable String[] input) {
                return input[1];
            }
        });
        iter.addColumn("c", "", new Function<String[], String>() {
            @Override
            public String apply(@Nullable String[] input) {
                return input[2];
            }
        });

        assertArrayEquals(inVals, iter.getColumnNames().toArray(new String[inVals.length]));
        assertTrue(iter.hasNext());

        List<String> outVals = iter.next();
        assertTrue(!outVals.isEmpty());
        assertArrayEquals(inVals, outVals.toArray(new String[inVals.length]));
    }

    private Iterator<String[]> newIterator(final int n, final String... vals) {
        return new Iterator<String[]>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < n;
            }

            @Override
            public String[] next() {
                i++;
                return vals;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

}
