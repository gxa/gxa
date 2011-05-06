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
import uk.ac.ebi.gxa.netcdf.reader.UpDownExpression;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Olga Melnichuk
 */
public class BoxAndWhisker {

    private final float median;
    private final float uq;
    private final float lq;
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
        this.uq = percentile(list, 0.75);
        this.lq = percentile(list, 0.25);
        this.up = upDown != null && upDown.isUp();
        this.down = upDown != null && upDown.isDown();
    }

    private float percentile(List<Float> sortedList, double rank) {
        return round(sortedList.get((int) ((sortedList.size() - 1) * rank)));
    }

    private float round(float v) {
        return Math.round(v * 100) / 100.0f;
    }

    public float getMedian() {
        return median;
    }

    public float getUq() {
        return uq;
    }

    public float getLq() {
        return lq;
    }

    public float getMax() {
        return max;
    }

    public float getMin() {
        return min;
    }

    public boolean isUp() {
        return up;
    }

    public boolean isDown() {
        return down;
    }
}
