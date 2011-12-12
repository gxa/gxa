package uk.ac.ebi.gxa.data;

import net.java.quickcheck.Generator;
import net.java.quickcheck.generator.PrimitiveGenerators;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.gxa.exceptions.UnexpectedException;

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
                BestDesignElementCandidate bestDesignElementCandidate = null;
                while (bestDesignElementCandidate == null) {
                    float pVal = p.next().floatValue();
                    float tStat = t.next().floatValue();
                    try {
                        bestDesignElementCandidate = new BestDesignElementCandidate(
                                pVal,
                                tStat,
                                de.next(), uefv.next());
                    } catch (UnexpectedException ue) {
                        if (BestDesignElementCandidate.isPvalValid(pVal) && BestDesignElementCandidate.isTStatValid(tStat))
                            throw LogUtil.createUnexpected("At least one of pVal:  " + pVal + "; tstat: " + tStat + " should be invalid");
                    }
                }
                return bestDesignElementCandidate;
            }
        };
    }

}
