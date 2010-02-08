package uk.ac.ebi.gxa.utils;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Iterator;

/**
 * @author pashky
 */
public class CountIteratorTest {

    @Test
    public void test_zeroTo() {
        int counter = 0;
        Iterator<Integer> counterIter = CountIterator.zeroTo(10);
        while(counterIter.hasNext()) {
            int value = counterIter.next();
            assertEquals(counter, value);
            ++counter;
        }
        assertEquals(10, counter);

        // border conditions
        assertFalse(CountIterator.zeroTo(0).hasNext());
        assertFalse(CountIterator.zeroTo(-1).hasNext());
    }

    @Test
    public void test_oneTo() {
        int counter = 1;
        Iterator<Integer> counterIter = CountIterator.oneTo(10);
        while(counterIter.hasNext()) {
            int value = counterIter.next();
            assertEquals(counter, value);
            ++counter;
        }
        assertEquals(11, counter);

        // border conditions
        assertFalse(CountIterator.oneTo(0).hasNext());
        assertFalse(CountIterator.oneTo(-1).hasNext());
    }
}
