package uk.ac.ebi.gxa.utils;

import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.HashMap;

/**
 * @author pashky
 */
public class CollectionUtilTest {
    @Test
    public void test_makeMap() {
        Map<String,Integer> map = CollectionUtil.makeMap("a", 1, "b", 2, "c", 3, "d", 4);
        assertNotNull(map);
        assertEquals(4, map.size());
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
        assertEquals(3, map.get("c"));
        assertEquals(4, map.get("d"));
    }

    @Test
    public void test_addMap() {
        Map<String,Integer> map = new HashMap<String, Integer>();
        map.put("a", 1);
        map.put("b", 2);
        Map<String,Integer> map2 = CollectionUtil.addMap(map, "c", 3, "d", 4, "a", 5);
        assertNotNull(map2);
        assertEquals(4, map2.size());
        assertEquals(5, map2.get("a")); // check replace
        assertEquals(2, map2.get("b"));
        assertEquals(3, map2.get("c"));
        assertEquals(4, map2.get("d"));
    }
}
