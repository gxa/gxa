package uk.ac.ebi.microarray.atlas.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 24-Sep-2009
 */
public class Gene {
  private String identifier;
  private String name;
  private String species;
  private List<Property> properties;
  private int geneID;
  private int designElementID;

  public int getGeneID() {
    return geneID;
  }

  public void setGeneID(int geneID) {
    this.geneID = geneID;
  }

  public int getDesignElementID() {
    return designElementID;
  }

  public void setDesignElementID(int designElementID) {
    this.designElementID = designElementID;
  }

  public String getSpecies() {
    return species;
  }

  public void setSpecies(String species) {
    this.species = species;
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<Property> getProperties() {
    return properties;
  }

  public void setProperties(List<Property> properties) {
    this.properties = properties;
  }

  /**
   * Convenience method for adding properties to a Gene
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
}
