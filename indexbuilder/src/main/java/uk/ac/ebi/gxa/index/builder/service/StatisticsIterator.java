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

package uk.ac.ebi.gxa.index.builder.service;

import uk.ac.ebi.gxa.data.AtlasDataException;
import uk.ac.ebi.gxa.data.ExperimentWithData;
import uk.ac.ebi.gxa.data.StatisticsNotFoundException;
import uk.ac.ebi.gxa.data.TwoDFloatArray;
import uk.ac.ebi.gxa.index.builder.IndexBuilderException;
import uk.ac.ebi.gxa.statistics.PTRank;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.UpDownExpression;

import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * @author alf
 */
public class StatisticsIterator {
    private int i = -1, j = -1;

    private final int deCount;
    private final int efvCount;

    private final List<Pair<String, String>> uEFVs;
    private final long[] bioentities;
    private final TwoDFloatArray tstat;
    private final TwoDFloatArray pvals;

    private final ArrayDesign ad;
    private final Experiment experiment;

    public StatisticsIterator(ExperimentWithData experimentWithData, ArrayDesign ad) throws AtlasDataException, StatisticsNotFoundException {
        this.ad = ad;

        experiment = experimentWithData.getExperiment();

        uEFVs = experimentWithData.getUniqueEFVs(ad);
        tstat = experimentWithData.getTStatistics(ad);
        pvals = experimentWithData.getPValues(ad);

        bioentities = experimentWithData.getGenes(ad);

        deCount = tstat.getRowCount();
        efvCount = uEFVs.size();
    }

    private static int safelyCastToInt(long l) {
        int i = (int) l;
        if ((long) i != l)
            throw new IndexBuilderException("bioEntityId: " + i + " is too large to be cast to int safely- unable to build bit index");
        return i;
    }

    boolean isNA() {
        return getExpressionClass().isNA();
    }

    boolean isUp() {
        return getExpressionClass().isUp();
    }

    boolean isNonDe() {
        return getExpressionClass().isNonDe();
    }

    int getEfvCount() {
        return efvCount;
    }

    Pair<String, String> getEFV() {
        return uEFVs.get(j);
    }

    int getBioEntityId() {
        return safelyCastToInt(bioentities[i]);
    }

    private UpDownExpression getExpressionClass() {
        return UpDownExpression.valueOf(pvals.get(i, j), tstat.get(i, j));
    }

    PTRank getPTRank() {
        return PTRank.of(pvals.get(i, j), tstat.get(i, j));
    }

    boolean isEmpty() {
        return uEFVs.size() == 0;
    }

    public int getDeCount() {
        return deCount;
    }

    public boolean nextEFV() {
        j++;
        while (j < efvCount) {
            final Pair<String, String> efv = getEFV();
            if (isNullOrEmpty(efv.getValue())) {
                j++;
            } else if ("(empty)".equals(efv.getValue())) {
                j++;
            } else
                break;
        }
        return j < efvCount;
    }

    public boolean nextBioEntity() {
        i++;
        while (i < deCount) {
            if (bioentities[i] == 0) {
                // in order to create a bitindex used for unit tests,
                // use <code>|| (bioentities[i] != 516248 && bioentities[i] != 838592)</code>
                // so that you would only index the bio entities used in tests
                i++;
            } else if (isNA()) {
                // Exclude NA p/t vals from bit index
                i++;
            } else
                break;
        }
        return i < deCount;
    }

    public String toString() {
        return experiment.getAccession() + "/" + ad.getAccession();
    }
}
