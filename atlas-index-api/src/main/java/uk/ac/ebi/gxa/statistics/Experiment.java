package uk.ac.ebi.gxa.statistics;

import java.io.Serializable;

/**
 * Serializable representation of an Atlas Experiment for the purpose of ConciseSet storage
 */
public class Experiment implements Serializable {

    private static final long serialVersionUID = 5513628423830801336L;

    private String accession;
    private long experimentId;

    // Used to store minimum pVal when retrieving ranked lists of experiments sorted (ASC) by pValue/tStat ranks wrt to a specific ef(-efv) combination
    PvalTstatRank pValTstatRank;

    // Attribute for which pValue and tStatRank were found e.g. when obtaining a list of experiments to display on the gene page
    private transient Attribute highestRankAttribute;


    public Experiment(final String accession, final Long experimentId) {
        this.accession = accession.intern();
        this.experimentId = experimentId;
    }

    public String getAccession() {
        return accession;
    }

    public void setAccession(final String accession) {
        this.accession = accession.intern();
    }

    public long getExperimentId() {
        return experimentId;
    }

    public PvalTstatRank getpValTStatRank() {
        return pValTstatRank;
    }

    public void setPvalTstatRank(PvalTstatRank pValTstatRank) {
        this.pValTstatRank = pValTstatRank;
    }

    public Attribute getHighestRankAttribute() {
        return highestRankAttribute;
    }

    public void setHighestRankAttribute(Attribute highestRankAttribute) {
         this.highestRankAttribute = highestRankAttribute;
    }

    @Override
    public String toString() {
        return "experimentId: " + experimentId + "; accession: " + accession +  "; highestRankAttribute: " + highestRankAttribute;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Experiment that = (Experiment) o;

        if (accession == null || !accession.equals(that.accession) || experimentId != experimentId) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = accession != null ? accession.hashCode() : 0;
        result = 31 * result + Long.valueOf(experimentId).hashCode();
        return result;
    }
}

