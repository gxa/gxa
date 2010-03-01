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
 * http://ostolop.github.com/gxa/
 */

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
