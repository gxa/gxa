package ae3.service.structuredquery;

/**
 * Experiment list's row container
 * @author pashky
 */
public class ExperimentRow implements Comparable<ExperimentRow> {
    private long experimentId;
    private String experimentAccession;
    private String experimentDescription;
    private String experimentName;
    private double pvalue;

    /**
     * Expression - up or down
     */
    public enum UpDn { UP, DOWN };
    private UpDn updn;

    /**
     * Constructor
     * @param experimentId experiment id
     * @param experimentName experimnet name
     * @param experimentAccession experiment accessment
     * @param experimentDescription experimnet description
     * @param pvalue p-value
     * @param updn up or down
     */
    public ExperimentRow(long experimentId, String experimentName, String experimentAccession, String experimentDescription, double pvalue, UpDn updn) {
        this.experimentId = experimentId;
        this.experimentAccession = experimentAccession;
        this.experimentDescription = experimentDescription;
        this.experimentName = experimentName;
        this.pvalue = pvalue;
        this.updn = updn;
    }

    /**
     * Returns p-value
     * @return p-value
     */
    public double getPvalue() {
        return pvalue;
    }

    /**
     * Returns experiment id
     * @return experiment id
     */
    public long getExperimentId() {
        return experimentId;
    }

    /**
     * Return experiment accession
     * @return experiment accession
     */
    public String getExperimentAccession() {
        return experimentAccession;
    }

    /**
     * Returns experiment description
     * @return experiment description
     */
    public String getExperimentDescription() {
        return experimentDescription;
    }

    /**
     * Returns experiment name
     * @return experiment name
     */
    public String getExperimentName() {
        return experimentName;
    }

    /**
     * Returns up or down
     * @return UP or DOWN
     */
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
                ", experimentAccession='" + experimentAccession + '\'' +
                ", experimentDescription='" + experimentDescription + '\'' +
                ", experimentName='" + experimentName + '\'' +
                ", pvalue=" + pvalue +
                '}';
    }
}
