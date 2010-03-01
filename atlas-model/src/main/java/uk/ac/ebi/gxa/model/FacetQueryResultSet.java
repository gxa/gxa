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

package uk.ac.ebi.gxa.model;

import java.util.*;

/**
 * @author pashky
 */
public class FacetQueryResultSet<ResultType,FacetType extends FacetQueryResultSet.FacetField> extends QueryResultSet<ResultType> {

    public static abstract class FacetField<Key extends Comparable<Key>, Value extends Comparable<Value>> {
        private SortedMap<Key,Value> facetMap = new TreeMap<Key,Value>();

        public void addValue(Key key, Value value) {
            facetMap.put(key, value);
        }

        public Value getValue(Key key) {
            return facetMap.get(key);
        }

        public Value getOrCreateValue(Key key) {
            if(!facetMap.containsKey(key))
                facetMap.put(key, createValue());
            return facetMap.get(key);
        }

        public Collection<Map.Entry<Key,Value>> getListByKey() {
            return facetMap.entrySet();
        }

        public Collection<Map.Entry<Key,Value>> getListByValue() {
            List<Map.Entry<Key,Value>> sortedList = new ArrayList<Map.Entry<Key,Value>>(facetMap.entrySet());
            Collections.sort(sortedList, new Comparator<Map.Entry<Key, Value>>() {
                public int compare(Map.Entry<Key, Value> o1, Map.Entry<Key, Value> o2) {
                    return o1.getValue().compareTo(o2.getValue());
                }
            });
            return sortedList;
        }

        public abstract Value createValue();

    }

    private List<FacetType> facets = new ArrayList<FacetType>();

    public void addFacet(FacetType type) {
        facets.add(type);
    }

    public List<FacetType> getFacets() {
        return facets;
    }

    
}
