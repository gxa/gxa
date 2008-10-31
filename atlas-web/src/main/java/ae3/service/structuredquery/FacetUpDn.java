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

    public void setUp(int up) {
        this.up = up;
    }

    public void setDown(int down) {
        this.down = down;
    }

    public void addUp(int up) {
        this.up += up;
    }

    public void addDown(int down) {
        this.down += down;
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
