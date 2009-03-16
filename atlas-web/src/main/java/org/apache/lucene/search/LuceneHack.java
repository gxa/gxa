package org.apache.lucene.search;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * @author pashky
 */
public class LuceneHack {
    private static Log log = LogFactory.getLog(LuceneHack.class);
    public static void cleanExtCache(IndexReader reader) {
        try {
            FieldCacheImpl fc = (FieldCacheImpl)ExtendedFieldCache.EXT_DEFAULT;
            Field f = FieldCacheImpl.Cache.class.getDeclaredField("readerCache");
            f.setAccessible(true);
            ((Map)f.get(fc.intsCache)).remove(reader);
            ((Map)f.get(fc.floatsCache)).remove(reader);
        } catch (Exception e) {
            log.error(e);
        }

    }
}
