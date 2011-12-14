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
import net.java.quickcheck.generator.PrimitiveGenerators;

import static java.util.Arrays.asList;
import static net.java.quickcheck.generator.CombinedGenerators.*;
import static net.java.quickcheck.generator.PrimitiveGenerators.doubles;

/**
 * @author alf
 */
public class BestDesignElementCandidateGenerators {

    public static Generator<Double> validPValues() {
        return ensureValues(asList(0.0, 0.5, 1.0), doubles(0.0, 1.0));
    }

    public static Generator<Double> validTStats() {
        return ensureValues(
                asList(0.0, 0.5, 1.0, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY),
                doubles(Float.MIN_VALUE, Float.MAX_VALUE));
    }

    public static Generator<Double> invalidPValues() {
        return ensureValues(asList(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN),
                oneOf(excludeValues(doubles(-Float.MAX_VALUE, -0.0), +0.0, -0.0))
                        .add(excludeValues(doubles(1.0, Float.MAX_VALUE), 1.0)));
    }

    public static Generator<Double> invalidTStats() {
        return ensureValues(Double.NaN);
    }

    public static Generator<DesignElementStatistics> deCandidates() {
        final Generator<Double> p = validPValues();
        final Generator<Double> t = validTStats();
        final Generator<Integer> de = PrimitiveGenerators.integers();
        final Generator<Integer> uefv = PrimitiveGenerators.integers();

        return new Generator<DesignElementStatistics>() {
            @Override
            public DesignElementStatistics next() {
                return new DesignElementStatistics(
                        p.next().floatValue(),
                        t.next().floatValue(),
                        de.next(), uefv.next());
            }
        };
    }
}
