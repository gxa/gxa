package uk.ac.ebi.microarray.atlas.model;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 25-Sep-2009
 */
public class OntologyMapping {
  private String experimentAccession;
  private String property;
  private String propertyValue;
  private String ontologyTerm;
  private boolean isSampleProperty;
  private boolean isAssayProperty;
  private boolean isFactorValue;

  public String getExperimentAccession() {
    return experimentAccession;
  }

  public void setExperimentAccession(String experimentAccession) {
    this.experimentAccession = experimentAccession;
  }

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

  public String getOntologyTerm() {
    return ontologyTerm;
  }

  public void setOntologyTerm(String ontologyTerm) {
    this.ontologyTerm = ontologyTerm;
  }

  public boolean isSampleProperty() {
    return isSampleProperty;
  }

  public void setSampleProperty(boolean sampleProperty) {
    isSampleProperty = sampleProperty;
  }

  public boolean isAssayProperty() {
    return isAssayProperty;
  }

  public void setAssayProperty(boolean assayProperty) {
    isAssayProperty = assayProperty;
  }

  public boolean isFactorValue() {
    return isFactorValue;
  }

  public void setFactorValue(boolean factorValue) {
    isFactorValue = factorValue;
  }
}
