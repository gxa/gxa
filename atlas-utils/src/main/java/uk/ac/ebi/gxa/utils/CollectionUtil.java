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

import com.google.common.base.Function;

import javax.annotation.Nonnull;
import java.util.*;

import static com.google.common.collect.Lists.transform;

/**
 * The missing collections handling utility functions. All are static and mostly unrelated to each other.
 *
 * @author pashky
 */
public class CollectionUtil {
    /**
     * Creates a map with key/value pairs defined by array of objects
     *
     * @param objs array of interleaved keys and values, should be of even size
     * @param <K>  map key type
     * @param <V>  map value type
     * @return constructed map
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> makeMap(Object... objs) {
        Map<K, V> map = new HashMap<K, V>();
        for (int i = 0; i < (objs.length & -2); i += 2) {
            map.put((K) objs[i], (V) objs[i + 1]);
        }
        return map;
    }

    /**
     * Extends map with key/value pairs defined by array of objects. Already existing keys are replaced with new values
     * The map is copied to a new object, original stays the same
     *
     * @param omap original map to extend
     * @param objs array of interleaved keys and values, should be of even size
     * @param <K>  map key type
     * @param <V>  map value type
     * @return constructed map
     */

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> addMap(Map<K, V> omap, Object... objs) {
        Map<K, V> map = new HashMap<K, V>(omap);
        for (int i = 0; i < (objs.length & -2); i += 2) {
            map.put((K) objs[i], (V) objs[i + 1]);
        }
        return map;
    }

    public static <T> Set<T> distinct(Collection<T> values) {
        return new LinkedHashSet<T>(values);
    }

    /**
     * Picks a list of elements according to the list of indices supplied
     *
     * @param source  the source to pick elements from
     * @param indices the indices to get
     * @return a list of floats according to the list of indices supplied
     */
    public static <T> List<T> multiget(final List<T> source, List<Integer> indices) {
        return transform(indices,
                new Function<Integer, T>() {
                    public T apply(@Nonnull Integer j) {
                        return source.get(j);
                    }
                });
    }

    public static <T> T first(List<T> results) {
        return results.size() > 0 ? results.get(0) : null;
    }

    public static <T> Iterable<T> iterable(final Iterator<T> iterator) {
        return new Iterable<T>() {
            public Iterator<T> iterator() {
                return iterator;
            }
        };
    }

    public static <T> Iterable<List<T>> asChunks(List<T> source, int chunksize) {
        return iterable(new ChunkedSublistIterator<List<T>>(source, chunksize));
    }
}
