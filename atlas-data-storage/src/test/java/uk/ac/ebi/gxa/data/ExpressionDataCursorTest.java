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

package uk.ac.ebi.gxa.data;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

/**
 * @author Olga Melnichuk
 */
public class ExpressionDataCursorTest {

    private static String[] assayAccessions = new String[]{"A", "B", "C", "D"};
    private static String[] deAccessions = new String[]{"DE1", "DE2", "DE3"};

    private float[][] expressions;

    @Before
    public void init() {
        Random r = new Random(12345L);
        expressions = new float[assayAccessions.length][deAccessions.length];
        for (int i = 0; i < assayAccessions.length; i++) {
            for (int j = 0; j < deAccessions.length; j++) {
                expressions[i][j] = r.nextFloat();
            }
        }
    }

    @Test
    public void testEmptyExpressionData() throws AtlasDataException, StatisticsNotFoundException {
        ExpressionDataCursor cursor = new ExpressionDataCursor(emptyDataProxy(new int[0]));
        assertEquals(0, cursor.getDeCount());
        assertFalse(cursor.hasNextDE());
        assertFalse(cursor.nextDE());
        try {
            cursor.getDeAccession();
            fail("ArrayIndexOutOfBoundException expected");
        } catch (ArrayIndexOutOfBoundsException e) {
            //OK
        }
        assertNull(cursor.getValues());

        cursor = new ExpressionDataCursor(emptyDataProxy(new int[0]), new int[]{1, 2, 3});
        assertEquals(0, cursor.getDeCount());
        assertFalse(cursor.hasNextDE());
        assertFalse(cursor.nextDE());
        try {
            cursor.getDeAccession();
            fail("ArrayIndexOutOfBoundException expected");
        } catch (ArrayIndexOutOfBoundsException e) {
            //OK
        }
        assertNull(cursor.getValues());
    }

    @Test
    public void testAllExpressionData() throws AtlasDataException, StatisticsNotFoundException {
        ExpressionDataCursor cursor = new ExpressionDataCursor(nonEmptyDataProxy(new int[0]));
        assertEquals(deAccessions.length, cursor.getDeCount());
        for(int d=0; d<deAccessions.length; d++) {
            assertTrue(cursor.hasNextDE());
            assertTrue(cursor.nextDE());
            assertEquals(deAccessions[d], cursor.getDeAccession());
            float[] values = cursor.getValues();
            float[] expected = expressions[d];
            assertArrayEquals(expected, values, 0.00001f);
        }
        assertFalse(cursor.hasNextDE());
        assertFalse(cursor.nextDE());
    }

    @Test
    public void testSelectedExpressionData() throws AtlasDataException, StatisticsNotFoundException {
        int[] selected = new int[]{0,2,1};
        ExpressionDataCursor cursor = new ExpressionDataCursor(nonEmptyDataProxy(selected), selected);
        assertEquals(deAccessions.length, cursor.getDeCount());
        for (int d : selected) {
            assertTrue(cursor.hasNextDE());
            assertTrue(cursor.nextDE());
            assertEquals(deAccessions[d], cursor.getDeAccession());
            float[] values = cursor.getValues();
            float[] expected = expressions[d];
            assertArrayEquals(expected, values, 0.00001f);
        }
        assertFalse(cursor.hasNextDE());
        assertFalse(cursor.nextDE());
    }

    private DataProxy emptyDataProxy(int[] deIndices) throws AtlasDataException, StatisticsNotFoundException {
        return createDataProxy(new String[0], new String[0], new float[0][0], deIndices);
    }

    private DataProxy nonEmptyDataProxy(int[] deIndices) throws AtlasDataException, StatisticsNotFoundException {
        return createDataProxy(deAccessions, assayAccessions, expressions, deIndices);
    }

    private DataProxy createDataProxy(String[] deAccessions, String[] assayAccessions, float[][] expressions, int[] deIndices)
            throws AtlasDataException, StatisticsNotFoundException {
        final DataProxy proxy = createMock(DataProxy.class);
        expect(proxy.getDesignElementAccessions()).andReturn(deAccessions).once();
        expect(proxy.getAssayAccessions()).andReturn(assayAccessions);
        expect(proxy.getAllExpressionData()).andReturn(floatMatrix(expressions));
        expect(proxy.getExpressionData(EasyMock.<int[]>anyObject())).andReturn(floatMatrix(subset(expressions, deIndices)));

        replay(proxy);
        return proxy;
    }

    private float[][] subset(float[][] in, int[] select) {
        float[][] out = new float[select.length][];
        for (int i = 0; i < select.length; i++) {
            int row = select[i];
            out[i] = in[row];
        }
        return out;
    }

    private FloatMatrixProxy floatMatrix(float[][] array) {
        return new FloatMatrixProxy(array, missVal());
    }

    private NetCDFMissingVal missVal() {
        final NetCDFMissingVal missingVal = createMock(NetCDFMissingVal.class);
        expect(missingVal.isMissVal(anyFloat())).andReturn(false).anyTimes();
        replay(missingVal);
        return missingVal;
    }
}
