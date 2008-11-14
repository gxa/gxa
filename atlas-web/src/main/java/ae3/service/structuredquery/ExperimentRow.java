package ae3.service.structuredquery;

/**
 * @author pashky
 */
public class ExperimentRow implements Comparable<ExperimentRow> {
    private long experimentId;
    private String experimentAccessment;
    private String experimentDescription;
    private String experimentName;
    private double pvalue;
    public enum UpDn { UP, DOWN };
    private UpDn updn;

    public ExperimentRow(long experimentId, String experimentName, String experimentAccessment, String experimentDescription, double pvalue, UpDn updn) {
        this.experimentId = experimentId;
        this.experimentAccessment = experimentAccessment;
        this.experimentDescription = experimentDescription;
        this.experimentName = experimentName;
        this.pvalue = pvalue;
        this.updn = updn;
    }

    public double getPvalue() {
        return pvalue;
    }

    public long getExperimentId() {
        return experimentId;
    }

    public String getExperimentAccessment() {
        return experimentAccessment;
    }

    public String getExperimentDescription() {
        return experimentDescription;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public UpDn getUpdn() {
        return updn;
    }

    public int compareTo(ExperimentRow o) {
        return Double.valueOf(getPvalue()).compareTo(o.getPvalue());
    }

    @Override
    public String toString() {
        return "ExperimentRow{" +
                "experimentId=" + experimentId +
                ", experimentAccessment='" + experimentAccessment + '\'' +
                ", experimentDescription='" + experimentDescription + '\'' +
                ", experimentName='" + experimentName + '\'' +
                ", pvalue=" + pvalue +
                '}';
    }
}
