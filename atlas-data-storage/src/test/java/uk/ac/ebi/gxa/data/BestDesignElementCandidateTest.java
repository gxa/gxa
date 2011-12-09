package uk.ac.ebi.gxa.data;

import org.junit.Test;

import static java.lang.Math.abs;
import static net.java.quickcheck.generator.iterable.Iterables.toIterable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Robert Petryszak
 */
public class BestDesignElementCandidateTest {
    private static final int DONTCARE = 0;
    private static final BestDesignElementCandidate BC2 = new BestDesignElementCandidate(0.3f, -3f, DONTCARE, DONTCARE);
    private static final BestDesignElementCandidate BC3 = new BestDesignElementCandidate(0.5f, 3f, DONTCARE, DONTCARE);
    private static final BestDesignElementCandidate BC4 = new BestDesignElementCandidate(1.1f, 3f, DONTCARE, DONTCARE);
    private static final BestDesignElementCandidate BC5 = new BestDesignElementCandidate(Float.NaN, 3f, DONTCARE, DONTCARE);

    @Test
    public void testCompareTo() {
        for (BestDesignElementCandidate candidate1 : toIterable(new BestDesignElementCandidateGenerator())) {
            for (BestDesignElementCandidate candidate2 : toIterable(new BestDesignElementCandidateGenerator())) {
                try {
                    if (candidate1.equals(candidate2))
                        assertEquals("equals-hashCode contract broken,", candidate1.hashCode(), candidate2.hashCode());

                    assertEquals("equals-compareTo contract broken,",
                            candidate1.compareTo(candidate2) == 0,
                            candidate1.equals(candidate2));

                    if (abs(candidate1.getTStat()) > abs(candidate2.getTStat()))
                        assertTrue("Higher absolute tStat should come first", candidate1.compareTo(candidate2) < 0);

                    if (abs(candidate1.getTStat()) < abs(candidate2.getTStat()))
                        assertTrue("Higher absolute tStat should come first", candidate1.compareTo(candidate2) > 0);
                } catch (AssertionError e) {
                    System.err.println("candidate1=" + candidate1);
                    System.err.println("candidate2=" + candidate2);
                    throw e;
                }
            }
        }


        assertTrue("If absolute tStats are the same, lower pValue should come first", BC2.compareTo(BC3) < 0);
        assertTrue("If absolute tStats are the same, pVal > 1 should always come second", BC2.compareTo(BC4) < 0);
        assertTrue("If absolute tStats are the same, NaN pVal should always come second", BC2.compareTo(BC5) < 0);
        assertTrue("If absolute tStats are the same, pVal > 1 should always come second", BC4.compareTo(BC2) > 0);
        assertTrue("If absolute tStats are the same, NaN pVal should always come second", BC5.compareTo(BC2) > 0);
    }
}
