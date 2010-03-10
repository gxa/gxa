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

import java.util.Iterator;

/**
 * Similar to {@link JoinIterator}, but iterates over any number of iterators of the same type not doing
 * any mapping/filtering. Bascially, unites several iterators into one big sequence. 
 * @author pashky
 */
public class SequenceIterator<Type> implements Iterator<Type> {
    private Iterator<Type> iters[];
    private int i = 0;

    public SequenceIterator(Iterator<Type>... iters) {
        this.iters = iters;
        skip();
    }

    public boolean hasNext() {
        return i < iters.length && iters[i].hasNext();
    }

    public Type next() {
        Type result = iters[i].next();
        skip();
        return result;
    }

    private void skip() {
        for(; i < iters.length && !iters[i].hasNext(); ++i);
    }

    public void remove() {
    }
}
