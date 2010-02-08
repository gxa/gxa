package uk.ac.ebi.gxa.utils;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.util.Iterator;

/**
 * @author pashky
 */
public class FlattenIteratorTest {
    @Test
    public void test_flatten() {
        Iterator<Integer> iterator = new FlattenIterator<Integer, Integer>(CountIterator.zeroTo(10)) {
            public Iterator<Integer> inner(Integer outerValue) {
                return CountIterator.zeroTo(outerValue);
            }
        };

        int outCounter = 1;
        int inCounter = 0;
        while(iterator.hasNext()) {
            int v = iterator.next();
            assertEquals(inCounter, v);
            ++inCounter;
            if(inCounter >= outCounter) {
                inCounter = 0;
                ++outCounter;
            }
        }
        assertEquals(10, outCounter);
    }
}
