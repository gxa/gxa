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
 * http://ostolop.github.com/gxa/
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
    private int experimentID;
    private transient int designElementID;  // we don't care about it
    private double tStatistic;
    private double pValAdjusted;
    private transient int efId;  // TODO: make it properly
    private transient int efvId; // TODO: make it properly
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

    public int getExperimentID() {
        return experimentID;
    }

    public void setExperimentID(int experimentID) {
        this.experimentID = experimentID;
    }

    public int getDesignElementID() {
        return designElementID;
    }

    public void setDesignElementID(int designElementID) {
        this.designElementID = designElementID;
    }

    public double getPValAdjusted() {
        return pValAdjusted;
    }

    public void setPValAdjusted(double pValAdjusted) {
        this.pValAdjusted = pValAdjusted;
    }

    public double getTStatistic() {
        return tStatistic;
    }

    public void setTStatistic(double tStatistic) {
        this.tStatistic = tStatistic;
    }

    public int getEfId() {
        return efId;
    }

    public void setEfId(int efId) {
        this.efId = efId;
    }

    public int getEfvId() {
        return efvId;
    }

    public void setEfvId(int efvId) {
        this.efvId = efvId;
    }

    public String[] getEfoAccessions() {
        return efoAccessions;
    }

    public void setEfoAccessions(String[] efoAccessions) {
        this.efoAccessions = efoAccessions;
    }

    public int compareTo(ExpressionAnalysis o) {
        return Double.valueOf(o.pValAdjusted).compareTo(pValAdjusted);
    }

    public boolean isUp() {
        return getTStatistic() > 0;
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
