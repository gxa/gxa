package uk.ac.ebi.gxa.loader.service;

import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.dao.ExperimentDAO;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.ExperimentEditorCommand;
import uk.ac.ebi.microarray.atlas.model.Experiment;

public class ExperimentEditorService {
    private ExperimentDAO experimentDAO;

    public void setExperimentDAO(ExperimentDAO experimentDAO) {
        this.experimentDAO = experimentDAO;
    }

    @Transactional
    public void process(ExperimentEditorCommand command, boolean isPrivate) throws AtlasLoaderException {
        try {
            final Experiment experiment = experimentDAO.getExperimentByAccession(command.getAccession());
            experiment.setPrivate(isPrivate);
            experimentDAO.save(experiment);
        } catch (Exception ex) {
            throw new AtlasLoaderException("can not release data for experiment:" + ex.getMessage());
        }
    }
}
