package uk.ac.ebi.gxa.utils;

import java.util.Iterator;

/**
 * @author pashky
 */
public class EmptyIterator {

    private static Iterator ITERATOR = new Iterator() {
        public boolean hasNext() {
            return false;
        }

        public Object next() {
            throw new IllegalStateException("I said: it's empty!");
        }

        public void remove() {  }
    };
    
    private static Iterable ITERABLE = new Iterable() {
        public Iterator iterator() {
            return ITERATOR;
        }
    };

    public static <T> Iterator<T> emptyIterator() {
        @SuppressWarnings("unchecked")
        Iterator<T> i = (Iterator<T>)ITERATOR;
        return i;
    }

    public static <T> Iterable<T> emptyIterable() {
        @SuppressWarnings("unchecked")
        Iterable<T> i = (Iterable<T>)ITERABLE;
        return i;
    }

}
