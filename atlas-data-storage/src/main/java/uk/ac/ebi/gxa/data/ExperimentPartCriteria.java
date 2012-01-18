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
import javax.annotation.Nullable;
import java.util.Collection;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Iterables.filter;

/**
 * @author Olga Melnichuk
 */
public class ExperimentPartCriteria {

    private static final Logger log = LoggerFactory.getLogger(ExperimentPartCriteria.class);

    private Predicate<ExperimentPart> criteria;

    private ExperimentPartCriteria(Predicate<ExperimentPart> criteria) {
        this.criteria = criteria;
    }

    public ExperimentPart retrieveFrom(ExperimentWithData ewd) {
        Experiment exp = ewd.getExperiment();
        for (ArrayDesign ad : exp.getArrayDesigns()) {
            ExperimentPart expPart = new ExperimentPart(ewd, ad);
            if (criteria.apply(expPart)) {
                return expPart;
            }
        }
        return null;
    }

    public ExperimentPartCriteria containsEfEfv(String ef, String efv) {
        if (isNullOrEmpty(ef)) {
            throw new IllegalArgumentException("'ef' argument can not be null or empty");
        }
        return addCriteria(ExperimentPartPredicates.containsEfEfv(ef, efv));
    }

    public ExperimentPartCriteria containsGenes(Collection<Long> geneIds) {
        if (geneIds == null || geneIds.isEmpty()) {
            throw new IllegalArgumentException("'geneIds' argument can not be null or empty");
        }
        return addCriteria(ExperimentPartPredicates.containsGenes(geneIds));
    }

    public ExperimentPartCriteria containsAtLeastOneGene(@Nonnull Predicate<Long> geneIds) {
        return addCriteria(ExperimentPartPredicates.containsAtLeastOneGene(geneIds));
    }

    public ExperimentPartCriteria containsDeAccessions(Collection<String> deAccessions) {
        if (deAccessions == null || deAccessions.isEmpty()) {
            throw new IllegalArgumentException("'deAccessions' argument can not be null or empty");
        }
        return addCriteria(ExperimentPartPredicates.containsDeAccessions(deAccessions));
    }

    public ExperimentPartCriteria hasArrayDesignAccession(String adAccession) {
        if (isNullOrEmpty(adAccession)) {
            throw new IllegalArgumentException("'adAccession' argument can not be null or empty");
        }
        return addCriteria(ExperimentPartPredicates.hasArrayDesignAccession(adAccession));
    }

    private ExperimentPartCriteria addCriteria(Predicate<ExperimentPart> predicate) {
        criteria = Predicates.and(criteria, predicate);
        return this;
    }

    public static ExperimentPartCriteria experimentPart() {
        return new ExperimentPartCriteria(Predicates.<ExperimentPart>alwaysTrue());
    }

    private static class ExperimentPartPredicates {

        static Predicate<ExperimentPart> containsGenes(final Collection<Long> geneIds) {
            return new Predicate<ExperimentPart>() {
                @Override
                public boolean apply(@Nonnull ExperimentPart expPart) {
                    try {
                        return expPart.getGeneIds().containsAll(geneIds);
                    } catch (AtlasDataException e) {
                        log.error("Failed to retrieve data for pair: " + expPart.toString(), e);
                        return false;
                    }
                }
            };
        }

        static Predicate<ExperimentPart> containsAtLeastOneGene(final Predicate<Long> genes) {
            return new Predicate<ExperimentPart>() {
                @Override
                public boolean apply(@Nullable ExperimentPart expPart) {
                    try {
                        return expPart != null && filter(expPart.getGeneIds(), genes).iterator().hasNext();
                    } catch (AtlasDataException e) {
                        log.error("Failed to retrieve data for pair: " + expPart.toString(), e);
                        return false;
                    }
                }
            };
        }

        static Predicate<ExperimentPart> containsDeAccessions(final Collection<String> deAccessions) {
            return new Predicate<ExperimentPart>() {
                @Override
                public boolean apply(@Nonnull ExperimentPart expPart) {
                    try {
                        return expPart.containsDeAccessions(deAccessions);
                    } catch (AtlasDataException e) {
                        log.error("Failed to retrieve data for pair: " + expPart.toString(), e);
                        return false;
                    }
                }
            };
        }

        static Predicate<ExperimentPart> hasArrayDesignAccession(final String adAccession) {
            return new Predicate<ExperimentPart>() {
                @Override
                public boolean apply(@Nonnull ExperimentPart expPart) {
                    return adAccession.equals(expPart.getArrayDesign().getAccession());
                }
            };
        }

        static Predicate<ExperimentPart> containsEfEfv(final String ef, final String efv) {
            return new Predicate<ExperimentPart>() {
                @Override
                public boolean apply(@Nullable ExperimentPart expPart) {
                    try {
                        return expPart != null && expPart.hasEfEfv(ef, efv);
                    } catch (AtlasDataException e) {
                        log.error("Cannot read unique factor values for " + expPart.toString(), e);
                        return false;
                    } catch (StatisticsNotFoundException e) {
                        log.error("No statistics were found for " + expPart.toString());
                        return false;
                    }
                }
            };
        }

    }
}
