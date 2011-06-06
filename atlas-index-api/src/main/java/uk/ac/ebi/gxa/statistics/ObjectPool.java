package uk.ac.ebi.gxa.statistics;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ThreadSafe
public class ObjectPool<T> {
    private ConcurrentMap<T, T> pool = new ConcurrentHashMap<T, T>();

    /**
     * Obtain canonical (i.e. least recently used) version of the requested object
     *
     * @param o the object to intern
     * @return canonical (i.e. least recently used) version of the requested object
     */
    public T intern(T o) {
        return pool.putIfAbsent(o, o);
    }
}
