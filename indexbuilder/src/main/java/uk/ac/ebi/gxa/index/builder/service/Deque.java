package uk.ac.ebi.gxa.index.builder.service;

import java.util.*;

/**
 * A very simple and loosy queue implementation based on linked list of arrays (deque)
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
            append(e);
    }

    public Deque(int blockSize, final Collection<T> source) {
        this(blockSize);
        for(T e : source)
            append(e);
    }

    private List<T> makeBlock() {
        return new ArrayList<T>(blockSize);
    }

    public void append(T e) {
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
