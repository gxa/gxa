package uk.ac.ebi.gxa.loader.service;

import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.ExperimentEditorCommand;

public class ExperimentEditorService {
    protected AtlasDAO atlasDAO;

    public void process(ExperimentEditorCommand command, boolean isPrivate) throws AtlasLoaderException {
        try {
            final String accession = command.getAccession();
            atlasDAO.setPrivate(accession, isPrivate);
        } catch (Exception ex) {
            throw new AtlasLoaderException("can not release data for experiment:" + ex.getMessage());
        }
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }
}
