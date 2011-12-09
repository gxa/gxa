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

    public static Generator<Double> pValues() {
        return ensureValues(asList(Double.NaN, 0.0, 1.0, 0.5, -1.0, 2.0));
    }

    public static Generator<Double> tStats() {
        return ensureValues(
                asList(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY),
                doubles(Float.MIN_VALUE, Float.MAX_VALUE));
    }

    public static Generator<BestDesignElementCandidate> deCandidates() {
        final Generator<Double> p = pValues();
        final Generator<Double> t = tStats();
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
