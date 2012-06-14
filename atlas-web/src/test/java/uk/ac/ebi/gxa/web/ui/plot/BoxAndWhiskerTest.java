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

package uk.ac.ebi.gxa.web.ui.plot;

import org.junit.Test;
import uk.ac.ebi.gxa.data.DataMatrixStorage;

import java.util.Random;

import static com.google.common.primitives.Floats.asList;
import static java.util.Collections.shuffle;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.ac.ebi.microarray.atlas.model.UpDownExpression.NONDE;

/**
 * @author Olga Melnichuk
 */
public class BoxAndWhiskerTest {

    private static final double E = 1.0e-6;

    private static final Random RANDOM = new Random(12345L);

    @Test
    public void testEmptyData() {
        BoxAndWhisker plot = newBoxAndWhisker();
        assertTrue(Float.isNaN(plot.getMax()));
        assertTrue(Float.isNaN(plot.getMedian()));
        assertTrue(Float.isNaN(plot.getMin()));
        assertTrue(Float.isNaN(plot.getLowerQuartile()));
        assertTrue(Float.isNaN(plot.getUpperQuartile()));
    }

    @Test
    public void testNonEmptyData() {
        checkBoxAndWhisker(
                newBoxAndWhisker(1.0f),
                1.0, 1.0, 1.0, 1.0, 1.0
        );

        checkBoxAndWhisker(
                newBoxAndWhisker(1.0f, 2.0f),
                1.0, 1.0, 1.0, 1.0, 2.0
        );

        checkBoxAndWhisker(
                newBoxAndWhisker(1.0f, 1.0f, 2.0f, 2.0f, DataMatrixStorage.NA_VAL),
                1.0, 1.0, 1.0, 2.0, 2.0
        );
    }

    private void checkBoxAndWhisker(BoxAndWhisker box, double min, double lq, double median, double uq, double max) {
        assertEquals(box.getMin(), min, E);
        assertEquals(box.getMax(), max, E);
        assertEquals(box.getLowerQuartile(), lq, E);
        assertEquals(box.getUpperQuartile(), uq, E);
        assertEquals(box.getMedian(), median, E);
    }

    private static BoxAndWhisker newBoxAndWhisker(float... data) {
        shuffle(asList(data), RANDOM);
        return new BoxAndWhisker(data, NONDE);
    }
}
