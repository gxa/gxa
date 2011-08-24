package uk.ac.ebi.gxa.statistics;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.io.Serializable;

/**
 * Serializable representation of an Atlas Experiment for the purpose of ConciseSet storage
 */
@Immutable
public class ExperimentInfo implements Serializable {
    private static final long serialVersionUID = 201106071035L;

    private final String accession;
    private final long experimentId;

    public ExperimentInfo(@Nonnull final String accession, final long experimentId) {
        this.accession = accession.intern();
        this.experimentId = experimentId;
    }

    public String getAccession() {
        return accession;
    }

    public long getExperimentId() {
        return experimentId;
    }

    @Override
    public String toString() {
        return "{experimentId: " + experimentId + "; accession: " + accession + "}";
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExperimentInfo that = (ExperimentInfo) o;
        return accession != null && accession.equals(that.accession) && experimentId == that.experimentId;
    }

    @Override
    public int hashCode() {
        int result = accession != null ? accession.hashCode() : 0;
        result = 31 * result + Long.valueOf(experimentId).hashCode();
        return result;
    }
}

