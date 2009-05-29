package ae3.service.structuredquery;

import uk.ac.ebi.ae3.indexbuilder.Expression;

import java.util.*;

/**
 * Experiments list container class
 * @author pashky
 */
public class ExperimentList implements Iterable<ExperimentRow>, Comparable<ExperimentList> {
    private SortedSet<ExperimentRow> ups = new TreeSet<ExperimentRow>();
    private SortedSet<ExperimentRow> downs = new TreeSet<ExperimentRow>();

    public static Comparator<ExperimentRow> ORDER_PVALUE = new Comparator<ExperimentRow>() {
        public int compare(ExperimentRow o1, ExperimentRow o2) {
            return o1.compareTo(o2);
        }
    };

    public static Comparator<ExperimentRow> ORDER_EFEFV_PVALUE = new Comparator<ExperimentRow>() {
        public int compare(ExperimentRow o1, ExperimentRow o2) {
            int d = o1.getEf().compareTo(o2.getEf());
            if(d != 0)
                return d;
            d = o1.getEfv().compareTo(o2.getEfv());
            if(d != 0)
                return d;
            d = Double.valueOf(o1.getPvalue()).compareTo(o2.getPvalue());
            return d;
        }
    };

    public static Comparator<ExperimentRow> ORDER_EXPID_PVALUE = new Comparator<ExperimentRow>() {
        public int compare(ExperimentRow o1, ExperimentRow o2) {
            int d = Long.valueOf(o1.getExperimentId()).compareTo(o2.getExperimentId());
            if(d != 0)
                return d;
            d = Double.valueOf(o1.getPvalue()).compareTo(o2.getPvalue()); 
            return d;
        }
    };

    /**
     * Merging iterartor
     */
    public class BothIterator implements Iterator<ExperimentRow> {
        private Iterator<ExperimentRow> upiter = ups.iterator();
        private Iterator<ExperimentRow> dniter = downs.iterator();
        private ExperimentRow up = null;
        private ExperimentRow dn = null;

        public boolean hasNext() {
            return upiter.hasNext() || dniter.hasNext() || up != null || dn != null;
        }

        public ExperimentRow next() {
            ExperimentRow out;
            if(up == null && upiter.hasNext())
                up = upiter.next();
            if(dn == null && dniter.hasNext())
                dn = dniter.next();
            if(dn == null || (up != null && ups.comparator().compare(up, dn) < 0))
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

    public ExperimentList(Comparator<ExperimentRow> comparator) {
        ups = new TreeSet<ExperimentRow>(comparator);
        downs = new TreeSet<ExperimentRow>(comparator);
    }

    public ExperimentList() {
        this(ORDER_PVALUE);
    }

    /**
     * Returns iterator merge soring experiments by their significance (p-value)
     * @return iterator of merged experiments rows
     */
    public Iterator<ExperimentRow> iterator() {
        return new BothIterator();
    }

    /**
     * Returns iterable list of UP experiments
     * @return iterable list of experiments
     */
    public Iterable<ExperimentRow> getUps() {
        return ups;
    }

    /**
     * Returns iterable list of DOWNS experiments
     * @return iterable list of experiments
     */
    public Iterable<ExperimentRow> getDowns() {
        return downs;
    }

    /**
     * Returns total number of experiments
     * @return total number of experiments
     */
    public int getNum()
    {
        return ups.size() + downs.size();
    }

    public int compareTo(ExperimentList o) {
        return Integer.valueOf(getNum()).compareTo(o.getNum());
    }

    /**
     * Adds experiment row to the list
     * @param row experiment row
     */
    public void add(ExperimentRow row)
    {
        (row.getUpdn().isUp() ? ups : downs).add(row);
    }

    public int getNumUps() {
        return ups.size();
    }

    public int getNumDowns() {
        return downs.size();
    }

    public double getMinPvalUp() {
        double r = 1;
        for(ExperimentRow er : ups)
            if(er.getPvalue() < r)
                r = er.getPvalue();
        return r;
    }

    public double getMinPvalDn() {
        double r = 1;
        for(ExperimentRow er : downs)
            if(er.getPvalue() < r)
                r = er.getPvalue();
        return r;
    }
}
