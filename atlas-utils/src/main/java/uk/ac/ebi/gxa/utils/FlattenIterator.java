package uk.ac.ebi.gxa.utils;

import java.util.Iterator;

/**
 * Iterator base class, allowing to flatten nested iterators into one sequence
 * @author pashky
 */
public abstract class FlattenIterator<Outer,Inner> implements Iterator<Inner> {

    private final Iterator<Outer> outeriter;
    private Iterator<Inner> inneriter;
    private Inner object;

    protected FlattenIterator(Iterator<Outer> outeriter) {
        this.outeriter = outeriter;
        skip();
    }

    private void skip() {
        object = null;
        while(object == null) {

            if(inneriter == null) {
                if(!outeriter.hasNext())
                    return;
                inneriter = inner(outeriter.next());
            }

            if(inneriter.hasNext())
                object = inneriter.next();
            else
                inneriter = null;
        }
    }

    /**
     * Implement this method to create inner iterator base on outer current value
     * @param outer outer value
     * @return inner iterator
     */
    public abstract Iterator<Inner> inner(Outer outer);

    public boolean hasNext() {
        return object != null;
    }

    public Inner next() {
        Inner value = object;
        skip();
        return value;
    }

    public void remove() {

    }
}
