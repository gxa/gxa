package uk.ac.ebi.ae3.indexbuilder;

/**
 * @author pashky
 */
public enum Expression {
    UP { public boolean isUp() { return true; } },
    DOWN { public boolean isUp() { return false; } };

    abstract public boolean isUp();

    public static Expression valueOf(boolean isUp) {
        return isUp ? Expression.UP : Expression.DOWN;        
    }
}
