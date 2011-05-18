/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

import java.util.Iterator;

/**
 * An iterator to walk through the double collections, like List<List<T>>;
 * it is helpful when you need to know both indexes (i, j) when iterating.
 *
 * @author Olga Melnichuk
 *         Date: 03/05/2011
 */
public class DoubleIndexIterator<E> implements Iterator<DoubleIndexIterator.Entry<E>> {
    private Iterator<? extends Iterable<E>> iIterator;
    private Iterator<E> jIterator;
    private int i = -1, j = -1;
    private boolean hasNext = false;

    public DoubleIndexIterator(Iterable<? extends Iterable<E>> collection) {
        iIterator = collection.iterator();
        hasNext = findNext();
    }

    private boolean findNext() {
        while (iIterator.hasNext()) {
            i++;
            jIterator = iIterator.next().iterator();
            if (jIterator.hasNext()) {
                j = -1;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public Entry<E> next() {
        Entry<E> entry = new Entry<E>(i, ++j, jIterator.next());
        hasNext = jIterator.hasNext() || findNext();
        return entry;
    }

    @Override
    public void remove() {
        throw new IllegalStateException("Operation is not supported");
    }

    public static class Entry<E> {
        private final int i;
        private final int j;
        private final E entry;

        Entry(int i, int j, E entry) {
            this.i = i;
            this.j = j;
            this.entry = entry;
        }

        public int getI() {
            return i;
        }

        public int getJ() {
            return j;
        }

        public E getEntry() {
            return entry;
        }
    }

}
