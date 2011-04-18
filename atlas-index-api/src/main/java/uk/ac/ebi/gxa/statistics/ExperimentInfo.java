package uk.ac.ebi.gxa.statistics;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: 4/18/11
 * Time: 2:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExperimentInfo implements Serializable {

    private static final long serialVersionUID = 7789968215270452137L;

    private String accession;
    private long experimentId;

    // Used to store minimum pVal when retrieving ranked lists of experiments sorted (ASC) by pValue/tStat ranks wrt to a specific ef(-efv) combination
    PvalTstatRank pValTstatRank;

    // Attribute for which pValue and tStatRank were found e.g. when obtaining a list of experiments to display on the gene page
    private transient EfvAttribute highestRankAttribute;


    public ExperimentInfo(final String accession, final Long experimentId) {
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

    public EfvAttribute getHighestRankAttribute() {
        return highestRankAttribute;
    }

    public void setHighestRankAttribute(EfvAttribute highestRankAttribute) {
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

        ExperimentInfo that = (ExperimentInfo) o;

        if (accession == null || !accession.equals(that.accession) || experimentId != that.experimentId) {
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

