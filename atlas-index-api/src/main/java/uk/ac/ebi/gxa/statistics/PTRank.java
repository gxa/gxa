package uk.ac.ebi.gxa.statistics;

import javax.annotation.concurrent.Immutable;
import java.io.Serializable;

import static java.lang.Float.compare;
import static java.lang.Float.isNaN;
import static java.lang.Math.abs;
import static java.lang.Math.round;

/**
 * This class is used as a key in SortedMaps to achieve sorting (by pval/tstat rank) of experiments for an OR list attributes (c.f. Statistics class)
 */
@Immutable
public final class PTRank implements Serializable, Comparable<PTRank> {
    private static final long serialVersionUID = 201106071204L;
    public static final float PRECISION = 1e-3F;
    // pValue rounded off to 3 decimal places - c.f. PRECISION
    private final float pValue;
    // For the definition of tStat rank see #getTStatRank()
    private final short tStatRank;

    private PTRank(float pValue, short tStatRank) {
        this.pValue = pValue;
        this.tStatRank = tStatRank;
    }

    public static PTRank of(float p, float t) {
        return new PTRank(roundToPrecision(p), getTStatRank(t));
    }

    public float getPValue() {
        return pValue;
    }

    public short getTStatRank() {
        return tStatRank;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PTRank ptRank = (PTRank) o;
        return compare(ptRank.pValue, pValue) == 0 && tStatRank == ptRank.tStatRank;
    }

    @Override
    public int hashCode() {
        int result = (pValue != +0.0f ? Float.floatToIntBits(pValue) : 0);
        result = 31 * result + (int) tStatRank;
        return result;
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
    public int compareTo(PTRank o) {
        if (abs(getTStatRank()) != abs(o.getTStatRank())) {
            return abs(o.getTStatRank()) - abs(getTStatRank()); // higher absolute value of tStatRank comes first
        }

        if (isNaN(getPValue()) || getPValue() > 1) // NA pVal for this experiment
            return 1; // the other PTRank comes first

        if (isNaN(o.getPValue()) || o.getPValue() > 1) // NA pVal for the compared experiment
            return -1; // this PTRank comes first

        if (compare(getPValue(), o.getPValue()) == 0)
            // if pvals are different, return the lower actual value of tStatRank
            // (arbitrary if it's lower or higher here - it's just that one of them has to come first)
            return getTStatRank() - o.getTStatRank();

        return compare(getPValue(), o.getPValue()); // lower pVals come first
    }


    /**
     * @param t thr T statistic value to convert into a roughly resembling it <code>short</code>
     * @return tStat ranks as follows:
     *         t =<  -9       -> rank: -10
     *         t in <-6, -9)  -> rank: -7
     *         t in <-3, -6)  -> rank: -4
     *         t in (-3,  0)  -> rank: -1
     *         t == 0         -> rank:  0
     *         t in ( 0,  3)  -> rank:  1
     *         t in < 3,  6)  -> rank:  4
     *         t in < 6,  9)  -> rank:  7
     *         t >=   9       -> rank:  10
     *         Note that the higher the absolute value of tStat (rank) the better the tStat.
     */
    private static short getTStatRank(float t) {
        // TODO: 4alf: what about NaN?
        if (t <= -9) {
            return -10;
        } else if (t <= -6) {
            return -7;
        } else if (t <= -3) {
            return -4;
        } else if (t < 0) {
            return -1;
        } else if (t == 0) {
            return 0;
        } else if (t < 3) {
            return 1;
        } else if (t < 6) {
            return 4;
        } else if (t < 9) {
            return 7;
        } else {
            return 10;
        }
    }

    private static float roundToPrecision(float value) {
        return round(value / PRECISION) * PRECISION;
    }
}
