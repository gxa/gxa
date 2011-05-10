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

package uk.ac.ebi.gxa.plot;

import com.google.common.collect.Lists;
import org.codehaus.jackson.annotate.JsonProperty;
import uk.ac.ebi.microarray.atlas.model.UpDownExpression;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Olga Melnichuk
 */
public class BoxAndWhisker {

    private final float median;
    private final float upperQuartile;
    private final float lowerQuartile;
    private final float max;
    private final float min;
    private final boolean up;
    private final boolean down;

    public BoxAndWhisker(Collection<Float> data, @Nullable UpDownExpression upDown) {
        List<Float> list = Lists.newArrayList(data);
        Collections.sort(list);
        this.median = percentile(list, 0.5);
        this.max = percentile(list, 1.0);
        this.min = percentile(list, 0.0);
        this.upperQuartile = percentile(list, 0.75);
        this.lowerQuartile = percentile(list, 0.25);
        this.up = upDown != null && upDown.isUp();
        this.down = upDown != null && upDown.isDown();
    }

    private float percentile(List<Float> sortedList, double rank) {
        return round(sortedList.get((int) ((sortedList.size() - 1) * rank)));
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
        return up;
    }

    @JsonProperty("down")
    public boolean isDown() {
        return down;
    }
}
