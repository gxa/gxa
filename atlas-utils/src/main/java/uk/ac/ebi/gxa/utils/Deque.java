/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

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

    /**
     * Default constructor
     */
    public Deque() {
    }

    /**
     * Constructor with custom block size
     * @param blockSize block size
     */
    public Deque(int blockSize) {
        this.blockSize = blockSize;
    }

    /**
     * Copy constructor from another collection
     * @param source collection to copy items from
     */
    public Deque(final Collection<T> source) {
        for(T e : source)
            offerLast(e);
    }

    /**
     * Copy constructor from another collection with custom block size
     * @param blockSize custom block size
     * @param source colelction to copy items from
     */
    public Deque(int blockSize, final Collection<T> source) {
        this(blockSize);
        for(T e : source)
            offerLast(e);
    }

    private List<T> makeBlock() {
        return new ArrayList<T>(blockSize);
    }

    /**
     * Append items to the end of deque
     * @param e item
     */
    public void offerLast(T e) {
        List<T> block = null;
        if(!storage.isEmpty())
            block = storage.getLast();
        if(block == null || block.size() == blockSize)
            storage.add(block = makeBlock());
        block.add(e);
    }

    /**
     * Poll item from the head of deque
     * @return item and null if none was found
     */
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

    /**
     * Gets size of the deque in items
     * @return size in items
     */
    public int size() {
        int size = storage.size() * blockSize;
        if(size > 0)
            size += storage.getLast().size() - blockSize;
        return size;
    }
}
