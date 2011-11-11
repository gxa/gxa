package uk.ac.ebi.gxa.statistics;

public class ExperimentResult {
    private ExperimentInfo experiment;

    // Used to store minimum pVal when retrieving ranked lists of experiments sorted (ASC) by pValue/tStat ranks wrt to a specific ef(-efv) combination
    private PTRank pValTstatRank;

    // Attribute for which pValue and tStatRank were found e.g. when obtaining a list of experiments to display on the gene page
    private EfAttribute highestRankAttribute;

    /**
     *
     * @param experiment
     * @param highestRankAttribute
     * @param pValTstatRank
     */
    public ExperimentResult(ExperimentInfo experiment, EfAttribute highestRankAttribute, PTRank pValTstatRank) {
        this.experiment = experiment;
        this.highestRankAttribute = highestRankAttribute;
        this.pValTstatRank = pValTstatRank;
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

    public EfAttribute getHighestRankAttribute() {
        return highestRankAttribute;
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
