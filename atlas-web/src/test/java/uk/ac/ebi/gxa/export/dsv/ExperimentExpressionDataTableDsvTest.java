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

package uk.ac.ebi.gxa.export.dsv;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.junit.Test;
import uk.ac.ebi.gxa.data.ExpressionDataCursor;
import uk.ac.ebi.gxa.utils.dsv.DsvColumn;
import uk.ac.ebi.gxa.utils.dsv.DsvRowIterator;

import javax.annotation.Nullable;
import java.util.*;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static uk.ac.ebi.gxa.export.dsv.ExperimentExpressionDataTableDsv.createDsvDocument;

/**
 * @author Olga Melnichuk
 */
public class ExperimentExpressionDataTableDsvTest {
    private static final Map<String, Float> assays = new LinkedHashMap<String, Float>();
    private static final float[] expectedValues = new float[]{1, 2, 3};
    private static final List<String> columnNames = new ArrayList<String>();

    static {
        assays.put("A1", expectedValues[0]);
        assays.put("A2", expectedValues[1]);
        assays.put("A3", expectedValues[2]);

        columnNames.addAll(columnNames(ExperimentExpressionDataTableDsv.permanentColumns()));
        columnNames.addAll(assays.keySet());
    }

    @Test
    public void testEmptyExpressionData() {
        DsvRowIterator<ExpressionDataCursor> iter =
                createDsvDocument(mockExpressionDataCursor(null));

        assertFalse(iter.hasNext());
        assertEquals(iter.getColumnNames(), columnNames);
    }

    @Test
    public void testNonEmptyExpressionData() {
        DsvRowIterator<ExpressionDataCursor> iter =
                createDsvDocument(mockExpressionDataCursor("DE"));

        assertTrue(iter.hasNext());
        assertEquals(iter.getColumnNames(), columnNames);

        List<String> values = iter.next();
        assertListContains(values, asListOfStrings(expectedValues));
    }

    private static List<String> asListOfStrings(float[] in) {
        List<String> out = new ArrayList<String>();
        for (float f : in) {
            out.add(Float.toString(f));
        }
        return out;
    }

    private static Collection<String> columnNames(Collection<DsvColumn<ExpressionDataCursor>> columns) {
        return Collections2.transform(columns, new Function<DsvColumn<ExpressionDataCursor>, String>() {
            @Override
            public String apply(@Nullable DsvColumn<ExpressionDataCursor> input) {
                return input.getName();
            }
        });
    }

    private static ExpressionDataCursor mockExpressionDataCursor(String deAccession) {
        ExpressionDataCursor cursor = createMock(ExpressionDataCursor.class);

        Collection<String> assayAccs = assays.keySet();

        expect(cursor.getAssayAccessions())
                .andReturn(assayAccs.toArray(new String[assayAccs.size()]));

        expect(cursor.getDeCount())
                .andReturn(deAccession == null ? 0 : 1)
                .anyTimes();

        expect(cursor.hasNextDE())
                .andReturn(deAccession != null)
                .anyTimes();

        if (deAccession != null) {
            expect(cursor.getDeAccession())
                    .andReturn(deAccession)
                    .anyTimes();
            expect(cursor.getValues())
                    .andReturn(expectedValues)
                    .anyTimes();
            expect(cursor.nextDE())
                    .andReturn(true)
                    .anyTimes();
        }
        replay(cursor);
        return cursor;
    }

    private static void assertListContains(List<String> list, List<String> expectedValues) {
        for(String v : expectedValues) {
            assertTrue(list.contains(v));
        }
    }
}
