package uk.ac.ebi.gxa.data;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.primitives.Longs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;

import uk.ac.ebi.microarray.atlas.model.ArrayDesign;

import static com.google.common.base.Predicates.or;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Collections2.transform;

/**
 * @author Alexey Filippov
 */
public class DataPredicates {
    public static class Pair {
        final ExperimentWithData experiment;
        final ArrayDesign arrayDesign;

        Pair(ExperimentWithData experiment, ArrayDesign arrayDesign) {
            this.experiment = experiment;
            this.arrayDesign = arrayDesign;
        }

        @Override
        public String toString() {
            return experiment.getExperiment().getAccession() + "/" + arrayDesign.getAccession();
        }
    }

    private final Logger log = LoggerFactory.getLogger(DataPredicates.class);

    public Predicate<Pair> hasArrayDesign(@Nonnull final String arrayDesign) {
        return new Predicate<Pair>() {
            public boolean apply(@Nonnull Pair pair) {
                return arrayDesign.equals(pair.arrayDesign.getAccession());
            }

            @Override
            public String toString() {
                return "HasArrayDesign(" + arrayDesign + ")";
            }
        };
    }

    public Predicate<Pair> containsGenes(final Collection<Long> geneIds) {
        if (geneIds == null || geneIds.isEmpty()) {
            return Predicates.alwaysTrue();
        }

        return new Predicate<Pair>() {
            public boolean apply(@Nonnull Pair pair) {
                try {
                    return Longs.asList(pair.experiment.getGenes(pair.arrayDesign)).containsAll(geneIds);
                } catch (AtlasDataException e) {
                    log.error("Failed to retrieve data for pair: " + pair, e);
                    return false;
                }
            }

            @Override
            public String toString() {
                return "ContainsGenes(" + geneIds + ")";
            }
        };
    }

    public Predicate<Pair> containsAtLeastOneGene(final Collection<Long> geneIds) {
        return or(transform(geneIds, new Function<Long, Predicate<? super Pair>>() {
            public Predicate<? super Pair> apply(@Nonnull Long input) {
                return containsGenes(Arrays.asList(input));
            }
        }));
    }

    public Predicate<Pair> containsEfEfv(final String ef, final String efv) {
        return isNullOrEmpty(efv) ?
                new Predicate<Pair>() {
                    public boolean apply(@Nonnull Pair pair) {
                        try {
                            for (KeyValuePair uefv : pair.experiment.getUniqueFactorValues(pair.arrayDesign)) {
                                if (uefv.key.equals(ef))
                                    return true;
                            }
                            return false;
                        } catch (AtlasDataException e) {
                            log.error("Cannot read pair " + pair, e);
                            return false;
                        }
                    }

                    @Override
                    public String toString() {
                        return "HasEF(" + ef + ")";
                    }
                } :
                new Predicate<Pair>() {
                    public boolean apply(@Nonnull Pair pair) {
                        try {
                            for (KeyValuePair uefv : pair.experiment.getUniqueFactorValues(pair.arrayDesign)) {
                                if (uefv.key.equals(ef) && uefv.value.equals(efv)) {
                                    return true;
                                }
                            }
                            return false;
                        } catch (AtlasDataException e) {
                            log.error("Cannot read pair " + pair, e);
                            return false;
                        }
                    }

                    @Override
                    public String toString() {
                        return "HasEFV(" + ef + "||" + efv + ")";
                    }
                };
    }
}
