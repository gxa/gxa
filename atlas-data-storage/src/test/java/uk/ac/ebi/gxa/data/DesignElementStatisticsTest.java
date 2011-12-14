/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.data;

import net.java.quickcheck.Generator;
import net.java.quickcheck.collection.Pair;
import org.junit.Test;
import uk.ac.ebi.gxa.exceptions.UnexpectedException;

import static java.lang.Math.abs;
import static net.java.quickcheck.generator.CombinedGenerators.pairs;
import static net.java.quickcheck.generator.PrimitiveGenerators.integers;
import static net.java.quickcheck.generator.iterable.Iterables.toIterable;
import static org.junit.Assert.*;
import static uk.ac.ebi.gxa.data.StatisticsGenerators.*;

/**
 * @author Robert Petryszak
 */
public class DesignElementStatisticsTest {
    @Test
    public void testBasicContracts() {
        for (Pair<DesignElementStatistics, DesignElementStatistics> p :
                toIterable(pairs(deCandidates(), deCandidates()))) {
            final DesignElementStatistics a = p.getFirst();
            final DesignElementStatistics b = p.getSecond();
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

    @Test
    public void testInvalidPVals() {
        Generator<Double> pg = invalidPValues();
        Generator<Double> tg = validTStats();
        Generator<Integer> someInt = integers();

        for (Double p : toIterable(pg)) {
            for (Double t : toIterable(tg)) {
                checkValidityConstraints(someInt, p, t, "pVal:  " + p);
            }
        }
    }

    @Test
    public void testInvalidTStats() {
        Generator<Double> pg = validPValues();
        Generator<Double> tg = invalidTStats();
        Generator<Integer> someInt = integers();

        for (Double p : toIterable(pg)) {
            for (Double t : toIterable(tg)) {
                checkValidityConstraints(someInt, p, t, "tStat:  " + t);
            }
        }
    }

    private void checkValidityConstraints(Generator<Integer> someInt, Double p, Double t, String diagnosis) {
        try {
            new DesignElementStatistics(p.floatValue(), t.floatValue(), someInt.next(), someInt.next());
            fail(diagnosis + " was invalid - an UnexpectedException should have been thrown");
        } catch (UnexpectedException ignored) {
            // as expected
        }
    }

    @Test
    public void testOrderingSemanticsWithSameT() {
        Generator<Double> pg = validPValues();
        Generator<Integer> someInt = integers();

        for (DesignElementStatistics a : toIterable(deCandidates())) {
            DesignElementStatistics b = new DesignElementStatistics(pg.next().floatValue(),
                    a.getTStat(), someInt.next(), someInt.next());

            checkSameAbsTProperties(a, b);
        }
    }

    @Test
    public void testOrderingSemanticsWithNegatedT() {
        Generator<Double> pg = validPValues();
        Generator<Integer> someInt = integers();

        for (DesignElementStatistics a : toIterable(deCandidates())) {
            DesignElementStatistics b = new DesignElementStatistics(pg.next().floatValue(),
                    -a.getTStat(), someInt.next(), someInt.next());

            checkSameAbsTProperties(a, b);
        }
    }

    private void checkSameAbsTProperties(DesignElementStatistics a, DesignElementStatistics b) {
        if (a.getPValue() < b.getPValue())
            assertTrue("P values ordering", a.compareTo(b) < 0);
        if (a.getPValue() > b.getPValue())
            assertTrue("P values ordering", a.compareTo(b) > 0);
        if (a.getPValue() == b.getPValue())
            assertTrue("P values ordering", a.compareTo(b) == 0);
    }

    @Test
    public void testTOrderingSemantics() {
        for (DesignElementStatistics a : toIterable(deCandidates())) {
            for (DesignElementStatistics b : toIterable(deCandidates())) {
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
