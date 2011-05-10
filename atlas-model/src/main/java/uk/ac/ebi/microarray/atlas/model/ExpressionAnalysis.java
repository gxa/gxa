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
import java.util.Arrays;

import static java.util.Arrays.asList;

/**
 * @author Tony Burdett
 */
public class ExpressionAnalysis implements Serializable, Comparable<ExpressionAnalysis> {
    public static final long serialVersionUID = -6759797835522535043L;

    private String efName;
    private String efvName;
    private long experimentID;
    private String designElementAccession;  // we don't care about it
    private float tStatistic;
    private float pValAdjusted;
    private transient long efId;  // TODO: make it properly
    private transient long efvId; // TODO: make it properly
    private String[] efoAccessions;
    // Id (i.e. filename) of the proxy in which data to populate this object were found
    private String proxyId;
    // Index of a design element (in proxyId) in which data to populate this object were found
    private Integer designElementIndex;

    public String getProxyId() {
        return proxyId;
    }

    public void setProxyId(String proxyId) {
        this.proxyId = proxyId;
    }

    public Integer getDesignElementIndex() {
        return designElementIndex;
    }

    public void setDesignElementIndex(Integer designElementIndex) {
        this.designElementIndex = designElementIndex;
    }

    public String getEfName() {
        return efName;
    }

    public void setEfName(String efName) {
        this.efName = efName;
    }

    public String getEfvName() {
        return efvName;
    }

    public void setEfvName(String efvName) {
        this.efvName = efvName;
    }

    public long getExperimentID() {
        return experimentID;
    }

    public void setExperimentID(long experimentID) {
        this.experimentID = experimentID;
    }

    public String getDesignElementAccession() {
        return designElementAccession;
    }

    public void setDesignElementAccession(String designElementAccession) {
        this.designElementAccession = designElementAccession;
    }

    public float getPValAdjusted() {
        return pValAdjusted;
    }

    public void setPValAdjusted(float pValAdjusted) {
        this.pValAdjusted = pValAdjusted;
    }

    public float getTStatistic() {
        return tStatistic;
    }

    public void setTStatistic(float tStatistic) {
        this.tStatistic = tStatistic;
    }

    public long getEfId() {
        return efId;
    }

    public void setEfId(long efId) {
        this.efId = efId;
    }

    public long getEfvId() {
        return efvId;
    }

    public void setEfvId(long efvId) {
        this.efvId = efvId;
    }

    public String[] getEfoAccessions() {
        return (efoAccessions == null) ? null : asList(efoAccessions).toArray(new String[efoAccessions.length]);
    }

    public void setEfoAccessions(String[] efoAccessions) {
        this.efoAccessions = (efoAccessions == null) ? null : asList(efoAccessions).toArray(new String[efoAccessions.length]);
    }

    public int compareTo(ExpressionAnalysis o) {
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

    @Override
    public String toString() {
        return "ExpressionAnalysis{" +
                "efName='" + efName + '\'' +
                ", efvName='" + efvName + '\'' +
                ", experimentID=" + experimentID +
                ", designElementAccession=" + designElementAccession +
                ", tStatistic=" + tStatistic +
                ", pValAdjusted=" + pValAdjusted +
                ", efId=" + efId +
                ", efvId=" + efvId +
                ", proxyId=" + proxyId +
                ", designElementIndex=" + designElementIndex +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExpressionAnalysis that = (ExpressionAnalysis) o;

        if (!designElementAccession.equals(that.designElementAccession)) return false;
        if (efId != that.efId) return false;
        if (efvId != that.efvId) return false;
        if (experimentID != that.experimentID) return false;
        if (Float.compare(that.pValAdjusted, pValAdjusted) != 0) return false;
        if (Float.compare(that.tStatistic, tStatistic) != 0) return false;
        if (efName != null ? !efName.equals(that.efName) : that.efName != null) return false;
        if (!Arrays.equals(efoAccessions, that.efoAccessions)) return false;
        if (efvName != null ? !efvName.equals(that.efvName) : that.efvName != null) return false;
        if (proxyId != null ? !proxyId.equals(that.proxyId) : that.proxyId != null) return false;
        if (designElementIndex != null ? !designElementIndex.equals(that.designElementIndex) : that.designElementIndex != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = efName != null ? efName.hashCode() : 0;
        result = 31 * result + (efvName != null ? efvName.hashCode() : 0);
        result = 31 * result + (int) (experimentID ^ (experimentID >>> 32));
        result = 31 * result + designElementAccession.hashCode();
        result = 31 * result + (tStatistic != +0.0f ? Float.floatToIntBits(tStatistic) : 0);
        result = 31 * result + (pValAdjusted != +0.0f ? Float.floatToIntBits(pValAdjusted) : 0);
        result = 31 * result + (int) (efId ^ (efId >>> 32));
        result = 31 * result + (int) (efvId ^ (efvId >>> 32));
        result = 31 * result + (efoAccessions != null ? Arrays.hashCode(efoAccessions) : 0);
        result = 31 * result + (proxyId != null ? proxyId.hashCode() : 0);
        result = 31 * result + (designElementIndex != null ? designElementIndex : 0);

        return result;
    }
}
