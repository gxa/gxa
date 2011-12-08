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

import static java.lang.Float.compare;
import static java.lang.Float.isNaN;
import static java.lang.Math.abs;

/**
 * This class is used as an element in Collections sorted by accurate (as stored in ncdfs) pval/tstat, specifically in
 * the experiment page's best design elements table
 *
 * @author Robert Petryszak
 */
public final class BestDesignElementCandidate implements Comparable<BestDesignElementCandidate> {
    private Float pValue;
    private Float tStat;
    private Integer deIndex;
    private Integer uEFVIndex;

    public BestDesignElementCandidate(Float pValue, Float tStat, Integer deIndex, Integer uEFVIndex) {
        this.pValue = pValue;
        this.tStat = tStat;
        this.deIndex = deIndex;
        this.uEFVIndex = uEFVIndex;
    }

    public Float getPValue() {
        return pValue;
    }

    public Float getTStat() {
        return tStat;
    }

    public Integer getDEIndex() {
        return deIndex;
    }

    public Integer getUEFVIndex() {
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

        BestDesignElementCandidate pt = (BestDesignElementCandidate) o;
        return compare(pt.getPValue(), getPValue()) == 0 && compare(getTStat(), pt.getTStat()) == 0;
    }

    /**
     * 1. If absolute tStat ranks differ, return the higher tStat first
     * 2. If absolute tStat ranks are the same:
     * a. if pVals are different, return the lower pVal first
     * b. if pVals are the same, return this BestDesignElementCandidate first
     *
     * @param o the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified object.
     * @throws ClassCastException if the specified object's type prevents it from being compared to this object.
     */
    public int compareTo(BestDesignElementCandidate o) {
        int tStatDiff = compare(abs(o.getTStat()), abs(getTStat())); // higher absolute value of tStatRank comes first
        if (tStatDiff != 0) {
            return tStatDiff;
        }

        if (isNaN(getPValue()) || getPValue() > 1) // NA pVal for this experiment
            return 1; // the other BestDesignElementCandidate comes first

        if (isNaN(o.getPValue()) || o.getPValue() > 1) // NA pVal for the compared experiment
            return -1; // this BestDesignElementCandidate comes first

        if (compare(getPValue(), o.getPValue()) == 0)
            // if pvals are the same, return the this PT first
            // (arbitrary which we return - it's just that one of them has to come first)
            return -1;

        return compare(getPValue(), o.getPValue()); // lower pVals come first
    }
}
