package uk.ac.ebi.gxa.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static uk.ac.ebi.gxa.utils.NumberFormatUtil.prettyFloatFormat;

public class NumberFormatUtilTest {
    @Test
    public void testEmptyValue() {
        assertEquals("Empty value", "<nobr>9.97 &#0215; 10<span style=\"vertical-align: super;\">36</span></nobr>",
                prettyFloatFormat(9969209968386869000000000000000000000.000f));
    }

    @Test
    public void testNAN() {
        assertEquals("NAN", "<nobr>N/A</nobr>",
                prettyFloatFormat(Float.NaN));
    }

    @Test
    public void testEmptyValueExponential() {
        assertEquals("Empty value (exponential)", "<nobr>9.97 &#0215; 10<span style=\"vertical-align: super;\">36</span></nobr>",
                prettyFloatFormat(9.97e36f));
    }

    @Test
    public void testIntegerValue() {
        assertEquals("Integer value", "<nobr>1</nobr>",
                prettyFloatFormat(1.0f));
    }

    @Test
    public void testSimpleFloatValue() {
        assertEquals("Simple float value", "<nobr>1.01</nobr>",
                prettyFloatFormat(1.01f));
    }

    @Test
    public void testFloatAccuracyPositive() {
        assertEquals("Float accuracy - positive test", "<nobr>1.001</nobr>",
                prettyFloatFormat(1.001f));
    }

    @Test
    public void testFloatAccuracyNegative() {
        assertEquals("Float accuracy - negative test", "<nobr>1</nobr>",
                prettyFloatFormat(1.0001f));
    }
}
