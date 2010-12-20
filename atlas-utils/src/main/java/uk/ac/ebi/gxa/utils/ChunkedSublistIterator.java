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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Allows to iterate over a list in sublist chunks of specified size. 
 */
public class ChunkedSublistIterator<T extends List> implements Iterator<T> {
    final private T list;
    final private int chunksize;

    private int last = 0;

    public ChunkedSublistIterator(T list, final int chunksize) {
        this.list = list;
        this.chunksize = chunksize;
    }

    public boolean hasNext() {
        return last < list.size();
    }

    @SuppressWarnings("unchecked")
    public T next() {
        final int to = last + chunksize;
        final int from = last;
        last = to;

        return (T) new ArrayList(list.subList(from, to > list.size() ? list.size() : to));
    }

    public void remove() {}
}
