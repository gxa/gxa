package uk.ac.ebi.gxa.data;

import net.java.quickcheck.Generator;
import net.java.quickcheck.generator.PrimitiveGenerators;

import static java.util.Arrays.asList;
import static net.java.quickcheck.generator.CombinedGenerators.ensureValues;
import static net.java.quickcheck.generator.PrimitiveGenerators.doubles;

/**
 * @author alf
 */
public class BestDesignElementCandidateGenerator implements Generator<BestDesignElementCandidate> {
    private Generator<Double> p = ensureValues(
            asList(Double.NaN, 0.0, 1.0, 0.5, -1.0, 2.0));
    private Generator<Double> t = ensureValues(
            asList(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY),
            doubles(Float.MIN_VALUE, Float.MAX_VALUE));
    private Generator<Integer> de = PrimitiveGenerators.integers(0, Integer.MAX_VALUE);
    private Generator<Integer> uefv = PrimitiveGenerators.integers(0, Integer.MAX_VALUE);

    @Override
    public BestDesignElementCandidate next() {
        return new BestDesignElementCandidate(p.next().floatValue(),
                t.next().floatValue(),
                de.next(), uefv.next());
    }
}
