package ae3.service.structuredquery;

import uk.ac.ebi.gxa.utils.EfvTree;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: 2/17/11
 * Time: 8:51 AM
 * This class represents a vertical slice through heatmap, corresponding to a single ef-efv. The data in this class is
 * used to sort efv columns according to their cumulative experiment counts.
 */
public class HeatMapColumn {
    private EfvTree.EfEfv<ColumnInfo> efEfv;

    List<UpdownCounter> rowCounters = new ArrayList<UpdownCounter>();

    UpdownCounter columnCounter = new UpdownCounter(0, 0, 0, 1, 1);

    public HeatMapColumn(EfvTree.EfEfv<ColumnInfo> efEfv) {
        this.efEfv = efEfv;
    }

    public UpdownCounter getColumnCounter() {
        return columnCounter;
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

    public int compareTo(HeatMapColumn o) {
        return getColumnCounter().compareTo(o.getColumnCounter());


    }
}
