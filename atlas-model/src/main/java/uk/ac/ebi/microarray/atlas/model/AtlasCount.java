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

/**
 * A basic class that models the interesting atlas counts in the database. These
 * counts essentially model unique property/property value pairs, combined with
 * details about whether the expression of this factor is up (+1) or down (-1)
 * for a given gene.  The total number of genes used to derive this result are
 * also shown.
 *
 * @author Tony Burdett
 * @date 14-Oct-2009
 */
public class AtlasCount {
    private String property;
    private String propertyValue;
    private String upOrDown;
    private int geneCount;
    private long propertyId;
    private long propertyValueId;

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public String getUpOrDown() {
        return upOrDown;
    }

    public void setUpOrDown(String upOrDown) {
        this.upOrDown = upOrDown;
    }

    public int getGeneCount() {
        return geneCount;
    }

    public void setGeneCount(int geneCount) {
        this.geneCount = geneCount;
    }

    public long getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(long propertyId) {
        this.propertyId = propertyId;
    }

    public long getPropertyValueId() {
        return propertyValueId;
    }

    public void setPropertyValueId(long propertyValueId) {
        this.propertyValueId = propertyValueId;
    }

    @Override
    public String toString() {
        return "AtlasCount{" +
                "property='" + property + '\'' +
                ", propertyValue='" + propertyValue + '\'' +
                ", upOrDown='" + upOrDown + '\'' +
                ", geneCount=" + geneCount +
                ", propertyId=" + propertyId +
                ", propertyValueId=" + propertyValueId +
                '}';
    }
}
