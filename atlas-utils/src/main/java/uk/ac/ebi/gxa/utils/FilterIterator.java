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
