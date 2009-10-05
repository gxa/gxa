package ae3.util;


/**
 * Pair container class
 * A - first element type
 * B - second element type
 * @author pashky
 */
public class Pair<A,B> {
    private final A first;
    private final B second;

    /**
     * Constructor. The class is designed to be immutable, so no other ways to change values
     * @param first first value
     * @param second second value
     */
    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Get first value
     * @return value
     */
    public A getFirst() { return first; }

    /**
     * Get second value
     * @return value
     */
    public B getSecond() { return second; }

    public String toString() {
        return "(" + first + ", " + second + ")";
    }

    private static boolean equals(Object x, Object y) {
        return (x == null && y == null) || (x != null && x.equals(y));
    }

    /**
     * Equal, when both values are equal() or both are null
     * @return true if equal
     */
    public boolean equals(Object other) {
        return
                other instanceof Pair &&
                        equals(first, ((Pair)other).first) &&
                        equals(second, ((Pair)other).second);
    }

    /**
     * Hashcode uses both values
     * @return hash code
     */
    public int hashCode() {
        if (first == null) return (second == null) ? 0 : second.hashCode() + 1;
        else if (second == null) return first.hashCode() + 2;
        else return first.hashCode() * 17 + second.hashCode();
    }
}
