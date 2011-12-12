package uk.ac.ebi.gxa.data;

import net.java.quickcheck.Generator;
import net.java.quickcheck.generator.PrimitiveGenerators;

import static java.util.Arrays.asList;
import static net.java.quickcheck.generator.CombinedGenerators.ensureValues;
import static net.java.quickcheck.generator.PrimitiveGenerators.doubles;

/**
 * @author alf
 */
public class BestDesignElementCandidateGenerators {

    public static Generator<Double> validPValues() {
        return ensureValues(asList(0.0, 1.0, 0.5));
    }

    public static Generator<Double> validTStats() {
        return ensureValues(
                asList(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY),
                doubles(Float.MIN_VALUE, Float.MAX_VALUE));
    }

    public static Generator<Double> invalidPValues() {
        return ensureValues(asList(Double.NaN, -1.0, 2.0));
    }

    public static Generator<Double> invalidTStats() {
        return ensureValues(asList(Double.NaN));
    }

    public static Generator<BestDesignElementCandidate> deCandidates() {
        final Generator<Double> p = validPValues();
        final Generator<Double> t = validTStats();
        final Generator<Integer> de = PrimitiveGenerators.integers();
        final Generator<Integer> uefv = PrimitiveGenerators.integers();

        return new Generator<BestDesignElementCandidate>() {
            @Override
            public BestDesignElementCandidate next() {
                return new BestDesignElementCandidate(
                        p.next().floatValue(),
                        t.next().floatValue(),
                        de.next(), uefv.next());
            }
        };
    }

    public static Generator<BestDesignElementCandidate> dECandidatesWithInvalidPVals() {
        final Generator<Double> p = invalidPValues();
        final Generator<Double> t = validTStats();
        final Generator<Integer> de = PrimitiveGenerators.integers();
        final Generator<Integer> uefv = PrimitiveGenerators.integers();

        return new Generator<BestDesignElementCandidate>() {
            @Override
            public BestDesignElementCandidate next() {
                return new BestDesignElementCandidate(
                        p.next().floatValue(),
                        t.next().floatValue(),
                        de.next(), uefv.next());
            }
        };
    }

    public static Generator<BestDesignElementCandidate> dECandidatesWithInvalidTStats() {
        final Generator<Double> p = validPValues();
        final Generator<Double> t = invalidTStats();
        final Generator<Integer> de = PrimitiveGenerators.integers();
        final Generator<Integer> uefv = PrimitiveGenerators.integers();

        return new Generator<BestDesignElementCandidate>() {
            @Override
            public BestDesignElementCandidate next() {
                return new BestDesignElementCandidate(
                        p.next().floatValue(),
                        t.next().floatValue(),
                        de.next(), uefv.next());
            }
        };
    }
}
