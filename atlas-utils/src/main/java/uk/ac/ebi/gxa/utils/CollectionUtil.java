package uk.ac.ebi.gxa.utils;

import java.util.Map;
import java.util.HashMap;

/**
 * @author pashky
 */
public class CollectionUtil {
    /**
     * Creates a map with key/value pairs defined by array of objects
     * @param objs array of interleaved keys and values, should be of even size
     * @param <K> map key type
     * @param <V> map value type
     * @return constructed map
     */
    @SuppressWarnings("unchecked")
    public static <K,V> Map<K,V> makeMap(Object ... objs) {
        Map<K,V> map = new HashMap<K,V>();
        for(int i = 0; i < (objs.length & -2); i += 2) {
            map.put((K)objs[i], (V)objs[i+1]);
        }
        return map;
    }

    /**
     * Extends map with key/value pairs defined by array of objects. Already existing keys are replaced with new values
     * The map is copied to a new object, original stays the same
     * @param omap original map to extend
     * @param objs array of interleaved keys and values, should be of even size
     * @param <K> map key type
     * @param <V> map value type
     * @return constructed map
     */

    @SuppressWarnings("unchecked")
    public static <K,V> Map<K,V> addMap(Map<K,V> omap, Object ... objs) {
        Map<K,V> map = new HashMap<K,V>(omap);
        for(int i = 0; i < (objs.length & -2); i += 2) {
            map.put((K)objs[i], (V)objs[i+1]);
        }
        return map;
    }
}
