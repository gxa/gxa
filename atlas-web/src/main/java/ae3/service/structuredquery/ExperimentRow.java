package ae3.service.structuredquery;

import uk.ac.ebi.ae3.indexbuilder.Expression;

/**
 * Experiment list's row container
 * @author pashky
 */
public class ExperimentRow implements Comparable<ExperimentRow> {
    private long experimentId;
    private String experimentAccession;
    private String experimentDescription;
    private String experimentName;
    private String ef;
    private String efv;
    private double pvalue;

    /**
     * Expression - up or down
     */
    private Expression updn;

    /**
     * Constructor
     * @param experimentId experiment id
     * @param experimentName experimnet name
     * @param experimentAccession experiment accessment
     * @param experimentDescription experimnet description
     * @param pvalue p-value
     * @param updn up or down
     */
    public ExperimentRow(long experimentId, String experimentName, String experimentAccession, String experimentDescription,
                         double pvalue, Expression updn, String ef, String efv) {
        this.experimentId = experimentId;
        this.experimentAccession = experimentAccession;
        this.experimentDescription = experimentDescription;
        this.experimentName = experimentName;
        this.pvalue = pvalue;
        this.updn = updn;
        this.ef = ef;
        this.efv = efv;
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
    public Expression getUpdn() {
        return updn;
    }

    public String getEf() {
        return ef;
    }

    public String getEfv() {
        return efv;
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
