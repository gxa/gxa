package uk.ac.ebi.gxa.data;

import net.java.quickcheck.Generator;
import net.java.quickcheck.generator.PrimitiveGenerators;
import org.junit.Test;

import static java.lang.Float.compare;
import static java.lang.Float.isNaN;
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
                    assertEquals("Equality must be reflexive,", 1, a.compareTo(a));
                    assertEquals("Equality must be reflexive,", 1, b.compareTo(b));
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
            BestDesignElementCandidate b = new BestDesignElementCandidate(pg.next().floatValue(),
                    a.getTStat(), someInt.next(), someInt.next());

            checkSameAbsTProperties(a, b);
        }
    }

    @Test
    public void testOrderingSemanticsWithNegatedT() {
        Generator<Double> pg = pValues();
        Generator<Integer> someInt = PrimitiveGenerators.integers();

        for (BestDesignElementCandidate a : toIterable(deCandidates())) {
            BestDesignElementCandidate b = new BestDesignElementCandidate(pg.next().floatValue(),
                    -a.getTStat(), someInt.next(), someInt.next());

            checkSameAbsTProperties(a, b);
        }
    }

    private void checkSameAbsTProperties(BestDesignElementCandidate a, BestDesignElementCandidate b) {
        if (isNaN(a.getPValue())) {
            assertEquals("Handling NaN P values",
                    isNaN(b.getPValue()) ? 0 : 1,
                    a.compareTo(b));
        } else {
            System.out.println("a = " + a);
            System.out.println("b = " + b);
            if (a.getPValue() < b.getPValue())
                assertTrue("P values ordering", a.compareTo(b) < 0);
            if (a.getPValue() > b.getPValue())
                assertTrue("P values ordering", a.compareTo(b) > 0);
            if (compare(a.getPValue(), b.getPValue()) == 0)
                assertTrue("P values ordering", a.compareTo(b) == 0);
        }
    }

    @Test
    public void testTOrderingSemantics() {
        for (BestDesignElementCandidate a : toIterable(deCandidates())) {
            for (BestDesignElementCandidate b : toIterable(deCandidates())) {
                try {
                    if (compare(abs(a.getTStat()), abs(b.getTStat())) == 0)
                        continue;
                    if (isNaN(a.getTStat()) && isNaN(b.getTStat()))
                        continue;

                    if (abs(a.getTStat()) > abs(b.getTStat()))
                        assertTrue("Higher absolute tStat should come first", a.compareTo(b) < 0);
                    else if (abs(a.getTStat()) < abs(b.getTStat()))
                        assertTrue("Higher absolute tStat should come first", a.compareTo(b) > 0);
                    else if (isNaN(a.getTStat()) && !isNaN(b.getTStat()))
                        assertTrue("NaNs come last", a.compareTo(b) > 0);
                    else if (!isNaN(a.getTStat()) && isNaN(b.getTStat()))
                        assertTrue("NaNs come last", a.compareTo(b) < 0);
                } catch (AssertionError e) {
                    System.err.println("a=" + a);
                    System.err.println("b=" + b);
                    throw e;
                }
            }
        }
    }
}
