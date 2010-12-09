package ae3.anatomogram;

public class AnatomogramArea {
    public int X0, X1, Y0, Y1;
    public String Name;
    public String Efo;

    public String getEfo() {
        return Efo;
    }

    public String getX0() {
        return String.valueOf(X0);
    }

    public String getX1() {
        return String.valueOf(X1);
    }

    public String getY0() {
        return String.valueOf(Y0);
    }

    public String getY1() {
        return String.valueOf(Y1);
    }
}
