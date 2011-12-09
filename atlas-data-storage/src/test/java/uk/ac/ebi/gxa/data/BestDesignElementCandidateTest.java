package uk.ac.ebi.gxa.data;

import org.junit.Test;

import static java.lang.Float.isNaN;
import static java.lang.Math.abs;
import static net.java.quickcheck.generator.iterable.Iterables.toIterable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Robert Petryszak
 */
public class BestDesignElementCandidateTest {
    @Test
    public void testCompareTo() {
        for (BestDesignElementCandidate candidate1 : toIterable(new BestDesignElementCandidateGenerator())) {
            for (BestDesignElementCandidate candidate2 : toIterable(new BestDesignElementCandidateGenerator())) {
                try {
                    if (candidate1.equals(candidate2))
                        assertEquals("equals-hashCode contract broken,", candidate1.hashCode(), candidate2.hashCode());

                    assertEquals("equals-compareTo contract broken,",
                            candidate1.equals(candidate2),
                            candidate1.compareTo(candidate2) == 0);

                    assertEquals("Antisymmetry broken, ",
                            candidate1.compareTo(candidate2),
                            -candidate2.compareTo(candidate1));

                    if (Float.compare(abs(candidate1.getTStat()), abs(candidate2.getTStat())) != 0) {
                        if (abs(candidate1.getTStat()) > abs(candidate2.getTStat()))
                            assertTrue("Higher absolute tStat should come first", candidate1.compareTo(candidate2) < 0);
                        else if (abs(candidate1.getTStat()) < abs(candidate2.getTStat()))
                            assertTrue("Higher absolute tStat should come first", candidate1.compareTo(candidate2) > 0);
                    } else {
                        if (isNaN(candidate1.getPValue())) {
                            assertEquals("Handling NaN P values",
                                    isNaN(candidate2.getPValue()) ? 0 : 1,
                                    candidate1.compareTo(candidate2));
                        } else {
                            System.out.println("candidate1 = " + candidate1);
                            System.out.println("candidate2 = " + candidate2);
                            if (candidate1.getPValue() < candidate2.getPValue())
                                assertTrue("P values ordering", candidate1.compareTo(candidate2) < 0);
                            if (candidate1.getPValue() > candidate2.getPValue())
                                assertTrue("P values ordering", candidate1.compareTo(candidate2) > 0);
                            if (candidate1.getPValue().equals(candidate2.getPValue()))
                                assertTrue("P values ordering", candidate1.compareTo(candidate2) == 0);
                        }
                    }
                } catch (AssertionError e) {
                    System.err.println("candidate1=" + candidate1);
                    System.err.println("candidate2=" + candidate2);
                    throw e;
                }
            }
        }
    }
}
