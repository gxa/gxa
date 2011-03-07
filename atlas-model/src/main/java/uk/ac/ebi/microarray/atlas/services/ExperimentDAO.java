package uk.ac.ebi.microarray.atlas.services;

import uk.ac.ebi.microarray.atlas.model.Experiment;

/**
 * Data access layer for experiments
 */
public interface ExperimentDAO {
    public Experiment getExperimentByAccession(String accession);
}
