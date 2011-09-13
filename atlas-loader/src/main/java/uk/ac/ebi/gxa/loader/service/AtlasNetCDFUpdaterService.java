package uk.ac.ebi.gxa.loader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.data.*;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.UpdateNetCDFForExperimentCommand;
import uk.ac.ebi.microarray.atlas.model.Experiment;

/**
 * NetCDF updater service which preserves expression values information, but updates all properties
 *
 * @author pashky
 */
public class AtlasNetCDFUpdaterService {
    private static final Logger log = LoggerFactory.getLogger(AtlasNetCDFUpdaterService.class);
    private AtlasDAO atlasDAO;
    private AtlasDataDAO atlasDataDAO;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(UpdateNetCDFForExperimentCommand cmd, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
        final Experiment experiment;
        try {
            experiment = atlasDAO.getExperimentByAccession(cmd.getAccession());
        } catch (RecordNotFoundException e) {
            throw new AtlasLoaderException(e.getMessage(), e);
        }
        final ExperimentWithData ewd = atlasDataDAO.createExperimentWithData(experiment);

        try {
            listener.setAccession(experiment.getAccession());
            ewd.updateAllData();
            listener.setProgress("Successfully updated the NetCDFs");
        } catch (AtlasDataException e) {
            listener.setProgress("Failed NetCDF update");
            throw new AtlasLoaderException(e);
        } finally {
            ewd.closeAllDataSources();
        }
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    public void setAtlasDataDAO(AtlasDataDAO atlasDataDAO) {
        this.atlasDataDAO = atlasDataDAO;
    }
}
