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

import org.junit.Test;
import uk.ac.ebi.gxa.service.experiment.ExperimentAnalytics;
import uk.ac.ebi.gxa.utils.dsv.DsvRowIterator;
import uk.ac.ebi.microarray.atlas.model.UpDownExpression;

import java.util.List;

import static java.util.Arrays.asList;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

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
    public void testOneRow() {
        ExperimentAnalytics analytics = createMock(ExperimentAnalytics.class);
        expect(analytics.size())
                .andReturn(1)
                .anyTimes();
        expect(analytics.getRows())
                .andReturn(asList(row));
        replay(analytics);

        DsvRowIterator<ExperimentAnalytics.TableRow> iter = ExperimentTableDsv.createDsvDocument(analytics);
        assertTrue(iter.hasNext());

        assertTrue(!iter.getColumnNames().isEmpty());
        assertEquals(0, iter.getColumnNames().size() - iter.getColumnDescriptions().size());

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
}
