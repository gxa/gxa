package ae3.service;

import ae3.model.AtlasExperiment;

/**
 * @author pashky
 */
public class AtlasExperimentRow {
    private long experimentId;
    private String experimentAccessment;
    private String experimentDescription;
    private String experimentName;
    private String ef;
    private String efv;
    private double pvalue;

    public AtlasExperimentRow(long experimentId, String experimentName, String experimentAccessment, String experimentDescription, double pvalue) {
        this.experimentId = experimentId;
        this.experimentAccessment = experimentAccessment;
        this.experimentDescription = experimentDescription;
        this.experimentName = experimentName;
        this.pvalue = pvalue;
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
}
