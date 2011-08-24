package uk.ac.ebi.gxa.statistics;

public class ExperimentResult {
    private ExperimentInfo experiment;

    // Used to store minimum pVal when retrieving ranked lists of experiments sorted (ASC) by pValue/tStat ranks wrt to a specific ef(-efv) combination
    private PTRank pValTstatRank;

    // Attribute for which pValue and tStatRank were found e.g. when obtaining a list of experiments to display on the gene page
    private EfvAttribute highestRankAttribute;

    public ExperimentResult(ExperimentInfo experiment) {
        this.experiment = experiment;
    }

    public String getAccession() {
        return experiment.getAccession();
    }

    public long getExperimentId() {
        return experiment.getExperimentId();
    }

    public PTRank getPValTStatRank() {
        return pValTstatRank;
    }

    public void setPValTstatRank(PTRank pValTstatRank) {
        this.pValTstatRank = pValTstatRank;
    }

    public EfvAttribute getHighestRankAttribute() {
        return highestRankAttribute;
    }

    public void setHighestRankAttribute(EfvAttribute highestRankAttribute) {
        this.highestRankAttribute = highestRankAttribute;
    }

    public ExperimentInfo getExperimentInfo() {
        return experiment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExperimentResult that = (ExperimentResult) o;
        return experiment == null ? that.experiment == null : experiment.equals(that.experiment);
    }

    @Override
    public int hashCode() {
        return experiment != null ? experiment.hashCode() : 0;
    }
}
