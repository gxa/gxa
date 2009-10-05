package ae3.service.structuredquery;

/**
 * @author pashky
*/
public class FacetCounter implements Comparable<FacetCounter> {
    private String name;
    private int count;

    public FacetCounter(String name, int count) {
        this.name = name;
        this.count = count;
    }

    public int getCount() {
        return count;
    }

    public String getName() {
        return name;
    }

    public int compareTo(FacetCounter o) {
        // descending order
        return - Integer.valueOf(getCount()).compareTo(o.getCount());
    }
}
