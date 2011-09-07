package uk.ac.ebi.gxa.loader.service;

import uk.ac.ebi.gxa.dao.ExperimentDAO;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.data.AtlasDataDAO;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.UnloadExperimentCommand;
import uk.ac.ebi.microarray.atlas.model.Experiment;

/**
 * @author pashky
 */
public class AtlasExperimentUnloaderService {
    private final ExperimentDAO experimentDAO;
    private final AtlasDataDAO atlasDataDAO;

    public AtlasExperimentUnloaderService(ExperimentDAO experimentDAO, AtlasDataDAO atlasDataDAO) {
        this.experimentDAO = experimentDAO;
        this.atlasDataDAO = atlasDataDAO;
    }

    public void process(UnloadExperimentCommand cmd, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
        final String accession = cmd.getAccession();

        try {
            if (listener != null) {
                listener.setProgress("Unloading");
                listener.setAccession(accession);
            }

            final Experiment experiment = experimentDAO.getByName(accession);
            atlasDataDAO.deleteExperiment(experiment);
            experimentDAO.delete(experiment);
        } catch (RecordNotFoundException e) {
            throw new AtlasLoaderException("Can't find the experiment: " + e.getMessage(), e);
        }
    }
}
