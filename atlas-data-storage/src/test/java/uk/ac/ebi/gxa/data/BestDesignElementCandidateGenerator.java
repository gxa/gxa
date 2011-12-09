package uk.ac.ebi.gxa.data;

import net.java.quickcheck.Generator;
import net.java.quickcheck.generator.PrimitiveGenerators;

/**
 * @author alf
 */
public class BestDesignElementCandidateGenerator implements Generator<BestDesignElementCandidate> {
    private Generator<Double> p = PrimitiveGenerators.doubles(-2, 2);
    private Generator<Double> t = PrimitiveGenerators.doubles(Float.MIN_VALUE, Float.MAX_VALUE);
    private Generator<Integer> de = PrimitiveGenerators.integers(0, Integer.MAX_VALUE);
    private Generator<Integer> uefv = PrimitiveGenerators.integers(0, Integer.MAX_VALUE);

    @Override
    public BestDesignElementCandidate next() {
        return new BestDesignElementCandidate(p.next().floatValue(),
                t.next().floatValue(),
                de.next(), uefv.next());
    }
}
