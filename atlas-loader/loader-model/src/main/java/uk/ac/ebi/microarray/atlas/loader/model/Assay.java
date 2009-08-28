package uk.ac.ebi.microarray.atlas.loader.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: Andrey Date: Aug 27, 2009 Time: 10:31:25 AM
 * To change this template use File | Settings | File Templates.
 */
public class Assay {
  private String accession;
  private String experimentAccession;
  private String arrayDesignAcession;
  private List<Property> properties;
  private List<ExpressionValue> expressionValues;

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

  public String getArrayDesignAcession() {
    return arrayDesignAcession;
  }

  public void setArrayDesignAcession(String arrayDesignAcession) {
    this.arrayDesignAcession = arrayDesignAcession;
  }

  public List<Property> getProperties() {
    return properties;
  }

  public void setProperties(List<Property> properties) {
    this.properties = properties;
  }

  public List<ExpressionValue> getExpressionValues() {
    return expressionValues;
  }

  public void setExpressionValues(List<ExpressionValue> expressionValues) {
    this.expressionValues = expressionValues;
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

  public ExpressionValue addExpressionValue(String designElementAccession,
                                            float value) {
    ExpressionValue result = new ExpressionValue();
    result.setDesignElementAccession(designElementAccession);
    result.setValue(value);

    if (null == expressionValues) {
      expressionValues = new ArrayList<ExpressionValue>();
    }

    expressionValues.add(result);

    return result;
  }
}
