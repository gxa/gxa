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
 * @deprecated use {@link DesignElementStatistics instead}
 */
@Deprecated
public class ExpressionAnalysis implements DesignElementStatistics {
    private final String arrayDesignAccession;
    private final DesignElementStatistics statistics;

    public ExpressionAnalysis(String arrayDesignAccession, DesignElementStatistics statistics) {
        this.arrayDesignAccession = arrayDesignAccession;
        this.statistics = statistics;
    }

    public String getArrayDesignAccession() {
        return arrayDesignAccession;
    }

    @Override
    public int getDeIndex() {
        return statistics.getDeIndex();
    }

    @Override
    public Pair<String, String> getEfv() {
        return statistics.getEfv();
    }

    @Override
    public long getBioEntityId() {
        return statistics.getBioEntityId();
    }

    @Override
    public String getDeAccession() {
        return statistics.getDeAccession();
    }

    @Override
    public float getP() {
        return statistics.getP();
    }

    @Override
    public float getT() {
        return statistics.getT();
    }

    @Override
    public UpDownExpression getExpression() {
        return statistics.getExpression();
    }
}
