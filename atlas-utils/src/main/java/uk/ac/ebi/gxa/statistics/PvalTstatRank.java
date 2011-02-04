package uk.ac.ebi.gxa.statistics;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: 1/24/11
 * Time: 9:15 AM
 * This class is used as a key in SortedMaps to achieve sorting (by pval/tstat rank) of experiments for an OR list attributes (c.f. Statistics class)
 */
public class PvalTstatRank implements Serializable, Comparable<PvalTstatRank> {

    private static final long serialVersionUID = -1725289896518124374L;
    // pValue rounded off to 3 decimal places - c.f. GeneAtlasBitIndexBuilderService.bitIndexNetCDFs()
    private Float pValue;
    // For the definition of tStat rank see GeneAtlasBitIndexBuilderService.getTStatRank()
    private Short tStatRank;

    public PvalTstatRank(Float pValue, Short tStatRank) {
        this.pValue = pValue;
        this.tStatRank = tStatRank;
    }

    public Float getPValue() {
        return pValue;
    }

    public Short getTStatRank() {
        return tStatRank;
    }

    private static boolean equals(Object x, Object y) {
        return x == null && y == null || x != null && x.equals(y);
    }

    /**
     * Equal, when both values are equal() or both are null
     *
     * @return true if equal
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof PvalTstatRank &&
                equals(getPValue(), ((PvalTstatRank) o).getPValue()) &&
                equals(getTStatRank(), ((PvalTstatRank) o).getTStatRank());
    }

    /**
     * Hashcode uses both values
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        if (getPValue() == null) return (getTStatRank() == null) ? 0 : getTStatRank().hashCode() + 1;
        else if (getTStatRank() == null) return getPValue().hashCode() + 2;
        else return getPValue().hashCode() * 17 + getTStatRank().hashCode();
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
     * sre stored as two distinct objects in a TreeSet. This is because PvalTstatRank class serves a dual function:
     * - from UP/DOWN Atlas data ordering point of view, (pVal=0 tStatRank=3) and (pVal=0 tStatRank=-3) are equivalent
     * - from UP vs DOWN point of view they are not.
     * Hence, the only way I could think of for supporting both requirements in Statistics.pValuesTStatRanks was to store both PvalTstatRanks as distinct from each other,
     * and yet co-collacted in the TreeSet used in the map.
     *
     * @param o
     * @return
     */
    public int compareTo(PvalTstatRank o) {
        if (Integer.valueOf(Math.abs(getTStatRank())).equals(Integer.valueOf(Math.abs(o.getTStatRank())))) {
            if (getPValue() == null || getPValue() > 1) // NA pVal for this experiment
                return 1; // the other PvalTstatRank comes first
            else if (o.getPValue() == null || o.getPValue() > 1) // NA pVal for the compared experiment
                return -1; // this PvalTstatRank comes first
            else if (Float.valueOf(getPValue()).equals(Float.valueOf(o.getPValue())))
                // if pvals are different, return the lower actual value of tStatRank (arbitrary if it's lower or higher here - it's just that one of them has to come first)
                return Integer.valueOf(getTStatRank()).compareTo(Integer.valueOf(o.getTStatRank()));
            else
                return Float.valueOf(getPValue()).compareTo(Float.valueOf(o.getPValue())); // lower pVals come first
        } else {
            return -Integer.valueOf(Math.abs(getTStatRank())).compareTo(Integer.valueOf(Math.abs(o.getTStatRank()))); // higher absolute value of tStatRank comes first
        }
    }
}
