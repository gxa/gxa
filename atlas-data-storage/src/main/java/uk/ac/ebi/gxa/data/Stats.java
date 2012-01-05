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

package uk.ac.ebi.gxa.data;

import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.UpDownExpression;

import static uk.ac.ebi.microarray.atlas.model.UpDownExpression.valueOf;

/**
 * @author alf
 */
public class Stats {
    public final Pair<String, String> efv;
    public final float p;
    public final float t;
    public final int uefvidx;
    public final int deidx;

    public Stats(Pair<String, String> efv, float p, float t, int uefvidx, int deidx) {
        this.efv = efv;
        this.p = p;
        this.t = t;
        this.uefvidx = uefvidx;
        this.deidx = deidx;
    }

    public DesignElementStatistics asDEStats() {
        return new DesignElementStatistics(p, t, deidx, uefvidx, efv);
    }

    public UpDownExpression expression() {
        return valueOf(p, t);
    }
}
