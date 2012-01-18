/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.UpDownExpression;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * @author alf
 */
public class StatisticsIterator {
    public static final Predicate<Pair<String, String>> NON_EMPTY_EFV = new Predicate<Pair<String, String>>() {
        @Override
        public boolean apply(@Nullable Pair<String, String> input) {
            return input != null && !isNullOrEmpty(input.getValue()) && !"(empty)".equals(input.getValue());
        }
    };

    private int i = -1, j = -1;

    private final int deCount;
    private final int efvCount;

    private final List<Pair<String, String>> uEFVs;
    private final long[] bioentities;
    private final TwoDFloatArray tstat;
    private final TwoDFloatArray pvals;

    private final ArrayDesign ad;
    private final Experiment experiment;

    private final Predicate<Long> bePredicate;
    private final Predicate<Pair<String, String>> efvPredicate;

    public StatisticsIterator(ExperimentWithData experimentWithData, ArrayDesign ad,
                              Predicate<Long> bePredicate, Predicate<Pair<String, String>> efvPredicate)
            throws AtlasDataException, StatisticsNotFoundException {
        this.ad = ad;
        this.bePredicate = bePredicate;
        this.efvPredicate = efvPredicate;

        experiment = experimentWithData.getExperiment();

        uEFVs = experimentWithData.getUniqueEFVs(ad);
        tstat = experimentWithData.getTStatistics(ad);
        pvals = experimentWithData.getPValues(ad);

        bioentities = experimentWithData.getGenes(ad);

        deCount = tstat.getRowCount();
        efvCount = uEFVs.size();
    }

    public UpDownExpression getExpression() {
        return UpDownExpression.valueOf(getP(), getT());
    }

    public int getEfvCount() {
        return efvCount;
    }

    public Pair<String, String> getEFV() {
        return uEFVs.get(j);
    }

    public int getIntegerBioEntityId() {
        return safelyCastToInt(bioentities[i]);
    }

    public float getT() {
        return tstat.get(i, j);
    }

    public float getP() {
        return pvals.get(i, j);
    }

    public boolean isEmpty() {
        return uEFVs.size() == 0;
    }

    public int getDeCount() {
        return deCount;
    }

    public boolean nextEFV() {
        for (j++; j < efvCount && !efvPredicate.apply(uEFVs.get(j)); j++) {
        }
        return j < efvCount;
    }

    public boolean nextBioEntity() {
        for (i++; i < deCount && !bePredicate.apply(bioentities[i]); i++) {
        }
        return i < deCount;
    }

    public String toString() {
        return experiment.getAccession() + "/" + ad.getAccession();
    }

    public DesignElementStatistics getDEStats() {
        return new DesignElementStatistics(getP(), getT(), i, j, getEFV());
    }

    private static int safelyCastToInt(long l) {
        if (l != (int) l)
            throw LogUtil.createUnexpected("bioEntityId: " + l + " is too large to be cast to int safely- unable to build bit index");
        return (int) l;
    }
}
