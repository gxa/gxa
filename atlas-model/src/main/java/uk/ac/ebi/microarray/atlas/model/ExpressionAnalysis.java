/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.microarray.atlas.model;

import uk.ac.ebi.gxa.utils.Pair;

/**
 * @author Tony Burdett
 */
public class ExpressionAnalysis implements DesignElementStatistics {
    private final String arrayDesignAccession;
    private final String designElementAccession;  // we don't care about it
    // Index of a design element (in netcdf file) in which data to populate this object were found
    private final int designElementIndex;

    private float tStatistic;
    private float pValAdjusted;
    private final Pair<String, String> efv;
    private final UpDownExpression expression;

    public ExpressionAnalysis(String arrayDesignAccession, String designElementAccession, int designElementIndex, String efName, String efvName, float tStatistic, float pValAdjusted) {
        this.arrayDesignAccession = arrayDesignAccession;
        this.designElementAccession = designElementAccession;
        this.designElementIndex = designElementIndex;
        if (pValAdjusted > 1) {
            // As the NA pvals/tstats  currently come back from ncdfs as 1.0E30, we convert them to Float.NaN
            this.tStatistic = Float.NaN;
            this.pValAdjusted = Float.NaN;
        } else {
            this.tStatistic = tStatistic;
            this.pValAdjusted = pValAdjusted;
        }
        efv = Pair.create(efName, efvName);
        expression = UpDownExpression.valueOf(this.pValAdjusted, this.tStatistic);
    }

    public String getArrayDesignAccession() {
        return arrayDesignAccession;
    }

    public int getDeIndex() {
        return designElementIndex;
    }

    @Override
    public Pair<String, String> getEfv() {
        return efv;
    }

    @Override
    public long getBioEntityId() {
        return 0;  //TODO
    }

    public String getDeAccession() {
        return designElementAccession;
    }

    public float getP() {
        return pValAdjusted;
    }

    public float getT() {
        return tStatistic;
    }

    public UpDownExpression getExpression() {
        return expression;
    }
}
