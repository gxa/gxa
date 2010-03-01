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

import java.util.Map;


/**
 * Pair container class
 * A - first element type
 * B - second element type
 * @author pashky
 */
public class Pair<A,B> implements Map.Entry<A,B> {
    private final A first;
    private final B second;

    /**
     * Constructor. The class is designed to be immutable, so no other ways to change values
     * @param first first value
     * @param second second value
     */
    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Get first value
     * @return value
     */
    public A getFirst() { return first; }

    /**
     * Get second value
     * @return value
     */
    public B getSecond() { return second; }

    public String toString() {
        return "(" + first + ", " + second + ")";
    }

    private static boolean equals(Object x, Object y) {
        return (x == null && y == null) || (x != null && x.equals(y));
    }

    /**
     * Equal, when both values are equal() or both are null
     * @return true if equal
     */
    public boolean equals(Object other) {
        return
                other instanceof Pair &&
                        equals(first, ((Pair)other).first) &&
                        equals(second, ((Pair)other).second);
    }

    /**
     * Hashcode uses both values
     * @return hash code
     */
    public int hashCode() {
        if (first == null) return (second == null) ? 0 : second.hashCode() + 1;
        else if (second == null) return first.hashCode() + 2;
        else return first.hashCode() * 17 + second.hashCode();
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
