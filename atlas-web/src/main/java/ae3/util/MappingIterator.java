package ae3.util;

import java.util.Iterator;

/**
 * Simple mapping iterator pattern: maps source iterated element to result of any expression using them
 * From - source element type
 * To - target element type
 * @author pashky
 */
public abstract class MappingIterator<From,To> implements Iterator<To> {
    private final Iterator<From> fromiter;

    /**
     * Constructor
     * @param fromiter source iterator
     */
    protected MappingIterator(Iterator<From> fromiter) {
        this.fromiter = fromiter;
    }

    public boolean hasNext() {
        return fromiter.hasNext();
    }

    public To next() {
        return map(fromiter.next());
    }

    /**
     * Implement this method to specify mapping expression
     * @param from source element
     * @return result to be put out
     */
    public abstract To map(From from);

    public void remove() {

    }
}
