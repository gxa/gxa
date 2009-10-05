package uk.ac.ebi.microarray.atlas.model;

/**
 * Created by IntelliJ IDEA. User: Andrey Date: Aug 27, 2009 Time: 10:29:44 AM
 * To change this template use File | Settings | File Templates.
 */

public class Property {
  private String accession;
  private String name;
  private String value;
  private boolean isFactorValue;

  public String getAccession() {
    return accession;
  }

  public void setAccession(String accession) {
    this.accession = accession;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public boolean isFactorValue() {
    return isFactorValue;
  }

  public void setFactorValue(boolean factorValue) {
    isFactorValue = factorValue;
  }

  @Override
  public String toString() {
    return "Property{" +
        "accession='" + accession + '\'' +
        ", name='" + name + '\'' +
        ", value='" + value + '\'' +
        ", isFactorValue=" + isFactorValue +
        '}';
  }
}
