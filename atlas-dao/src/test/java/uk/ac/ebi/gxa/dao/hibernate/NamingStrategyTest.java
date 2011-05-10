package uk.ac.ebi.gxa.dao.hibernate;

import org.junit.Assert;
import org.junit.Test;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;

public class NamingStrategyTest {
    @Test
    public void testNamingStrategy() {
        AtlasNamingStrategy ns = new AtlasNamingStrategy();
        Assert.assertEquals("A2_ARRAYDESIGN", ns.classToTableName(ArrayDesign.class.getName()));
    }
}
