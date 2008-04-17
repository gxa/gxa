package ae3.service;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: ostolop
 * Date: Apr 16, 2008
 * Time: 1:01:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class AtlasResultSetCache {
    private static SortedMap<String, AtlasResultSet> arsCache = Collections.synchronizedSortedMap(new TreeMap<String,AtlasResultSet>());

    synchronized public boolean containsKey(String arsCacheKey) {
        return arsCache.containsKey(arsCacheKey);
    }


    synchronized public AtlasResultSet get(String arsCacheKey) {
        return arsCache.get(arsCacheKey);
    }

    synchronized public AtlasResultSet put(String arsCacheKey, AtlasResultSet ars) {
        // TODO: evict LRU element if too many elts in cache
        return arsCache.put(arsCacheKey, ars);
    }

    synchronized public AtlasResultSet remove(String arsCacheKey) {
        AtlasResultSet ars = arsCache.remove(arsCacheKey);
        ars.cleanup();

        return ars;
    }
}
