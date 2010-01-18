package uk.ac.ebi.gxa.utils;

import java.util.*;

/**
 * A very simple and loose queue implementation based on linked list of arrays (deque).  This is basically equivalent to
 * a Deque implementation in Java 6, but means our code retains backwards compatibility with Java 5.
 *
 * @author pashky
 */
public class Deque<T> {

    private int blockSize = 100;

    private LinkedList<List<T>> storage = new LinkedList<List<T>>();
    private int position = 0;

    public Deque() {
    }

    public Deque(int blockSize) {
        this.blockSize = blockSize;
    }

    public Deque(final Collection<T> source) {
        for(T e : source)
            offerLast(e);
    }

    public Deque(int blockSize, final Collection<T> source) {
        this(blockSize);
        for(T e : source)
            offerLast(e);
    }

    private List<T> makeBlock() {
        return new ArrayList<T>(blockSize);
    }

    public void offerLast(T e) {
        List<T> block = null;
        if(!storage.isEmpty())
            block = storage.getLast();
        if(block == null || block.size() == blockSize)
            storage.add(block = makeBlock());
        block.add(e);
    }

    public T poll() {
        if(storage.isEmpty())
            return null;

        List<T> block = storage.getFirst();
        if(position >= block.size())
            return null;

        T result = block.set(position, null);
        ++position;
        if(position >= blockSize) {
            position = 0;
            storage.removeFirst();
        }
        return result;
    }

    public int size() {
        int size = storage.size() * blockSize;
        if(size > 0)
            size += storage.getLast().size() - blockSize;
        return size;
    }
}
