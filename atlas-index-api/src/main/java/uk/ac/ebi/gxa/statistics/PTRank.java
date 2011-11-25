package uk.ac.ebi.gxa.statistics;

import uk.ac.ebi.gxa.utils.PT;

import javax.annotation.concurrent.Immutable;
import java.io.Serializable;

import static java.lang.Float.compare;
import static java.lang.Float.isNaN;
import static java.lang.Math.abs;
import static java.lang.Math.round;

/**
 * This class is used as a key in SortedMaps to achieve sorting (by rounded pval/tstat rank)
 * of experiments for an OR list attributes (c.f. Statistics class)
 */
@Immutable
public final class PTRank extends PT implements Serializable {

    public static final float PRECISION = 1e-3F;
    private static final long serialVersionUID = -1561898890077805058L;
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

    // This method is necessary only to centralize the sorting logic in the parent
    public float getTStat() {
        return (float) tStatRank;
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
