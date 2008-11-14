package ae3.service.structuredquery;

import java.util.*;

/**
 * @author pashky
 */
public class ExperimentList implements Iterable<ExperimentRow>, Comparable<ExperimentList> {
    private SortedSet<ExperimentRow> ups = new TreeSet<ExperimentRow>();
    private SortedSet<ExperimentRow> downs = new TreeSet<ExperimentRow>();

    /**
     * Merging iterartor
     */
    public class BothIterator implements Iterator<ExperimentRow> {
        Iterator<ExperimentRow> upiter = ExperimentList.this.ups.iterator();
        Iterator<ExperimentRow> dniter = ExperimentList.this.downs.iterator();
        ExperimentRow up = null;
        ExperimentRow dn = null;

        public boolean hasNext() {
            return upiter.hasNext() || dniter.hasNext() || up != null || dn != null;
        }

        public ExperimentRow next() {
            ExperimentRow out;
            if(up == null && upiter.hasNext())
                up = upiter.next();
            if(dn == null && dniter.hasNext())
                dn = dniter.next();
            if(dn == null || (up != null && up.compareTo(dn) < 0))
            {
                out = up;
                up = null;
            }
            else
            {
                out = dn;
                dn = null;
            }
            return out;
        }

        public void remove() { }
    }

    public Iterator<ExperimentRow> iterator() {
        return new BothIterator();
    }

    public Iterable<ExperimentRow> getUps() {
        return ups;
    }

    public Iterable<ExperimentRow> getDowns() {
        return downs;
    }

    private int getNum()
    {
        return ups.size() + downs.size();
    }

    public int compareTo(ExperimentList o) {
        return Integer.valueOf(getNum()).compareTo(o.getNum());
    }

    void add(ExperimentRow row)
    {
        (row.getUpdn() == ExperimentRow.UpDn.UP ? ups : downs).add(row);
    }
}
