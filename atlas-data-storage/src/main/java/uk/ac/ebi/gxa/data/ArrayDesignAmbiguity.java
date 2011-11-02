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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import javax.annotation.Nonnull;
import java.util.Collection;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * @author Olga Melnichuk
 */
public class ArrayDesignAmbiguity {
    private static final Logger log = LoggerFactory.getLogger(ArrayDesignAmbiguity.class);
    private Predicate<ExperimentPart> criteria = Predicates.alwaysTrue();

    public ExperimentPart resolve(ExperimentWithData ewd) {
        Experiment exp = ewd.getExperiment();
        for (ArrayDesign ad : exp.getArrayDesigns()) {
            ExperimentPart expPart = new ExperimentPart(ewd, ad);
            if (criteria.apply(expPart)) {
                return expPart;
            }
        }
        return null;
    }

    public ArrayDesignAmbiguity containsEfEfv(final String ef, final String efv) {
        if (isNullOrEmpty(ef)) {
            return this;
        }
        return addCriteria(
                new Predicate<ExperimentPart>() {
                    public boolean apply(@Nonnull ExperimentPart expPart) {
                        try {
                            for (KeyValuePair efEfv : expPart.getUniqueFactorValues()) {
                                if (efEfv.key.equals(ef) &&
                                        (isNullOrEmpty(efv) || efEfv.value.equals(efv))) {
                                    return true;
                                }
                            }
                        } catch (AtlasDataException e) {
                            log.error("Cannot read unique factor values for " + expPart.toString(), e);
                        } catch (StatisticsNotFoundException e) {
                            log.error("No statistics were found for " + expPart.toString());
                        }
                        return false;
                    }

                    @Override
                    public String toString() {
                        return isNullOrEmpty(efv)
                                ? "HasEF(" + ef + ")" : "HasEFV(" + ef + "||" + efv + ")";
                    }
                });
    }

    public ArrayDesignAmbiguity containsGenes(final Collection<Long> geneIds) {
        if (geneIds == null || geneIds.isEmpty()) {
            return this;
        }

        return addCriteria(new Predicate<ExperimentPart>() {
            public boolean apply(@Nonnull ExperimentPart expPart) {
                try {
                    return expPart.getGeneIds().containsAll(geneIds);
                } catch (AtlasDataException e) {
                    log.error("Failed to retrieve data for pair: " + expPart.toString(), e);
                    return false;
                }
            }

            @Override
            public String toString() {
                return "ContainsGenes(" + geneIds + ")";
            }
        });
    }

    private ArrayDesignAmbiguity addCriteria(Predicate<ExperimentPart> predicate) {
        criteria = Predicates.and(criteria, predicate);
        return this;
    }
}
