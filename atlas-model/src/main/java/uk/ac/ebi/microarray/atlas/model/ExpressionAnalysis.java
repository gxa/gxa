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
    private int designElementID;
    private double tStatistic;
    private double pValAdjusted;
    private int efId;
    private int efvId;

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

    public int getDesignElementID() {
        return designElementID;
    }

    public void setDesignElementID(int designElementID) {
        this.designElementID = designElementID;
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

    public int getEfId() {
        return efId;
    }

    public void setEfId(int efId) {
        this.efId = efId;
    }

    public int getEfvId() {
        return efvId;
    }

    public void setEfvId(int efvId) {
        this.efvId = efvId;
    }

    @Override
    public String toString() {
        return "ExpressionAnalysis{" +
                "efName='" + efName + '\'' +
                ", efvName='" + efvName + '\'' +
                ", experimentID=" + experimentID +
                ", designElementID=" + designElementID +
                ", tStatistic=" + tStatistic +
                ", pValAdjusted=" + pValAdjusted +
                ", efId=" + efId +
                ", efvId=" + efvId +
                '}';
    }
}
