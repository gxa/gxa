package uk.ac.ebi.gxa.statistics;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: Nov 17, 2010
 * Time: 3:27:28 PM
 * Class to represent the overall experiment counts (score) for a given gene - used in ordering of genes in heatmap
 */
public class GeneScore<GeneIdType> implements Comparable {
    private GeneIdType geneId;
    private int ups = 0;
    private int downs = 0;
    private int nones = 0;

    public GeneScore(GeneIdType geneId) {
        this.geneId = geneId;
    }

    public void addCounts(int upCount, int dnCount, int nonDECount) {
        ups += upCount;
        downs += dnCount;
        nones += nonDECount;
    }

    public boolean isNonZero() {
        return getUps() + getDowns() + getNones() > 0;
    }

    public int compareTo(Object o) {
        if (this == o)
            return 0;

        GeneScore<GeneIdType> that = (GeneScore<GeneIdType>) o;

        int thisExpCountsSum = this.getUps() + this.getDowns() + this.getNones();
        int thatExpCountsSum = that.getUps() + that.getDowns() + that.getNones();

        if (thisExpCountsSum < thatExpCountsSum) { // greater counts should come first
            return 1;
        } else if (thisExpCountsSum == thatExpCountsSum) {
            return 0;
        }
        return -1;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GeneScore that = (GeneScore) o;

        if (geneId != null ? !geneId.equals(that.getGeneId()) : that.getGeneId() != null) return false;
        if (this.getUps() != that.getUps()) return false;
        if (this.getDowns() != that.getDowns()) return false;
        if (this.getNones() != that.getNones()) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = geneId != null ? geneId.hashCode() : 0;
        result = 31 * result + ups;
        result = 31 * result + downs;
        result = 31 * result + nones;

        return result;
    }

    @Override
    public String toString() {
        return "GeneScore{" +
                "geneId=" + geneId +
                ", ups=" + ups +
                ", downs=" + downs +
                ", nones=" + nones +
                '}';
    }

    public GeneIdType getGeneId() {
        return geneId;
    }

    public int getUps() {
        return ups;
    }

    public int getDowns() {
        return downs;
    }

    public int getNones() {
        return nones;
    }
}
