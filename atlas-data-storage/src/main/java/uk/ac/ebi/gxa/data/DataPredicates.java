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
    private final ExperimentWithData ewd;
    private final Logger log = LoggerFactory.getLogger(DataPredicates.class);

    public DataPredicates(ExperimentWithData ewd) {
        this.ewd = ewd;
    }

    public Predicate<ArrayDesign> containsGenes(final Collection<Long> geneIds) {
        if (geneIds == null || geneIds.isEmpty()) {
            return Predicates.alwaysTrue();
        }

        return new Predicate<ArrayDesign>() {
            public boolean apply(@Nonnull ArrayDesign arrayDesign) {
                try {
                    return Longs.asList(ewd.getGenes(arrayDesign)).containsAll(geneIds);
                } catch (AtlasDataException e) {
                    log.error("Failed to retrieve data for pair: " + pairToString(arrayDesign), e);
                    return false;
                }
            }

            @Override
            public String toString() {
                return "ContainsGenes(" + geneIds + ")";
            }
        };
    }

    public Predicate<ArrayDesign> containsAtLeastOneGene(final Collection<Long> geneIds) {
        return or(transform(geneIds, new Function<Long, Predicate<? super ArrayDesign>>() {
            public Predicate<? super ArrayDesign> apply(@Nonnull Long input) {
                return containsGenes(Arrays.asList(input));
            }
        }));
    }

    public Predicate<ArrayDesign> containsEfEfv(final String ef, final String efv) {
        return
            new Predicate<ArrayDesign>() {
                public boolean apply(@Nonnull ArrayDesign arrayDesign) {
                    try {
                        for (KeyValuePair uefv : ewd.getUniqueFactorValues(arrayDesign)) {
                            if (uefv.key.equals(ef) &&
                                (isNullOrEmpty(efv) || uefv.value.equals(efv))) {
                                return true;
                            }
                        }
                        return false;
                    } catch (AtlasDataException e) {
                        log.error("Cannot read pair " + pairToString(arrayDesign), e);
                        return false;
                    }
                }

                @Override
                public String toString() {
                    return isNullOrEmpty(efv)
                        ? "HasEF(" + ef + ")" : "HasEFV(" + ef + "||" + efv + ")";
                }
            };
    }

    private String pairToString(ArrayDesign arrayDesign) {
        return ewd.getExperiment().getAccession() + "/" + arrayDesign.getAccession();
    }
}
