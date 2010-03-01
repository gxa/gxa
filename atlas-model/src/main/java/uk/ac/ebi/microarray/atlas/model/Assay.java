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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA. User: Andrey Date: Aug 27, 2009 Time: 10:31:25 AM To change this template use File |
 * Settings | File Templates.
 */
public class Assay {
    private String accession;
    private String experimentAccession;
    private String arrayDesignAccession;
    private List<Property> properties;
    //  private List<ExpressionValue> expressionValues;
    private int assayID;

    // maps design elements to expression values as primitives
    private Map<Integer, Float> expressionValues;
    // maps design elements to expression values using string accessions instead of int IDs
    private Map<String, Float> expressionValuesAcc;

    public String getAccession() {
        return accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public String getExperimentAccession() {
        return experimentAccession;
    }

    public void setExperimentAccession(String experimentAccession) {
        this.experimentAccession = experimentAccession;
    }

    public String getArrayDesignAccession() {
        return arrayDesignAccession;
    }

    public void setArrayDesignAccession(String arrayDesignAccession) {
        this.arrayDesignAccession = arrayDesignAccession;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    public Map<Integer, Float> getExpressionValues() {
        return this.expressionValues;
    }

    public void setExpressionValues(Map<Integer, Float> expressionValues) {
        this.expressionValues = expressionValues;
    }

    /**
     * Returns a map of expression values indexed by the design element reference supplied.  This returns an equivalent
     * set of results to {@link #getExpressionValues()}, but is included for convenience as the loader only has access
     * to design element references in the data file, whereas the database can access both.  You should check both
     * methods before concluding that this Assay has no expression values linked.
     * <p/>
     *
     * @return a map of expression values, indexed by design element reference.  This reference may be the name or the
     *         accession of the design element.
     */
    public Map<String, Float> getExpressionValuesByDesignElementReference() {
        return this.expressionValuesAcc;
    }

    /**
     * Sets the expression values for this assay, indexed by the design element references supplied in this map.  This
     * is really an alternative to {@link #setExpressionValues(java.util.Map)}, but is included for convenience as the
     * loader only has access to design element references in the data file, whereas once data has been stored in the
     * database it has an ID assigned.
     * <p/>
     *
     * @param expressionValues a map of expression values, indexed by design element reference.  This reference may be
     *                         the name or the accession of the design element.
     */
    public void setExpressionValuesByDesignElementReference(Map<String, Float> expressionValues) {
        this.expressionValuesAcc = expressionValues;
    }

    public float[] getAllExpressionValues() {
        float[] result = new float[expressionValues.keySet().size()];
        int i = 0;
        for (Float f : expressionValues.values()) {
            result[i] = f;
            i++;
        }
        return result;
    }

    public float getExpressionValueByDesignElement(int designElementID) {
        return expressionValues.get(designElementID);
    }

    public int getAssayID() {
        return assayID;
    }

    public void setAssayID(int assayID) {
        this.assayID = assayID;
    }

    /**
     * Convenience method for adding properties to an Assay
     *
     * @param accession     the property accession to set
     * @param name          the property name
     * @param value         the property value
     * @param isFactorValue whether this property is a factor value or not
     * @return the resulting property
     */
    public Property addProperty(String accession, String name, String value,
                                boolean isFactorValue) {
        Property result = new Property();
        result.setAccession(accession);
        result.setName(name);
        result.setValue(value);
        result.setFactorValue(isFactorValue);

        if (null == properties) {
            properties = new ArrayList<Property>();
        }

        properties.add(result);

        return result;
    }

    public boolean addProperty(Property p) {
        if (null == properties) {
            properties = new ArrayList<Property>();
        }

        return properties.add(p);
    }

    public void addExpressionValue(int designElementID,
                                   float value) {
//    ExpressionValue result = new ExpressionValue();
//    result.setDesignElementAccession(designElementAccession);
//    result.setValue(value);
//
//    if (null == expressionValues) {
//      expressionValues = new ArrayList<ExpressionValue>();
//    }
//
//    expressionValues.add(result);
//
//    return result;

        if (expressionValues == null) {
            expressionValues = new HashMap<Integer, Float>();
        }

        expressionValues.put(designElementID, value);
    }

    public void addExpressionValueByAccession(String designElementAccession,
                                              float value) {
        ExpressionValue result = new ExpressionValue();
        result.setDesignElementAccession(designElementAccession);
        result.setValue(value);

        if (expressionValuesAcc == null) {
            expressionValuesAcc = new HashMap<String, Float>();
        }

        expressionValuesAcc.put(designElementAccession, value);
    }

    @Override
    public String toString() {
        return "Assay{" +
                "accession='" + accession + '\'' +
                ", experimentAccession='" + experimentAccession + '\'' +
                ", arrayDesignAcession='" + arrayDesignAccession + '\'' +
                '}';
    }
}
