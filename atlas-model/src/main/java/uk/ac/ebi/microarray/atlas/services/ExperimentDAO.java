package uk.ac.ebi.microarray.atlas.services;

import uk.ac.ebi.gxa.Experiment;

/**
 * Data access layer for experiments
 */
public interface ExperimentDAO {
    public Experiment getExperimentByAccession(String accession);
}
