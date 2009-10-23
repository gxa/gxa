package uk.ac.ebi.microarray.atlas.model;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 28-Sep-2009
 */
public class ArrayDesign {
  private String accession;
  private String type;
  private String name;
  private String provider;
  private int arrayDesignID;

  public String getAccession() {
    return accession;
  }

  public void setAccession(String accession) {
    this.accession = accession;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public int getArrayDesignID() {
    return arrayDesignID;
  }

  public void setArrayDesignID(int arrayDesignID) {
    this.arrayDesignID = arrayDesignID;
  }
}
