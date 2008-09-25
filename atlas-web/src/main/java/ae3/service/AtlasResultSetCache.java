package ae3.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import net.sf.ehcache.*;
import net.sf.ehcache.event.CacheEventListener;

public class AtlasResultSetCache {
    private final Log log = LogFactory.getLog(getClass());
    private Cache arsCache;

    public AtlasResultSetCache() {
        arsCache = CacheManager.getInstance().getCache("AtlasResultSetCache");

        arsCache.getCacheEventNotificationService().registerListener(new CacheEventListener() {
            public void notifyElementRemoved(Ehcache ehcache, Element element) throws CacheException { }
            public void notifyElementPut(Ehcache ehcache, Element element) throws CacheException { }
            public void notifyElementUpdated(Ehcache ehcache, Element element) throws CacheException { }
            public void notifyElementExpired(Ehcache ehcache, Element element) { }
            public void notifyRemoveAll(Ehcache ehcache) {}
            public void dispose() {}
            public void notifyElementEvicted(Ehcache ehcache, Element element) {
                 ((AtlasResultSet)element.getValue()).cleanup();
            }
            public Object clone() throws java.lang.CloneNotSupportedException { return super.clone(); }
        });
    }

    public boolean containsKey(String searchKey) {
        return arsCache.isKeyInCache(searchKey);
    }

    public AtlasResultSet get(String searchKey) {
        Element element = arsCache.get(searchKey);
        return (AtlasResultSet) element.getValue();
    }

    public void put(AtlasResultSet ars) {
        arsCache.put(new Element(ars.getSearchKey(), ars));
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

        Set<String> existingUUIDs = new HashSet<String>();
        for(String key : (List<String>) arsCache.getKeys()) {
            AtlasResultSet ars = (AtlasResultSet) arsCache.get(key).getValue();

            if(!ars.isAvailableInDB()) {
                arsCache.remove(key);
                outOfSyncCount++;
            } else {
                existingUUIDs.add(ars.getIdkey());
            }
        }

        Connection conn = null;

        try {
            conn = ArrayExpressSearchService.instance().getMEMConnection();
            PreparedStatement memstm = conn.prepareStatement("SELECT DISTINCT idkey FROM ATLAS");

            ResultSet rs = memstm.executeQuery();
            while(rs.next()) {
                String idkey = rs.getString(1);

                if(!existingUUIDs.contains(idkey)) {
                    AtlasResultSet ars = new AtlasResultSet(idkey);
                    ars.setIdkey(idkey);
                    ars.cleanup();
                    notInCacheCount++;
                }
            }
        } catch (SQLException e) {
            log.error(e);
        } finally {
            if (conn != null) try { conn.close(); } catch (Exception e) {}
        }

        log.info("Synchronized cache with DB: " + outOfSyncCount + " result sets cleaned up from cache, "
                                                + notInCacheCount + " result sets cleaned up from DB, "
                                                + arsCache.getSize() + " result sets total in cache.");

    }
}
