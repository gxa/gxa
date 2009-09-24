package uk.ac.ebi.microarray.atlas.model;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 24-Sep-2009
 */
public class ExpressionAnalytics {
  private String efName;
  private String efvName;
  private long experimentID;
  private double pValAdjusted;

  public String getEfName() {
    return efName;
  }

  public void setEfName(String efName) {
    this.efName = efName;
  }

  public String getEfvName() {
    return efvName;
  }

  public void setEfvName(String efvName) {
    this.efvName = efvName;
  }

  public long getExperimentID() {
    return experimentID;
  }

  public void setExperimentID(long experimentID) {
    this.experimentID = experimentID;
  }

  public double getPValAdjusted() {
    return pValAdjusted;
  }

  public void setPValAdjusted(double pValAdjusted) {
    this.pValAdjusted = pValAdjusted;
  }
}
