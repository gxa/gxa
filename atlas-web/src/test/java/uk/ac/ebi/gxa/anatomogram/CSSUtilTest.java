package uk.ac.ebi.gxa.anatomogram;

import junit.framework.Assert;
import org.junit.Test;

/**
 */
public class CSSUtilTest {
    @Test
    public void testAttributeReplacement() {
        Assert.assertEquals("fill:#ffffff\n" +
                "fill:#ffffff\n" +
                "fill:#ffffff\n" +
                "fill:#ffffff;",
                CSSUtil.replaceColor("fill:#ffffff\n" +
                        "fill:#0000ff\n" +
                        "fill:none\n" +
                        "fill:#fff0fa;", "fill", "#ffffff"));
    }
}
