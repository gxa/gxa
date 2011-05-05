package uk.ac.ebi.microarray.atlas.model;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class AssumptionsTest {
    @Test
    public void testSplit() {
        assertArrayEquals("".split(","), new String[]{""});
    }
}
