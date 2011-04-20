package uk.ac.ebi.gxa.loader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import uk.ac.ebi.gxa.analytics.compute.AtlasComputeService;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.DefaultAtlasLoader;
import uk.ac.ebi.gxa.loader.UnloadExperimentCommand;
import uk.ac.ebi.gxa.Experiment;
import uk.ac.ebi.gxa.Model;

/**
 * @author pashky
 */
public class AtlasExperimentUnloaderService {
    protected Model atlasModel;

    public void process(UnloadExperimentCommand cmd, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
        final String accession = cmd.getAccession();

        try {
            if(listener != null) {
                listener.setProgress("Unloading");
                listener.setAccession(accession);
            }
            // TODO: why to get experiment if we just want to remove them?
            final Experiment experiment = atlasModel.getExperimentByAccession(accession);
            if (experiment == null)
                throw new AtlasLoaderException("Can't find experiment to unload");

            experiment.deleteFromStorage();
        } catch(DataAccessException e) {
            throw new AtlasLoaderException("DB error while unloading experiment " + accession, e);
        }
    }

    public void setAtlasModel(Model atlasModel) {
        this.atlasModel = atlasModel;
    }
}
