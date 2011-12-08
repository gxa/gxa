package uk.ac.ebi.gxa.data;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Robert Petryszak
 */
public class BestDesignElementCandidateTest {

    private static final int DONTCARE = 0;
    private static final BestDesignElementCandidate BC1 = new BestDesignElementCandidate(0.1f, 2f, DONTCARE, DONTCARE);
    private static final BestDesignElementCandidate BC2 = new BestDesignElementCandidate(0.3f, -3f, DONTCARE, DONTCARE);
    private static final BestDesignElementCandidate BC3 = new BestDesignElementCandidate(0.5f, 3f, DONTCARE, DONTCARE);
    private static final BestDesignElementCandidate BC4 = new BestDesignElementCandidate(1.1f, 3f, DONTCARE, DONTCARE);
    private static final BestDesignElementCandidate BC5 = new BestDesignElementCandidate(Float.NaN, 3f, DONTCARE, DONTCARE);

    @Test
    public void testCompareTo() {
        assertTrue("Higher absolute tStat should come first", BC2.compareTo(BC1) < 0);
        assertTrue("Higher absolute tStat should come first", BC3.compareTo(BC1) < 0);
        assertTrue("If absolute tStats are the same, lower pValue should come first", BC2.compareTo(BC3) < 0);
        assertTrue("If absolute tStats are the same, pVal > 1 should always come second", BC2.compareTo(BC4) < 0);
        assertTrue("If absolute tStats are the same, NaN pVal should always come second", BC2.compareTo(BC5) < 0);
        assertTrue("If absolute tStats are the same, pVal > 1 should always come second", BC4.compareTo(BC2) > 0);
        assertTrue("If absolute tStats are the same, NaN pVal should always come second", BC5.compareTo(BC2) > 0);
    }
}
