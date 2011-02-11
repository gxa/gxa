package uk.ac.ebi.gxa.anatomogram;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static uk.ac.ebi.gxa.anatomogram.svgutil.CSSUtil.replaceColor;

/**
 */
public class CSSUtilTest {
    @Test
    public void testAttributeReplacement() {
        assertEquals("fill:#ffffff\n" +
                "fill:#ffffff\n" +
                "fill:#ffffff\n" +
                "fill:#ffffff;\n" +
                "fill:#ffffff\n" +
                "fill:#ffffff\n" +
                "fill:#ffffff\n",
                replaceColor("fill:#ffffff\n" +
                        "fill:#0000ff\n" +
                        "fill:none\n" +
                        "fill:#fff0fa;\n" +
                        "fill:#0000ff\n" +
                        "fill:red\n" +
                        "fill:blue\n" +
                        "",
                        "fill",
                        "#ffffff"));
    }
}
