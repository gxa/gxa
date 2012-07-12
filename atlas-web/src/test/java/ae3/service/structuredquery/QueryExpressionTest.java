package ae3.service.structuredquery;

import org.junit.Test;

import static ae3.service.structuredquery.QueryExpression.*;
import static org.junit.Assert.assertEquals;

public class QueryExpressionTest {
    @Test
    public void testFuzzyLogicCompatibleWithValueOf() {
        for (QueryExpression qe : QueryExpression.values()) {
            final String name = qe.name();
            assertEquals("valueOf(" + qe + ")", valueOf(name), parseFuzzyString(name));
        }
    }

    @Test
    public void testFuzzyLogicCompatibleWithValueOfEmpty() {
        assertEquals(UP_DOWN, parseFuzzyString(""));
    }

    @Test
    public void testLowercase() {
        for (QueryExpression qe : QueryExpression.values()) {
            final String name = qe.name();
            assertEquals("valueOf(" + qe + ")", valueOf(name), parseFuzzyString(name.toLowerCase()));
        }
    }
}
