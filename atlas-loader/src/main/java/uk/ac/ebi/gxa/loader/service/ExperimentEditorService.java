package uk.ac.ebi.gxa.loader.service;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.dao.ExperimentDAO;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.ExperimentEditorCommand;
import uk.ac.ebi.microarray.atlas.model.Experiment;

public class ExperimentEditorService {
    private final ExperimentDAO experimentDAO;

    public ExperimentEditorService(ExperimentDAO experimentDAO) {
        this.experimentDAO = experimentDAO;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(ExperimentEditorCommand command, boolean isPrivate) throws AtlasLoaderException {
        try {
            final Experiment experiment = experimentDAO.getByName(command.getAccession());
            experiment.setPrivate(isPrivate);
            experimentDAO.save(experiment);
        } catch (RecordNotFoundException e) {
            throw new AtlasLoaderException(e.getMessage(), e);
        } catch (Exception ex) {
            throw new AtlasLoaderException("can not release data for experiment:" + ex.getMessage());
        }
    }
}
