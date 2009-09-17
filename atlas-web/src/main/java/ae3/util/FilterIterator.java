package ae3.util;

import java.util.Iterator;

/**
 * Filtering iterator class handy to make lazily filtered and mapped iteration out of another iterator
 * From - source element type
 * To - target element type
 *
 * Misfunctionality: can't output null's as null menas "skip this element"
 *
 * @author pashky
 */
public abstract class FilterIterator<From,To> implements Iterator<To> {
    private final Iterator<From> fromiter;
    private To object;

    /**
     * Constructor
     * @param fromiter source iterator to wrap
     */
    protected FilterIterator(Iterator<From> fromiter) {
        this.fromiter = fromiter;
        skip();
    }

    public boolean hasNext() {
        return object != null || fromiter.hasNext();
    }

    public To next() {
        To result = object;
        skip();
        return result;
    }

    /**
     * Implement this method to support filtering/mapping functionality
     * @param from source elemtn
     * @return target element or null if this element should be omitted in output iteration
     */
    public abstract To map(From from);

    private void skip() {
        object = null;
        while(object == null && fromiter.hasNext()) {
            object = map(fromiter.next());
        }
    }

    public void remove() {

    }
}
