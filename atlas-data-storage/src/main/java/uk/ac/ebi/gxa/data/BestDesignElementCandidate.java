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
 * @author Robert Petryszak
 *         Date: 11/24/11
 *         <p/>
 *         This class is used as an element in Collections sorted by accurate (as stored in ncdfs) pval/tstat, specifically in the experiment page's best design elements table
 */
public final class BestDesignElementCandidate implements Comparable<BestDesignElementCandidate>{
    Float pValue;
    Float tStat;
    Integer deIndex;
    Integer uEFVIndex;

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
     * 1. If absolute tStat ranks differ, return the higher tStatRank first
     * 2. If absolute tStat ranks are the same:
     * a. if pVals are different, return the lower pVal first
     * b. if pVals are the same, return the lower actual (i.e. non-absolute) tStatRank first
     * <p/>
     * NB. The ordering maintained by a set must be consistent with equals if it is to correctly implement the Set interface. This is so because the Set i/f is
     * defined in terms of the equals operation, but a TreeSet instance (c.f. Statistics.pValuesTStatRanks map) performs all element comparisons using its compareTo
     * method, so two elements that are deemed equal by this method are, from the standpoint of the set, equal.
     * <p/>
     * 2c. serves to ensure that contract between compareTo() and equal() is preserved, but also ensures that two PvalTstatRanks (pVal=0 tStatRank=3) and (pVal=0 tStatRank=-3)
     * sre stored as two distinct objects in a TreeSet. This is because PTRank class serves a dual function:
     * - from UP/DOWN Atlas data ordering point of view, (pVal=0 tStatRank=3) and (pVal=0 tStatRank=-3) are equivalent
     * - from UP vs DOWN point of view they are not.
     * Hence, the only way I could think of for supporting both requirements in Statistics.pValuesTStatRanks was to store both PvalTstatRanks as distinct from each other,
     * and yet co-collacted in the TreeSet used in the map.
     *
     * @param o the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified object.
     * @throws ClassCastException if the specified object's type prevents it
     *                            from being compared to this object.
     */
    public int compareTo(BestDesignElementCandidate o) {
        int tStatDiff = compare(abs(o.getTStat()), abs(getTStat())); // higher absolute value of tStatRank comes first
        if (tStatDiff != 0) {
            return tStatDiff;
        }

        if (isNaN(getPValue()) || getPValue() > 1) // NA pVal for this experiment
            return 1; // the other PTRank comes first

        if (isNaN(o.getPValue()) || o.getPValue() > 1) // NA pVal for the compared experiment
            return -1; // this PTRank comes first

        if (compare(getPValue(), o.getPValue()) == 0)
            // if pvals are the same, return the this PT first
            // (arbitrary which we return - it's just that one of them has to come first)
            return -1;

        return compare(getPValue(), o.getPValue()); // lower pVals come first
    }
}
