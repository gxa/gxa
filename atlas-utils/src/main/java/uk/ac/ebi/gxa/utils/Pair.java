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

import javax.annotation.concurrent.Immutable;
import java.io.Serializable;
import java.util.Map;


/**
 * Pair container class
 * A - first element type
 * B - second element type
 *
 * @author pashky
 */
@Immutable
public class Pair<A, B> implements Map.Entry<A, B>, Serializable {
    private final A first;
    private final B second;

    protected Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Factory method. The class is designed to be immutable, so no other ways to change values
     *
     * @param first  first value
     * @param second second value
     * @return newly-created, immutable pair
     */
    public static <A, B> Pair<A, B> create(A first, B second) {
        return new Pair<A, B>(first, second);
    }

    /**
     * Get first value
     *
     * @return value
     */
    public A getFirst() {
        return first;
    }

    /**
     * Get second value
     *
     * @return value
     */
    public B getSecond() {
        return second;
    }

    public String toString() {
        return "(" + first + ", " + second + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pair pair = (Pair) o;

        if (first != null ? !first.equals(pair.first) : pair.first != null) return false;
        if (second != null ? !second.equals(pair.second) : pair.second != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = first != null ? first.hashCode() : 0;
        result = 31 * result + (second != null ? second.hashCode() : 0);
        return result;
    }

    public A getKey() {
        return getFirst();
    }

    public B getValue() {
        return getSecond();
    }

    public B setValue(B value) {
        throw new IllegalAccessError("Setter is not implemented, pair is immutable");
    }
}
