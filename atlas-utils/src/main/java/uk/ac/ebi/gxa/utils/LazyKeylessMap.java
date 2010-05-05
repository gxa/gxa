package uk.ac.ebi.gxa.utils;

import org.apache.commons.lang.NotImplementedException;

import java.util.Map;
import java.util.Set;
import java.util.Collection;

/**
 * Fake map implementing only one operation - getting of value by key.
 * Used to trick JSTL-EL.
 * @author pashky
 */
public abstract class LazyKeylessMap<Key, Value> implements Map<Key, Value> {
    protected abstract Value map(Key key);

    public boolean isEmpty() { return false; }

    @SuppressWarnings("unchecked")
    public boolean containsKey(Object key) {
        return map(key != null ? (Key)key : null) != null;
    }

    @SuppressWarnings("unchecked")
    public Value get(Object key) {
        return map(key != null ? (Key)key : null);
    }

    public int size() { throw new NotImplementedException(); }
    public boolean containsValue(Object value) { throw new NotImplementedException(); }
    public Value put(Key key, Value value) { throw new NotImplementedException(); }
    public Value remove(Object key) { throw new NotImplementedException(); }
    public void putAll(Map<? extends Key, ? extends Value> t) { throw new NotImplementedException(); }
    public void clear() { throw new NotImplementedException(); }
    public Set<Key> keySet() { throw new NotImplementedException(); }
    public Collection<Value> values() { throw new NotImplementedException(); }
    public Set<Map.Entry<Key, Value>> entrySet() { throw new NotImplementedException(); }
}
