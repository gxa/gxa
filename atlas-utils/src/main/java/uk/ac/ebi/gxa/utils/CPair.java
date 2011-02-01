package uk.ac.ebi.gxa.utils;

/**
 * {@link Comparable} {@link Pair}
 */
public class CPair<T1 extends Comparable<T1>, T2 extends Comparable<T2>> extends Pair<T1, T2> implements Comparable<CPair<T1, T2>> {
    public CPair(T1 first, T2 second) {
        super(first, second);
    }

    public int compareTo(CPair<T1, T2> o) {
        int d = getFirst().compareTo(o.getFirst());
        return d != 0 ? d : getSecond().compareTo(o.getSecond());
    }
}
