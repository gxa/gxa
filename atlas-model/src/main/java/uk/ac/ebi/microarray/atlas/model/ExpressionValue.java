package uk.ac.ebi.microarray.atlas.model;

/**
 * Created by IntelliJ IDEA. User: Andrey Date: Aug 27, 2009 Time: 10:30:58 AM
 * To change this template use File | Settings | File Templates.
 */
public class ExpressionValue {
  private String designElementAccession;
  private Float value;

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
