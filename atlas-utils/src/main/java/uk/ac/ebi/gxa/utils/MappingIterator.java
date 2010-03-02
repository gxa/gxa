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
