package uk.ac.ebi.gxa.utils;

import java.util.Iterator;

/**
 * @author pashky
 */
public class CountIterator implements Iterator<Integer> {
    private int i;
    private int to;
    private int step;

    public static CountIterator zeroTo(int n) { return new CountIterator(0, n, 1); }
    public static CountIterator oneTo(int n) { return new CountIterator(1, n + 1, 1); }

    public CountIterator(int from, int to, int step) {
        this.i = from;
        this.to = to;
        this.step = step;
    }

    public boolean hasNext() {
        return i < to;
    }

    public Integer next() {
        int v = i;
        i += step;
        return v;
    }

    public void remove() {

    }
}
