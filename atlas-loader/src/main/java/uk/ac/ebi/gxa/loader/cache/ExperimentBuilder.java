package uk.ac.ebi.gxa.loader.cache;

import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.util.Collection;

/**
 * Methods necessary to build an Experiment metadata
 */
public interface ExperimentBuilder {
    void linkAssayToSample(Assay assay, String sampleAccession) throws AtlasLoaderException;

    Sample fetchOrCreateSample(String accession);

    void setExperiment(Experiment experiment);

    Experiment fetchExperiment();

    void addAssay(Assay assay);

    Assay fetchAssay(String accession);

    Collection<Assay> fetchAllAssays();

    void addSample(Sample sample);

    Sample fetchSample(String accession);

    Collection<Sample> fetchAllSamples();
}
