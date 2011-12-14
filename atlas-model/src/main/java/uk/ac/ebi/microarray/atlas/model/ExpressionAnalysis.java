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

import java.io.Serializable;

/**
 * @author Tony Burdett
 */
public class ExpressionAnalysis implements Serializable, Comparable<ExpressionAnalysis> {
    public static final long serialVersionUID = -6759797835522535043L;

    private final String arrayDesignAccession;
    private final String designElementAccession;  // we don't care about it
    // Index of a design element (in netcdf file) in which data to populate this object were found
    private final int designElementIndex;

    private final String efName;
    private final String efvName;

    private float tStatistic;
    private float pValAdjusted;

    public ExpressionAnalysis(String arrayDesignAccession, String designElementAccession, int designElementIndex, String efName, String efvName, float tStatistic, float pValAdjusted) {
        this.arrayDesignAccession = arrayDesignAccession;
        this.designElementAccession = designElementAccession;
        this.designElementIndex = designElementIndex;
        this.efName = efName;
        this.efvName = efvName;
        this.tStatistic = tStatistic;
        this.pValAdjusted = pValAdjusted;
    }

    public String getArrayDesignAccession() {
        return arrayDesignAccession;
    }

    public Integer getDesignElementIndex() {
        return designElementIndex;
    }

    public String getEfName() {
        return efName;
    }

    public String getEfvName() {
        return efvName;
    }

    public String getDesignElementAccession() {
        return designElementAccession;
    }

    public float getPValAdjusted() {
        return pValAdjusted;
    }

    public float getTStatistic() {
        return tStatistic;
    }

    public void setPValAdjusted(float pValAdjusted) {
        this.pValAdjusted = pValAdjusted;
    }

    public void setTStatistic(float tStatistic) {
        this.tStatistic = tStatistic;
    }

    public int compareTo(ExpressionAnalysis o) {
        assert o.pValAdjusted >= 0 && o.pValAdjusted <= 1;
        assert pValAdjusted >= 0 && pValAdjusted <= 1;
        return Float.valueOf(o.pValAdjusted).compareTo(pValAdjusted);
    }

    public boolean isUp() {
        return UpDownExpression.isUp(pValAdjusted, tStatistic);
    }

    public boolean isNo() {
        return UpDownExpression.isNonDe(pValAdjusted, tStatistic);
    }

    public boolean isDown() {
        return UpDownExpression.isDown(pValAdjusted, tStatistic);
    }

    public UpDownExpression getUpDownExpression() {
        return UpDownExpression.valueOf(pValAdjusted, tStatistic);
    }

    @Override
    public String toString() {
        return "ExpressionAnalysis{" +
                "efName='" + efName + '\'' +
                ", efvName='" + efvName + '\'' +
                //", experimentID=" + experimentID +
                ", designElementAccession=" + designElementAccession +
                ", tStatistic=" + tStatistic +
                ", pValAdjusted=" + pValAdjusted +
                ", designElementIndex=" + designElementIndex +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExpressionAnalysis that = (ExpressionAnalysis) o;

        if (!designElementAccession.equals(that.designElementAccession)) return false;
        if (Float.compare(that.pValAdjusted, pValAdjusted) != 0) return false;
        if (Float.compare(that.tStatistic, tStatistic) != 0) return false;
        if (efName != null ? !efName.equals(that.efName) : that.efName != null) return false;
        if (efvName != null ? !efvName.equals(that.efvName) : that.efvName != null) return false;
        if (designElementIndex != that.designElementIndex) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = efName != null ? efName.hashCode() : 0;
        result = 31 * result + (efvName != null ? efvName.hashCode() : 0);
        //result = 31 * result + (int) (experimentID ^ (experimentID >>> 32));
        result = 31 * result + designElementAccession.hashCode();
        result = 31 * result + (tStatistic != +0.0f ? Float.floatToIntBits(tStatistic) : 0);
        result = 31 * result + (pValAdjusted != +0.0f ? Float.floatToIntBits(pValAdjusted) : 0);
        result = 31 * result + designElementIndex;

        return result;
    }
}
