package ae3.service.structuredquery;

/**
 * @author pashky
*/
public class UpdownCounter {
    private int ups;
    private int downs;
    private double mpvup;
    private double mpvdn;

    public UpdownCounter(int ups, int downs, double mpvup, double mpvdn) {
        this.ups = ups;
        this.downs = downs;
        this.mpvup = mpvup;
        this.mpvdn = mpvdn;
    }

    public int getUps() {
        return ups;
    }

    public int getDowns() {
        return downs;
    }

    public double getMpvUp() {
        return mpvup;
    }

    public double getMpvDn() {
        return mpvdn;
    }
}
