package uk.ac.ebi.gxa.loader.service;

import org.springframework.dao.DataAccessException;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.UnloadExperimentCommand;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.microarray.atlas.model.Experiment;

/**
 * @author pashky
 */
public class AtlasExperimentUnloaderService {

    private AtlasDAO atlasDAO;
    private AtlasNetCDFDAO atlasNetCDFDAO;


    public void process(UnloadExperimentCommand cmd, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
        final String accession = cmd.getAccession();

        try {
            if(listener != null) {
                listener.setProgress("Unloading");
                listener.setAccession(accession);
            }
            Experiment experiment = getAtlasDAO().getExperimentByAccession(accession);
            if(experiment == null)
                throw new AtlasLoaderException("Can't find experiment to unload");

            getAtlasDAO().deleteExperiment(accession);
            getAtlasNetCDFDAO().removeExperimentData(accession);
        } catch(DataAccessException e) {
            throw new AtlasLoaderException("DB error while unloading experiment " + accession, e);
        }
    }

    AtlasDAO getAtlasDAO() {
        return atlasDAO;
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    AtlasNetCDFDAO getAtlasNetCDFDAO() {
        return atlasNetCDFDAO;
    }

    public void setAtlasNetCDFDAO(AtlasNetCDFDAO atlasNetCDFDAO) {
        this.atlasNetCDFDAO = atlasNetCDFDAO;
    }
}
