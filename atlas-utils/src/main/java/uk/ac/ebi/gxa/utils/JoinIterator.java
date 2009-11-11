package uk.ac.ebi.gxa.utils;

import java.util.Iterator;

/**
 * Join iterator pattern allows to iterator over two other iterators in sequence, doing mapping
 * Can have two different input element types and two mapping function, each of them can be used for filtering
 * elements out - just return null value to mit element in output.
 *
 * From1 - source #1 type
 * From2 - source #2 type
 * To - target type
 * @author pashky
 */
public abstract class JoinIterator<From1,From2,To> implements Iterator<To> {
    private final Iterator<From1> i1;
    private final Iterator<From2> i2;

    /**
     * Constructor
     * @param i1 source iterator #1
     * @param i2 source iterator #2
     */
    public JoinIterator(Iterator<From1> i1, Iterator<From2> i2) {
        this.i1 = i1;
        this.i2 = i2;
        skip();
    }

    private To object;

    public boolean hasNext() {
        return object != null || i1.hasNext() || i2.hasNext();
    }

    public To next() {
        To result = object;
        skip();
        return result;
    }

    /**
     * Implement this method to filter/map iterator #1 values
     * @param from value
     * @return target value or null if should be omitted
     */
    public abstract To map1(From1 from);

    /**
     * Implement this method to filter/map iterator #2 values
     * @param from value
     * @return target value or null if should be omitted
     */
    public abstract To map2(From2 from);

    private void skip() {
        object = null;
        while(i1.hasNext()) {
            object = map1(i1.next());
            if(object != null)
                return;
        }
        while(i2.hasNext()) {
            object = map2(i2.next());
            if(object != null)
                return;
        }
    }

    public void remove() { }
}
