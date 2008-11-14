package ae3.service.structuredquery;

/**
 * @author pashky
*/
public class FacetUpDn implements Comparable<FacetUpDn> {
    private int up;
    private int down;

    public FacetUpDn() {
        up = down = 0;
    }

    void add(int v, boolean doUp)
    {
        if(doUp)
            up += v;
        else
            down += v;
    }

    public int getUp() {
        return up;
    }

    public int getDown() {
        return down;
    }

    public int compareTo(FacetUpDn o) {
        // descending order
        return - Integer.valueOf(getDown() + getUp()).compareTo(o.getUp() + o.getDown());
    }

}
