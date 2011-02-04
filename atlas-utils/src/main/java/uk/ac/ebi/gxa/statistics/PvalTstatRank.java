package uk.ac.ebi.gxa.statistics;

import java.io.Serializable;

/**
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PvalTstatRank that = (PvalTstatRank) o;

        if (pValue != null ? !pValue.equals(that.pValue) : that.pValue != null) return false;
        if (tStatRank != null ? !tStatRank.equals(that.tStatRank) : that.tStatRank != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = pValue != null ? pValue.hashCode() : 0;
        result = 31 * result + (tStatRank != null ? tStatRank.hashCode() : 0);
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
     * sre stored as two distinct objects in a TreeSet. This is because PvalTstatRank class serves a dual function:
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
    public int compareTo(PvalTstatRank o) {
        if (Math.abs(getTStatRank()) != Math.abs(o.getTStatRank())) {
            return Math.abs(o.getTStatRank()) - Math.abs(getTStatRank()); // higher absolute value of tStatRank comes first
        }

        if (getPValue() == null || getPValue() > 1) // NA pVal for this experiment
            return 1; // the other PvalTstatRank comes first

        if (o.getPValue() == null || o.getPValue() > 1) // NA pVal for the compared experiment
            return -1; // this PvalTstatRank comes first

        if (getPValue().equals(o.getPValue()))
            // if pvals are different, return the lower actual value of tStatRank
            // (arbitrary if it's lower or higher here - it's just that one of them has to come first)
            return getTStatRank() - o.getTStatRank();


        return getPValue().compareTo(o.getPValue()); // lower pVals come first
    }
}
