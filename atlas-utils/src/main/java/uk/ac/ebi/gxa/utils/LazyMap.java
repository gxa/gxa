package uk.ac.ebi.gxa.utils;

import java.util.*;

/**
 * Helper abstract interface used to make full-featured Map classes out of something
 * which can return iterator of its keys (maybe not backed up by some actual collection)
 * and provide a method to get value by key.
 *
 * @author pashky
 */
public abstract class LazyMap<Key,Value> extends AbstractMap<Key,Value> {
    /**
     * Implement this method to return values by key
     * @param key key to find value by
     * @return value
     */
    protected abstract Value map(Key key);

    /**
     * Implement this method to iterator over possible "map" keys
     * @return iterator of keys
     */
    protected abstract Iterator<Key> keys();

    @Override @SuppressWarnings("unchecked")
    public Value get(Object key) {
        return map((Key)key);
    }

    public Set<Map.Entry<Key, Value>> entrySet() {
        return new AbstractSet<Map.Entry<Key, Value>>() {
            public Iterator<Entry<Key, Value>> iterator() {
                return new MappingIterator<Key, Entry<Key, Value>>(keys()) {
                    @Override
                    public Entry<Key, Value> map(Key from) {
                        return Pair.create(from, LazyMap.this.map(from));
                    }
                };
            }

            public int size() {
                int size = 0;
                for(Iterator<Key> ki = keys(); ki.hasNext(); ki.next())
                    ++size;
                return size;
            }
        };
    }
}
