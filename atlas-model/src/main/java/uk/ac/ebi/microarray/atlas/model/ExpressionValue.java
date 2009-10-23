package uk.ac.ebi.microarray.atlas.model;

/**
 * Created by IntelliJ IDEA. User: Andrey Date: Aug 27, 2009 Time: 10:30:58 AM
 * To change this template use File | Settings | File Templates.
 */
public class ExpressionValue {
  private int designElementID;
  private int assayID;
  private String designElementAccession;
  private Float value;

  public int getDesignElementID() {
    return designElementID;
  }

  public void setDesignElementID(int designElementID) {
    this.designElementID = designElementID;
  }

  public int getAssayID() {
    return assayID;
  }

  public void setAssayID(int assayID) {
    this.assayID = assayID;
  }

  public String getDesignElementAccession() {
    return designElementAccession;
  }

  public void setDesignElementAccession(String designElementAccession) {
    this.designElementAccession = designElementAccession;
  }

  public Float getValue() {
    return value;
  }

  public void setValue(Float value) {
    this.value = value;
  }

  public String toString() {
    return "ExpressionValue{" +
        "designElementAccession='" + designElementAccession + '\'' +
        ", value=" + value +
        '}';
  }
}
