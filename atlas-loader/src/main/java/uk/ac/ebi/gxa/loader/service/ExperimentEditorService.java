package uk.ac.ebi.gxa.loader.service;

import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.dao.ExperimentDAO;
import uk.ac.ebi.gxa.dao.hibernate.DAOException;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.ExperimentEditorCommand;
import uk.ac.ebi.microarray.atlas.model.Experiment;

public class ExperimentEditorService {
    private final ExperimentDAO experimentDAO;
    private AtlasDAO atlasDAO;

    public ExperimentEditorService(ExperimentDAO experimentDAO, AtlasDAO atlasDAO) {
        this.experimentDAO = experimentDAO;
        this.atlasDAO = atlasDAO;
    }

    public void process(ExperimentEditorCommand command, boolean isPrivate) throws AtlasLoaderException {
        atlasDAO.startSession();
        try {
            final Experiment experiment = experimentDAO.getByName(command.getAccession());
            experiment.setPrivate(isPrivate);
            experimentDAO.save(experiment);
        } catch (DAOException e) {
            throw new AtlasLoaderException(e.getMessage(), e);
        } catch (Exception ex) {
            throw new AtlasLoaderException("can not release data for experiment:" + ex.getMessage());
        } finally {
            atlasDAO.finishSession();
        }
    }
}
