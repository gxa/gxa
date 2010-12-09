package ae3.anatomogram;

public class AnatomogramArea {
    public int x0, x1, y0, y1;
    public String name;
    public String efo;

    public String getEfo() {
        return efo;
    }

    public String getX0() {
        return String.valueOf(x0);
    }

    public String getX1() {
        return String.valueOf(x1);
    }

    public String getY0() {
        return String.valueOf(y0);
    }

    public String getY1() {
        return String.valueOf(y1);
    }
}
