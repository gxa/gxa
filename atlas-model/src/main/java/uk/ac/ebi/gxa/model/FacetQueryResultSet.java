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

        public Iterable<Map.Entry<Key,Value>> byKey() {
            return facetMap.entrySet();
        }

        public Iterable<Map.Entry<Key,Value>> byValue() {
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
