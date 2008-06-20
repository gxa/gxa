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
    private Cache arsSearchKeyCache;

    public AtlasResultSetCache() {
        arsCache            = CacheManager.getInstance().getCache("AtlasResultSetCache");
        arsSearchKeyCache   = CacheManager.getInstance().getCache("AtlasResultSetSearchKeyCache");
    }

    public boolean containsKey(String searchKey) {
        if(arsSearchKeyCache.isKeyInCache(searchKey)) {
            return arsCache.isKeyInCache(arsSearchKeyCache.get(searchKey).getValue());
        }

        return false;
    }

    public AtlasResultSet get(String searchKey) {
        String arsIdKey = (String) arsSearchKeyCache.get(searchKey).getValue();

        if (null == arsIdKey)
            return null;

        return (AtlasResultSet) arsCache.get(arsIdKey).getValue();
    }

    public void put(AtlasResultSet ars) {
        arsSearchKeyCache.put(new Element(ars.getSearchKey(), ars.getIdkey()));
        arsCache.put(new Element(ars.getIdkey(), ars));
    }

    public int size() {
        assert (arsSearchKeyCache.getSize() == arsCache.getSize()) : "AtlasResultSetSearchKey Cache and AtlasResultSet Cache sizes not equal!";

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
                arsSearchKeyCache.remove(ars.getSearchKey());
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

        assert (arsSearchKeyCache.getSize() == arsCache.getSize()) : "AtlasResultSetSearchKey Cache and AtlasResultSet Cache sizes not equal!";
    }
}
