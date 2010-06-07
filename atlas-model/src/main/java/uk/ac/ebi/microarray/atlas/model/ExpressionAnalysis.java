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
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 24-Sep-2009
 */
public class ExpressionAnalysis implements Serializable, Comparable<ExpressionAnalysis> {
    private String efName;
    private String efvName;
    private long experimentID;
    private transient long designElementID;  // we don't care about it
    private float tStatistic;
    private float pValAdjusted;
    private transient long efId;  // TODO: make it properly
    private transient long efvId; // TODO: make it properly
    private String[] efoAccessions;

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

    public long getDesignElementID() {
        return designElementID;
    }

    public void setDesignElementID(long designElementID) {
        this.designElementID = designElementID;
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
        return efoAccessions;
    }

    public void setEfoAccessions(String[] efoAccessions) {
        this.efoAccessions = efoAccessions;
    }

    public int compareTo(ExpressionAnalysis o) {
        return Float.valueOf(o.pValAdjusted).compareTo(pValAdjusted);
    }

    public boolean isUp() {
        return getTStatistic() > 0;
    }

    public boolean isNo() {
        return pValAdjusted > 0.05 || tStatistic == 0;
    }

    @Override
    public String toString() {
        return "ExpressionAnalysis{" +
                "efName='" + efName + '\'' +
                ", efvName='" + efvName + '\'' +
                ", experimentID=" + experimentID +
                ", designElementID=" + designElementID +
                ", tStatistic=" + tStatistic +
                ", pValAdjusted=" + pValAdjusted +
                ", efId=" + efId +
                ", efvId=" + efvId +
                '}';
    }
}
