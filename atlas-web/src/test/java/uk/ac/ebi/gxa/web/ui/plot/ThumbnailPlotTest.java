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

import org.easymock.EasyMock;
import org.junit.Test;
import uk.ac.ebi.gxa.data.AtlasDataException;
import uk.ac.ebi.gxa.data.ExperimentPart;
import uk.ac.ebi.gxa.data.ExpressionValue;
import uk.ac.ebi.gxa.data.StatisticsNotFoundException;

import java.util.*;

import static java.util.Arrays.asList;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

/**
 * @author Olga Melnichuk
 */
public class ThumbnailPlotTest {
    private static List<ExpressionValue> EXPRESSION_VALUES = new ArrayList<ExpressionValue>();
    private static String EFV = "EFV2";
    private static int X_MAX_VALUE = 50;
    private static int X_MIN_VALUE = 1;
    private static float Y_MAX_VALUE;
    private static float Y_MIN_VALUE;

    static {
        Random rnd = new Random(1234);
        Float min = null, max = null;
        for (int i = X_MIN_VALUE, j = 0; i <= X_MAX_VALUE; i++) {
            if (i % 10 == 0) {
                j++;
            }
            float v = i % 5 == 0 ? Float.NaN : 1 + rnd.nextFloat() * 5;
            EXPRESSION_VALUES.add(new ExpressionValue("EFV" + j, v));
            if (!Float.isNaN(v)) {
                max = max == null ? v : Math.max(max, v);
                min = min == null ? v : Math.min(min, v);
            }
        }
        Y_MAX_VALUE = max;
        Y_MIN_VALUE = min;
    }

    @Test
    public void testDEExpressionsThumbnailPlot() throws AtlasDataException, StatisticsNotFoundException {
        Map<String, Object> map = ThumbnailPlot.create(
                createExperimentPart(), "DE-ACCESSION", "EF", EFV).asMap();
        assertDataSeriesEquals(EXPRESSION_VALUES, map.get("series"));
        assertDataSeriesHasEFVMarkings(getEFVMarkings(EXPRESSION_VALUES, EFV), map.get("options"));
    }

    @Test
    public void testBestGeneExpressionsThumbnailPlot() throws AtlasDataException, StatisticsNotFoundException {
        Map<String, Object> map = ThumbnailPlot.create(
                createExperimentPart(), 0L, "EF", EFV).asMap();
        assertDataSeriesEquals(EXPRESSION_VALUES, map.get("series"));
        assertDataSeriesHasEFVMarkings(getEFVMarkings(EXPRESSION_VALUES, EFV), map.get("options"));
    }

    @Test
    public void testPlotScale() throws AtlasDataException, StatisticsNotFoundException {
        int width = X_MAX_VALUE / 2;
        int height = Math.round(Y_MAX_VALUE / 2);

        Map<String, Object> map = ThumbnailPlot
                .create(createExperimentPart(), "DE-ACCESSION", "EF", EFV)
                .scale(width, height)
                .asMap();

        float kx = (1.0F * width / Math.abs(X_MAX_VALUE - X_MIN_VALUE));
        float ky = (1.0F * height / Math.abs(Y_MAX_VALUE - Y_MIN_VALUE));

        assertDataSeriesInRange((int) (X_MIN_VALUE * kx), (int) (X_MAX_VALUE * kx), Y_MIN_VALUE * ky, Y_MAX_VALUE * ky, map.get("series"));

        int[] markings = getEFVMarkings(EXPRESSION_VALUES, EFV);
        int[] scaledMarkings = new int[]{(int) (markings[0] * kx), (int) (markings[1] * kx)};
        assertDataSeriesHasEFVMarkings(scaledMarkings, map.get("options"));
    }

    @Test
    public void testRange() {
        checkRange(asList(1.0f),
                asList(1.0f));
        checkRange(asList(1.0f, 2.0f),
                asList(1.0f, 2.0f));
        checkRange(asList(2.0f, 1.0f),
                asList(2.0f, 1.0f));
        checkRange(asList(1.0f, 2.0f, 3.0f),
                asList(1.0f, 3.0f));
        checkRange(asList(3.0f, 1.0f, 2.0f),
                asList(3.0f, 1.0f));
    }

    private void checkRange(List<Float> values, List<Float> expected) {
        int x = 1;

        List<ThumbnailPlot.Point> before = new ArrayList<ThumbnailPlot.Point>();
        for (Float v : values) {
            before.add(new ThumbnailPlot.Point(x, v));
        }

        List<ThumbnailPlot.Point> after = ThumbnailPlot.range(before);
        assertEquals(expected.size(), after.size());

        int i = 0;
        for (ThumbnailPlot.Point p : after) {
            assertEquals(new ThumbnailPlot.Point(x, expected.get(i++)), p);
        }
    }

    private int[] getEFVMarkings(List<ExpressionValue> values, String efv) {
        int startMark = -1, endMark = -1;
        int i = 1;
        for (ExpressionValue ev : values) {
            if (ev.getEfv().equals(efv)) {
                startMark = startMark < 0 ? i : startMark;
                endMark = i;
            }
            i++;
        }
        return new int[]{startMark, endMark};
    }

    private void assertDataSeriesHasEFVMarkings(int[] markings, Object options) {
        assertArrayEquals(markings, extractEFVMarkings(options));
    }

    private void assertDataSeriesInRange(int x1, int x2, float y1, float y2, Object series) {
        Collection<List<Number>> data = extractData(series);
        for (List<Number> lst : data) {
            assertEquals(2, lst.size());
            int x = lst.get(0).intValue();
            assertTrue(x >= x1 && x <= x2);

            Number n = lst.get(1);
            if (n == null) {
                continue;
            }
            float y = n.floatValue();
            assertTrue(y >= y1 && y <= y2);
        }
    }

    private void assertDataSeriesEquals(List<ExpressionValue> expressionValues, Object series) {
        Collection<List<Number>> data = extractData(series);
        assertEquals(EXPRESSION_VALUES.size(), data.size());

        int i = 0;
        for (List<Number> lst : data) {
            assertEquals(2, lst.size());
            int x = lst.get(0).intValue();
            assertEquals(i + 1, x);

            float v = expressionValues.get(i).getValue();
            Number y = lst.get(1);
            if (y == null) {
                assertTrue(Float.isNaN(v));
            } else {
                assertEquals(v, y.floatValue(), 1e-10);
            }
            i++;
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<List<Number>> extractData(Object series) {
        assertNotNull(series);
        assertTrue(series instanceof Collection);

        Collection<Object> tmp1 = ((Collection<Object>) series);
        assertEquals(1, tmp1.size());

        Object tmp2 = tmp1.iterator().next();
        assertTrue(tmp2 instanceof Map);

        Object data = ((Map<String, Object>) tmp2).get("data");
        assertNotNull(data);
        assertTrue(data instanceof Collection);

        return (Collection<List<Number>>) data;
    }

    @SuppressWarnings("unchecked")
    private int[] extractEFVMarkings(Object options) {
        assertNotNull(options);
        assertTrue(options instanceof Map);

        Object grid = ((Map<String, Object>) options).get("grid");
        assertNotNull(grid);
        assertTrue(grid instanceof Map);

        Object markings = ((Map<String, Object>) grid).get("markings");
        assertNotNull(markings);
        assertTrue(markings instanceof Collection);

        Collection<Object> tmp = ((Collection<Object>) markings);
        assertEquals(1, tmp.size());

        Object tmp1 = tmp.iterator().next();
        assertTrue(tmp1 instanceof Map);

        Object tmp2 = ((Map<String, Object>) tmp1).get("xaxis");
        assertNotNull(tmp2);
        assertTrue(tmp2 instanceof Map);

        Map<String, Object> xaxis = (Map<String, Object>) tmp2;
        Object from = xaxis.get("from");
        Object to = xaxis.get("to");
        assertNotNull(from);
        assertNotNull(to);
        return new int[]{(Integer) from, (Integer) to};
    }

    private ExperimentPart createExperimentPart() throws AtlasDataException, StatisticsNotFoundException {
        final ExperimentPart experimentPart = createMock(ExperimentPart.class);
        expect(experimentPart.getDeExpressionValues(EasyMock.<String>anyObject(), EasyMock.<String>anyObject()))
                .andReturn(EXPRESSION_VALUES).anyTimes();
        expect(experimentPart.getBestGeneExpressionValues(EasyMock.anyLong(), EasyMock.<String>anyObject(), EasyMock.<String>anyObject()))
                .andReturn(EXPRESSION_VALUES).anyTimes();
        replay(experimentPart);
        return experimentPart;
    }
}
