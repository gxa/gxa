/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

import uk.ac.ebi.gxa.exceptions.LogUtil;

import javax.annotation.concurrent.Immutable;

import static java.lang.Float.compare;
import static java.lang.Math.abs;
import static uk.ac.ebi.microarray.atlas.model.UpDownExpression.isPvalValid;
import static uk.ac.ebi.microarray.atlas.model.UpDownExpression.isTStatValid;

/**
 * This class is used as an element in Collections sorted by accurate (as stored in ncdfs) pval/tstat, specifically in
 * the experiment page's best design elements table
 *
 * @author Robert Petryszak
 */
@Immutable
public final class DesignElementStatistics implements Comparable<DesignElementStatistics> {
    private final float pValue;
    private final float tStat;
    private final int deIndex;
    private final int uEFVIndex;

    public DesignElementStatistics(float pValue, float tStat, int deIndex, int uEFVIndex) {
        if (!isPvalValid(pValue))
            throw LogUtil.createUnexpected("Invalid pValue: " + pValue);
        if (!isTStatValid(tStat))
            throw LogUtil.createUnexpected("Invalid tStatistic: " + tStat);

        this.pValue = pValue;
        this.tStat = tStat;
        this.deIndex = deIndex;
        this.uEFVIndex = uEFVIndex;
    }

    public float getPValue() {
        return pValue;
    }

    public float getTStat() {
        return tStat;
    }

    public int getDEIndex() {
        return deIndex;
    }

    public int getUEFVIndex() {
        return uEFVIndex;
    }

    @Override
    public int hashCode() {
        int result = (getPValue() != +0.0f ? Float.floatToIntBits(getPValue()) : 0);
        result = 31 * result + (getTStat() != +0.0f ? Float.floatToIntBits(getTStat()) : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DesignElementStatistics pt = (DesignElementStatistics) o;
        return compare(getPValue(), pt.getPValue()) == 0 && compare(getTStat(), pt.getTStat()) == 0;
    }

    /**
     * Defines natural order descending by absolute value of T first, then ascending by P
     * <p/>
     * Note that there is one case when <code>compareTo(DesignElementStatistics)</code>
     * is NOT compatible with {@link #equals(Object)}: it happens is P values are equals, and T values are opposite.
     *
     * @param o the DE candidate to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified candidate.
     */
    public int compareTo(DesignElementStatistics o) {
        int result = -compare(abs(getTStat()), abs(o.getTStat()));
        return result != 0 ? result : compare(getPValue(), o.getPValue());
    }

    @Override
    public String toString() {
        return "DesignElementStatistics{" +
                "pValue=" + pValue +
                ", tStat=" + tStat +
                ", deIndex=" + deIndex +
                ", uEFVIndex=" + uEFVIndex +
                '}';
    }
}
