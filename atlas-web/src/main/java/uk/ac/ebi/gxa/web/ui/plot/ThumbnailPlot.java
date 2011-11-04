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
import uk.ac.ebi.gxa.data.AtlasDataException;
import uk.ac.ebi.gxa.data.ExperimentPart;
import uk.ac.ebi.gxa.data.ExpressionValue;
import uk.ac.ebi.gxa.data.StatisticsNotFoundException;
import uk.ac.ebi.gxa.utils.FactorValueOrdering;

import javax.annotation.Nullable;
import java.util.*;

import static uk.ac.ebi.gxa.utils.CollectionUtil.makeMap;

/**
 * @author Olga Melnichuk
 */
public class ThumbnailPlot {

    public static Map<String, Object> create(ExperimentPart expPart, String deAccession, String ef, String efv) throws AtlasDataException {
        List<ExpressionValue> expressionValues = expPart.getDeExpressionValues(deAccession, ef);
        return createPlot(expressionValues, efv);
    }

    public static Map<String, Object> create(ExperimentPart expPart, Long geneId, String ef, String efv) throws AtlasDataException, StatisticsNotFoundException {
        List<ExpressionValue> expressionValues = expPart.getBestGeneExpressionValues(geneId, ef, efv);
        return createPlot(expressionValues, efv);
    }

    private static Map<String, Object> createPlot(List<ExpressionValue> expressionValues, String efv) {
        sortByFactorValues(expressionValues);

        List<Object> seriesData = new ArrayList<Object>();
        int startMark = -1;
        int endMark = -1;

        for (ExpressionValue ev : expressionValues) {
            String efvi = ev.getEfv();
            if (efvi.equals(efv)) {
                startMark = startMark < 0 ? seriesData.size() + 1 : startMark;
                endMark = seriesData.size() + 1;
            }

            float value = ev.getValue();
            seriesData.add(Arrays.<Number>asList(seriesData.size() + 1, value <= -1000000 ? null : value));
        }

        return makeMap(
                "series", Collections.singletonList(makeMap(
                "data", seriesData,
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

}