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
 * An iterator, returning a set of sequential integers
 * @author pashky
 */
public class CountIterator implements Iterator<Integer> {
    private int i;
    private int to;
    private int step;

    /**
     * Static constructor method returning iterator counting from 0 to n (non-inclusive)
     * @param n upper value (non-inclusive)
     * @return counting iterator instance
     */
    public static CountIterator zeroTo(int n) { return new CountIterator(0, n, 1); }

    /**
     * Static constructor method returning iterator counting from 1 to n (both inclusive)
     * @param n final value
     * @return counting iterator instance
     */
    public static CountIterator oneTo(int n) { return new CountIterator(1, n + 1, 1); }

    /**
     * Default constructor
     * @param from starting value (inclusive)
     * @param to final value (not inclusive)
     * @param step increment step
     */
    public CountIterator(int from, int to, int step) {
        this.i = from;
        this.to = to;
        this.step = step;
    }

    public boolean hasNext() {
        return i < to;
    }

    public Integer next() {
        int v = i;
        i += step;
        return v;
    }

    public void remove() {

    }
}
