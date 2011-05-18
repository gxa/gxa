package uk.ac.ebi.gxa.loader.service;

import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.DataReleaseCommand;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;

public class AtlasDataReleaseService {

    protected AtlasDAO atlasDAO;
    protected AtlasNetCDFDAO atlasNetCDFDAO;

    public void process(DataReleaseCommand command) throws AtlasLoaderException {
        try {
            final String accession = command.getAccession();
            getAtlasNetCDFDAO().releaseExperiment(accession);
            getAtlasDAO().setExperimentReleaseDate(accession);
        } catch (Exception ex) {
            throw new AtlasLoaderException("can not release data for experiment:" + ex.getMessage());
        }
    }

    public AtlasDAO getAtlasDAO() {
        return atlasDAO;
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    public AtlasNetCDFDAO getAtlasNetCDFDAO() {
        return atlasNetCDFDAO;
    }

    public void setAtlasNetCDFDAO(AtlasNetCDFDAO atlasNetCDFDAO) {
        this.atlasNetCDFDAO = atlasNetCDFDAO;
    }
}
