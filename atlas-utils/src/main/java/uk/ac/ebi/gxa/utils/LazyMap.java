package uk.ac.ebi.gxa.utils;

import java.util.*;

/**
 * @author pashky
 */
public abstract class LazyMap<Key,Value> extends AbstractMap<Key,Value> {
    protected abstract Value map(Key key);
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
                        return new Pair<Key,Value>(from, LazyMap.this.map(from));
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
