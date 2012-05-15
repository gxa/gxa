package ae3.service.structuredquery;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Robert Petryszak
 */
public class AutoCompleteResultTest {
    private static final AutoCompleteResult RESULT = new AutoCompleteResult();

    private static final List<String> TYPES = Arrays.asList("gene", "efv", "efo", "efoefv");
    private static final AutoCompleteItem AC1 = new AutoCompleteItem("property1", "id1", "value", 1L);
    private static final AutoCompleteItem AC2 = new AutoCompleteItem("property2", "id2", "value", 1L);
    private static final AutoCompleteItem AC3 = new AutoCompleteItem("property3", "id3", "value1", 1L);
    private static final AutoCompleteItem AC4 = new AutoCompleteItem("property4", "id4", "value2", 1L);


    @Before
    public void setUp() throws Exception {
        RESULT.add(AC1);
        RESULT.add(AC2);
        RESULT.add(AC3);
        RESULT.add(AC4);
    }


    @Test
    public void testAutoCompleteResult() {
        List<AutoCompleteItem> res;
        for (String type : TYPES) {
            res = RESULT.getResults(type);
            if ("efoefv".equals(type)) {
                assertEquals(3, res.size());
                assertEquals(AC1, res.get(0));
                assertEquals(AC3, res.get(1));
                assertEquals(AC4, res.get(2));
            } else {
                assertEquals(4, res.size());
                assertEquals(AC1, res.get(0));
                assertEquals(AC2, res.get(1));
                assertEquals(AC3, res.get(2));
                assertEquals(AC4, res.get(3));
            }
        }
    }
}
