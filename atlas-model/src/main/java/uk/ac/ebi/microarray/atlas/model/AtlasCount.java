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

  public String toString() {
    return "AtlasCount{" +
        "property='" + property + '\'' +
        ", propertyValue='" + propertyValue + '\'' +
        ", upOrDown='" + upOrDown + '\'' +
        ", geneCount=" + geneCount +
        '}';
  }
}
