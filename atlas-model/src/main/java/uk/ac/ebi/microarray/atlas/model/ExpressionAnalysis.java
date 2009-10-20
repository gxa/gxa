package uk.ac.ebi.microarray.atlas.model;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 24-Sep-2009
 */
public class ExpressionAnalysis {
  private String efName;
  private String efvName;
  private int experimentID;
  private int geneID;
  private double tStatistic;
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

  public int getExperimentID() {
    return experimentID;
  }

  public void setExperimentID(int experimentID) {
    this.experimentID = experimentID;
  }

  public int getGeneID() {
    return geneID;
  }

  public void setGeneID(int geneID) {
    this.geneID = geneID;
  }

  public double getPValAdjusted() {
    return pValAdjusted;
  }

  public void setPValAdjusted(double pValAdjusted) {
    this.pValAdjusted = pValAdjusted;
  }

  public double getTStatistic() {
    return tStatistic;
  }

  public void setTStatistic(double tStatistic) {
    this.tStatistic = tStatistic;
  }

  @Override public String toString() {
    return "ExpressionAnalytics{" +
        "efName='" + efName + '\'' +
        ", efvName='" + efvName + '\'' +
        ", experimentID=" + experimentID +
        ", tStatistic=" + tStatistic +
        ", pValAdjusted=" + pValAdjusted +
        '}';
  }
}
