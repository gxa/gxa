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

import uk.ac.ebi.gxa.data.DataMatrixStorage;

import com.google.common.annotations.VisibleForTesting;
import org.codehaus.jackson.annotate.JsonProperty;
import uk.ac.ebi.gxa.data.StatisticsCursor;
import uk.ac.ebi.microarray.atlas.model.UpDownExpression;

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * @author Olga Melnichuk
 */
public class BoxAndWhisker {

    private final float median;
    private final float upperQuartile;
    private final float lowerQuartile;
    private final float max;
    private final float min;
    private final UpDownExpression expression;

    @VisibleForTesting
    BoxAndWhisker(float[] data, @Nonnull UpDownExpression upDown) {
        Arrays.sort(data);
        this.median = percentile(data, 0.5);
        this.max = percentile(data, 1.0);
        this.min = getMin(data);
        this.upperQuartile = percentile(data, 0.75);
        this.lowerQuartile = percentile(data, 0.25);
        expression = upDown;
    }

    public BoxAndWhisker(StatisticsCursor statistics) {
        this(statistics.getRawExpression(), statistics.getExpression());
    }

    private float percentile(float[] sortedList, double rank) {
        return sortedList.length == 0 ? Float.NaN :
                round(sortedList[(int) ((sortedList.length - 1) * rank)]);
    }

    private float round(float v) {
        return Math.round(v * 100) / 100.0f;
    }

    @JsonProperty("median")
    public float getMedian() {
        return median;
    }

    @JsonProperty("uq")
    public float getUpperQuartile() {
        return upperQuartile;
    }

    @JsonProperty("lq")
    public float getLowerQuartile() {
        return lowerQuartile;
    }

    @JsonProperty("max")
    public float getMax() {
        return max;
    }

    @JsonProperty("min")
    public float getMin() {
        return min;
    }

    @JsonProperty("up")
    public boolean isUp() {
        return expression.isUp();
    }

    @JsonProperty("down")
    public boolean isDown() {
        return expression.isDown();
    }

    private float getMin(float[] data) {
        for (int i = 0; i < data.length; i++) {
            if (data[i] != DataMatrixStorage.NA_VAL)
                return data[i];
        }
        return Float.NaN;
    }
}
