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
import org.junit.Test;
import uk.ac.ebi.gxa.service.experiment.ExperimentAnalytics;
import uk.ac.ebi.gxa.utils.dsv.DsvColumn;
import uk.ac.ebi.gxa.utils.dsv.DsvRowIterator;
import uk.ac.ebi.microarray.atlas.model.UpDownExpression;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static com.google.common.collect.Lists.transform;
import static java.util.Arrays.asList;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static uk.ac.ebi.gxa.export.dsv.ExperimentTableDsv.permanentColumns;

/**
 * @author Olga Melnichuk
 */
public class ExperimentTableDsvTest {

    private static final ExperimentAnalytics.TableRow row = new ExperimentAnalytics.TableRow(
            "geneName",
            "geneIdentifier",
            "deAccession",
            1,
            "factor",
            "factorValue",
            1f,
            1f,
            UpDownExpression.UP
    );

    @Test
    public void testEmptyRows() {
        DsvRowIterator<ExperimentAnalytics.TableRow> iter =
                ExperimentTableDsv.createDsvDocument(mockAnalytics(
                        Collections.<ExperimentAnalytics.TableRow>emptyList()));

        assertListEquals(permanentColumnNames(), iter.getColumnNames());
        assertEquals(0, iter.getColumnNames().size() - iter.getColumnDescriptions().size());
        assertEquals(0, iter.getTotalRowCount());
        assertFalse(iter.hasNext());

        try {
            iter.next();
            fail("Iterator should throw NoSuchElementException if there are no more elements");
        } catch (NoSuchElementException e) {
            //OK
        }
    }

    @Test
    public void testOneRow() {
        DsvRowIterator<ExperimentAnalytics.TableRow> iter =
                ExperimentTableDsv.createDsvDocument(mockAnalytics(asList(row)));

        assertListEquals(permanentColumnNames(), iter.getColumnNames());
        assertEquals(0, iter.getColumnNames().size() - iter.getColumnDescriptions().size());
        assertEquals(1, iter.getTotalRowCount());
        assertTrue(iter.hasNext());

        List<String> values = iter.next();
        String[] expected = new String[]{
                row.getGeneName(),
                row.getGeneIdentifier(),
                row.getDeAccession(),
                row.getFactor(),
                row.getFactorValue(),
                row.getUpDown(),
                Float.toString(row.getFloatPValue()),
                Float.toString(row.getFloatTValue())};

        assertArrayEquals(expected, values.toArray(new String[values.size()]));
        assertFalse(iter.hasNext());
    }

    private ExperimentAnalytics mockAnalytics(List<ExperimentAnalytics.TableRow> rows) {
        ExperimentAnalytics analytics = createMock(ExperimentAnalytics.class);
        expect(analytics.size())
                .andReturn(rows.size())
                .anyTimes();
        expect(analytics.getRows())
                .andReturn(rows);
        replay(analytics);
        return analytics;
    }

    private List<String> permanentColumnNames() {
        return transform(permanentColumns(),
                new Function<DsvColumn<ExperimentAnalytics.TableRow>, String>() {
                    @Override
                    public String apply(@Nullable DsvColumn<ExperimentAnalytics.TableRow> column) {
                        return column.getName();
                    }
                });
    }

    private static void assertListEquals(List<String> list1, List<String> list2) {
        assertArrayEquals(list1.toArray(), list2.toArray());
    }
}
