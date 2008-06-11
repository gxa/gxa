package ae3.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.List;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;

public class AtlasResultSetCache {
    private final Log log = LogFactory.getLog(getClass());
    private Cache arsCache;

    public AtlasResultSetCache() {
        arsCache = CacheManager.getInstance().getCache("AtlasResultSetCache");
    }

    public boolean containsKey(String arsCacheKey) {
        return arsCache.isKeyInCache(arsCacheKey);
    }

    public AtlasResultSet get(String arsCacheKey) {
        return (AtlasResultSet) arsCache.get(arsCacheKey).getValue();
    }

    public void put(String arsCacheKey, AtlasResultSet ars) {
        arsCache.put(new Element(arsCacheKey, ars));
    }

    public int size() {
        return arsCache.getSize();
    }

    /**
     * Syncs the persistent cache with the DB. Any AtlasResultSet objects that have no entries in the DB are removed
     * from the cache. Also any result sets that are in the DB but not in the cache are cleaned out of the DB.
     */
    public void syncWithDB() {
        int outOfSyncCount = 0;
        int notInCacheCount = 0;

        for(String key : (List<String>) arsCache.getKeys()) {
            AtlasResultSet ars = (AtlasResultSet) arsCache.get(key).getValue();

            if(!ars.isAvailableInDB()) {
                arsCache.remove(key);
                outOfSyncCount++;
            }
        }

        Connection conn = null;

        try {
            conn = ArrayExpressSearchService.instance().getMEMConnection();
            PreparedStatement memstm = conn.prepareStatement("SELECT DISTINCT idkey FROM ATLAS");

            ResultSet rs = memstm.executeQuery();
            while(rs.next()) {
                String idkey = rs.getString(1);

                if(!arsCache.isKeyInCache(idkey)) {
                    AtlasResultSet ars = new AtlasResultSet(idkey);
                    arsCache.put(new Element(idkey, ars));
                    notInCacheCount++;
                }
            }
        } catch (SQLException e) {
            log.error(e);
        } finally {
            if (conn != null) try { conn.close(); } catch (Exception e) {}
        }

        log.info("Synchronized cache with DB: " + outOfSyncCount + " result sets cleaned up, "
                                                + notInCacheCount + " result sets added to cache, "
                                                + arsCache.getSize() + " result sets total in cache.");
    }
}