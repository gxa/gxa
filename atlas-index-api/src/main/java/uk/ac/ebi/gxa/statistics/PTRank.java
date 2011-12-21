package uk.ac.ebi.gxa.statistics;

import uk.ac.ebi.gxa.exceptions.LogUtil;

import javax.annotation.concurrent.Immutable;
import java.io.Serializable;

import static java.lang.Float.compare;
import static java.lang.Math.abs;
import static java.lang.Math.round;
import static uk.ac.ebi.microarray.atlas.model.UpDownExpression.isPvalValid;
import static uk.ac.ebi.microarray.atlas.model.UpDownExpression.isTStatValid;

/**
 * This class is used as a key in SortedMaps to achieve sorting (by pval/tstat rank) of experiments for an OR list attributes (c.f. Statistics class)
 */
@Immutable
public final class PTRank implements Serializable, Comparable<PTRank> {
    private static final long serialVersionUID = 6825217040776538478L;
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
        if (!isPvalValid(p))
            throw LogUtil.createUnexpected("Invalid pValue: " + p);
        if (!isTStatValid(t))
            throw LogUtil.createUnexpected("Invalid tStatistic: " + t);
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
     * Defines natural order descending by absolute value of T first, then ascending by P
     * <p/>
     * Note that there is one case when <code>compareTo(PTRank)</code>
     * is NOT compatible with {@link #equals(Object)}: it happens is P values are equals, and T values are opposite.
     *
     * @param o the DE candidate to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified candidate.
     */
    public int compareTo(PTRank o) {
        int result = -compare(abs(getTStatRank()), abs(o.getTStatRank()));
        return result != 0 ? result : compare(getPValue(), o.getPValue());
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
