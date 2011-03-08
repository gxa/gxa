package ae3.service.structuredquery;

import uk.ac.ebi.gxa.utils.EfvTree;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a vertical slice through heatmap, corresponding to a single ef-efv. The data in this class is
 * used to sort efv columns according to their cumulative experiment counts.
 */
public class HeatMapColumn implements Comparable<HeatMapColumn> {
    private EfvTree.EfEfv<ColumnInfo> efEfv;

    List<UpdownCounter> rowCounters = new ArrayList<UpdownCounter>();

    UpdownCounter columnCounter = new UpdownCounter(0, 0, 0, 1, 1);

    // Flag specifying if this heat map column has at least one cell that qualifies it to be displayed in the heatmap
    boolean qualifies = false;

    public HeatMapColumn(EfvTree.EfEfv<ColumnInfo> efEfv) {
        this.efEfv = efEfv;
    }

    public UpdownCounter getRowCounter(int rowNum) {
        return rowCounters.get(rowNum);
    }

    public EfvTree.EfEfv<ColumnInfo> getEfEfv() {
        return efEfv;
    }

    public void addRowCounter(UpdownCounter counter) {
        rowCounters.add(counter);
        columnCounter.addUps(counter.getUps());
        columnCounter.addDowns(counter.getDowns());
        columnCounter.addNones(counter.getNones());
    }

    public boolean qualifies() {
        return qualifies;
    }

    public void setQualifies(boolean qualifies) {
        this.qualifies = qualifies;
    }

    /**
     * Note: this class has a natural ordering that is inconsistent with equals.
     *
     * @param o the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified object.
     */
    public int compareTo(HeatMapColumn o) {
        if (efEfv.getEf().equals(o.efEfv.getEf()))
            return columnCounter.compareTo(o.columnCounter);
        return efEfv.getEf().compareTo(o.efEfv.getEf());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HeatMapColumn that = (HeatMapColumn) o;

        if (columnCounter != null ? !columnCounter.equals(that.columnCounter) : that.columnCounter != null)
            return false;
        if (efEfv != null ? !efEfv.equals(that.efEfv) : that.efEfv != null) return false;
        if (rowCounters != null ? !rowCounters.equals(that.rowCounters) : that.rowCounters != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = efEfv != null ? efEfv.hashCode() : 0;
        result = 31 * result + (rowCounters != null ? rowCounters.hashCode() : 0);
        result = 31 * result + (columnCounter != null ? columnCounter.hashCode() : 0);
        return result;
    }
}
