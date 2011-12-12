package uk.ac.ebi.gxa.data;

import net.java.quickcheck.Generator;
import net.java.quickcheck.generator.PrimitiveGenerators;
import org.junit.Test;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.gxa.exceptions.UnexpectedException;

import static java.lang.Math.abs;
import static net.java.quickcheck.generator.iterable.Iterables.toIterable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.ac.ebi.gxa.data.BestDesignElementCandidateGenerators.deCandidates;
import static uk.ac.ebi.gxa.data.BestDesignElementCandidateGenerators.pValues;

/**
 * @author Robert Petryszak
 */
public class BestDesignElementCandidateTest {
    @Test
    public void testBasicContracts() {
        for (BestDesignElementCandidate a : toIterable(deCandidates())) {
            for (BestDesignElementCandidate b : toIterable(deCandidates())) {
                try {
                    assertEquals("Equality must be reflexive,", 0, a.compareTo(a));
                    assertEquals("Equality must be reflexive,", 0, b.compareTo(b));
                    if (a.equals(b)) {
                        assertEquals("equals-hashCode contract broken,", a.hashCode(), b.hashCode());
                    }
                    assertEquals("equals-compareTo contract broken,", a.equals(b), a.compareTo(b) == 0);
                    assertEquals("Antisymmetry broken, ", a.compareTo(b), -b.compareTo(a));
                } catch (AssertionError e) {
                    System.err.println("a=" + a);
                    System.err.println("b=" + b);
                    throw e;
                }
            }
        }
    }

    @Test
    public void testOrderingSemanticsWithSameT() {
        Generator<Double> pg = pValues();
        Generator<Integer> someInt = PrimitiveGenerators.integers();

        for (BestDesignElementCandidate a : toIterable(deCandidates())) {
            float pVal = pg.next().floatValue();
            float tStat = a.getTStat();
            try {
                BestDesignElementCandidate b = new BestDesignElementCandidate(pVal, tStat, someInt.next(), someInt.next());
                checkSameAbsTProperties(a, b);
            } catch (UnexpectedException ue) {
                if (BestDesignElementCandidate.isPvalValid(pVal) && BestDesignElementCandidate.isTStatValid(tStat))
                    throw LogUtil.createUnexpected("At least one of pVal:  " + pVal + "; tstat: " + tStat + " should be invalid");
            }
        }
    }

    @Test
    public void testOrderingSemanticsWithNegatedT() {
        Generator<Double> pg = pValues();
        Generator<Integer> someInt = PrimitiveGenerators.integers();

        for (BestDesignElementCandidate a : toIterable(deCandidates())) {
            float pVal = pg.next().floatValue();
            float tStat = a.getTStat();
            try {
                BestDesignElementCandidate b = new BestDesignElementCandidate(pVal, -tStat, someInt.next(), someInt.next());
                checkSameAbsTProperties(a, b);
            } catch (UnexpectedException ue) {
                if (BestDesignElementCandidate.isPvalValid(pVal) && BestDesignElementCandidate.isTStatValid(tStat))
                    throw LogUtil.createUnexpected("At least one of pVal:  " + pVal + "; tstat: " + tStat + " should be invalid");
            }
        }
    }

    private void checkSameAbsTProperties(BestDesignElementCandidate a, BestDesignElementCandidate b) {
        if (a.getPValue() < b.getPValue())
            assertTrue("P values ordering", a.compareTo(b) < 0);
        if (a.getPValue() > b.getPValue())
            assertTrue("P values ordering", a.compareTo(b) > 0);
        if (a.getPValue() == b.getPValue())
            assertTrue("P values ordering", a.compareTo(b) == 0);
    }

    @Test
    public void testTOrderingSemantics() {
        for (BestDesignElementCandidate a : toIterable(deCandidates())) {
            for (BestDesignElementCandidate b : toIterable(deCandidates())) {
                try {
                    if (abs(a.getTStat()) == abs(b.getTStat()))
                        continue;

                    if (abs(a.getTStat()) > abs(b.getTStat()))
                        assertTrue("Higher absolute tStat should come first", a.compareTo(b) < 0);
                    else if (abs(a.getTStat()) < abs(b.getTStat()))
                        assertTrue("Higher absolute tStat should come first", a.compareTo(b) > 0);
                } catch (AssertionError e) {
                    System.err.println("a=" + a);
                    System.err.println("b=" + b);
                    throw e;
                }
            }
        }
    }
}
