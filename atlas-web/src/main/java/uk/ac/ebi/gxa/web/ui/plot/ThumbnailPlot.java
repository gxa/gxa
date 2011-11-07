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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import uk.ac.ebi.gxa.data.AtlasDataException;
import uk.ac.ebi.gxa.data.ExperimentPart;
import uk.ac.ebi.gxa.data.ExpressionValue;
import uk.ac.ebi.gxa.data.StatisticsNotFoundException;
import uk.ac.ebi.gxa.utils.FactorValueOrdering;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static uk.ac.ebi.gxa.utils.CollectionUtil.makeMap;

/**
 * @author Olga Melnichuk
 */
public class ThumbnailPlot {

    private final List<Point> seriesData;
    private final int startMark;
    private final int endMark;
    private float yScale = 1.0f;
    private float xScale = 1.0f;

    private ThumbnailPlot(@Nonnull List<Point> seriesData, int startMark, int endMark) {
        this.seriesData = seriesData;
        this.startMark = startMark;
        this.endMark = endMark;
    }

    /**
     * Creates a thumbnail plot of design element's expression data. Where values on the x- and y-axis are
     * the experiment assay indices and expression values correspondingly.
     *
     * @param expPart     an experiment coupled with array design
     * @param deAccession a design element accession to create the plot for
     * @param ef          an experiment factor to label values along x-axis
     * @param efv         an experiment factor value to highlight on the plot
     * @return an instance of {@link ThumbnailPlot}
     * @throws AtlasDataException if any problem with retrieving experiment data happens
     */
    public static ThumbnailPlot create(ExperimentPart expPart, String deAccession, String ef, String efv) throws AtlasDataException {
        List<ExpressionValue> expressionValues = expPart.getDeExpressionValues(deAccession, ef);
        return createPlot(expressionValues, efv);
    }

    /**
     * Creates a thumbnail plot of the best design element's expression data. Where values on the x- and y-axis
     * are the experiment assay indices and expression values correspondingly.
     * <p/>
     * The gene parameter specifies the set of design elements to choose the one with the best statistics.
     *
     * @param expPart an experiment coupled with array design
     * @param geneId  a gene id to find design element with the best expression analysis values
     * @param ef      an experiment factor to label values along x-axis
     * @param efv     an experiment factor value to highlight on the plot
     * @return an instance of {@link ThumbnailPlot}
     * @throws AtlasDataException          if any problem with retrieving experiment data happens
     * @throws StatisticsNotFoundException if no statistics found for experiment
     */
    public static ThumbnailPlot create(ExperimentPart expPart, Long geneId, String ef, String efv) throws AtlasDataException, StatisticsNotFoundException {
        List<ExpressionValue> expressionValues = expPart.getBestGeneExpressionValues(geneId, ef, efv);
        return createPlot(expressionValues, efv);
    }

    public ThumbnailPlot scale(int width, int height) {
        if (seriesData.isEmpty()) {
            return this;
        }

        int xMax = seriesData.size();
        boolean found = false;
        float yMin = 0;
        float yMax = 0;
        for (Point p : seriesData) {
            if (!found && !p.isNaN()) {
                yMin = p.y;
                yMax = p.y;
                found = true;
            } else if (!p.isNaN()) {
                yMin = Math.min(yMin, p.y);
                yMax = Math.max(yMax, p.y);
            }
        }

        if (found) {
            xScale = (1.0f * width) / xMax;
            yScale = (1.0f * height) / Math.abs(yMax - yMin);
        }
        return this;
    }

    private Collection<Object> range(List<Point> list) {
        if (list.size() >= 3) {
            Point pMin = null, pMax = null;
            int iMax = 0, iMin = 0, i = 0;
            for (Point p : list) {
                if (pMin == null) {
                    pMin = p;
                    pMax = p;
                    iMin = i;
                    iMax = i;
                } else {
                    if (pMin.y > p.y) {
                        iMin = i;
                        pMin = p;
                    }
                    if (pMax.y < p.y) {
                        iMax = i;
                        pMax = p;
                    }
                }
                i++;
            }
            list = new ArrayList<Point>();
            if (iMax < iMin) {
                list.add(pMax);
                list.add(pMin);
            } else {
                list.add(pMin);
                list.add(pMax);
            }
        }
        return Lists.transform(list, new Function<Point, Object>() {
            @Override
            public Object apply(@Nullable Point input) {
                return input == null ? null : input.asList();
            }
        });
    }

    public Map<String, Object> asMap() {
        List<Object> series = new ArrayList<Object>();

        int xPrev = -1;
        List<Point> subset = new ArrayList<Point>();

        for (Point p : seriesData) {
            Point scaled = p.scale(xScale, yScale);
            if (scaled.x > xPrev || scaled.isNaN()) {
                if (!subset.isEmpty()) {
                    series.addAll(range(subset));
                    subset.clear();
                }
            }
            if (scaled.isNaN()) {
                series.add(scaled.asList());
            } else {
                subset.add(scaled);
            }
            xPrev = scaled.x;
        }

        int startMark = Math.round(this.startMark * xScale);
        int endMark = Math.round(this.endMark * xScale);

        return makeMap(
                "series", Collections.singletonList(makeMap(
                "data", series,
                "lines", makeMap("show", true, "lineWidth", 2, "fill", false),
                "legend", makeMap("show", false))),
                "options", makeMap(
                "xaxis", makeMap("ticks", 0),
                "yaxis", makeMap("ticks", 0),
                "legend", makeMap("show", false),
                "colors", Collections.singletonList("#edc240"),
                "grid", makeMap(
                "backgroundColor", "#f0ffff",
                "autoHighlight", false,
                "hoverable", true,
                "clickable", true,
                "borderWidth", 1,
                "markings", Collections.singletonList(
                makeMap("xaxis", makeMap("from", startMark, "to", endMark),
                        "color", "#F5F5DC"))
        ),
                "selection", makeMap("mode", "x")
        )
        );
    }

    private static ThumbnailPlot createPlot(List<ExpressionValue> expressionValues, String efv) {
        sortByFactorValues(expressionValues);

        List<Point> seriesData = new ArrayList<Point>();
        int startMark = -1;
        int endMark = -1;

        for (ExpressionValue ev : expressionValues) {
            String efvi = ev.getEfv();
            if (efvi.equals(efv)) {
                startMark = startMark < 0 ? seriesData.size() + 1 : startMark;
                endMark = seriesData.size() + 1;
            }

            float value = ev.getValue();
            //TODO move this -1e6 value check to the lower layers
            seriesData.add(new Point(seriesData.size() + 1, value <= -1000000 ? Float.NaN : value));
        }

        return new ThumbnailPlot(seriesData, startMark, endMark);
    }

    private static void sortByFactorValues(List<ExpressionValue> expressions) {
        Collections.sort(expressions, (new FactorValueOrdering()).onResultOf(
                new Function<ExpressionValue, String>() {
                    @Override
                    public String apply(@Nullable ExpressionValue input) {
                        return input == null ? "" : input.getEfv();
                    }
                }
        ));
    }

    private static class Point {
        private final int x;
        private final float y;

        private Point(int x, float y) {
            this.x = x;
            this.y = y;
        }

        private List<Number> asList() {
            return Arrays.<Number>asList(x, isNaN() ? null : y);
        }

        private boolean isNaN() {
            return Float.isNaN(y);
        }

        private Point scale(float xScale, float yScale) {
            return new Point(
                    Math.round(x * xScale),
                    isNaN() ? y : y * yScale);
        }
    }

}