package uk.ac.ebi.gxa.loader.service;

import org.springframework.dao.DataAccessException;
import uk.ac.ebi.gxa.dao.ExperimentDAO;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.UnloadExperimentCommand;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.microarray.atlas.model.Experiment;

/**
 * @author pashky
 */
public class AtlasExperimentUnloaderService {
    private final ExperimentDAO experimentDAO;
    private final AtlasNetCDFDAO netCDFDAO;

    public AtlasExperimentUnloaderService(ExperimentDAO experimentDAO, AtlasNetCDFDAO netCDFDAO) {
        this.experimentDAO = experimentDAO;
        this.netCDFDAO = netCDFDAO;
    }

    public void process(UnloadExperimentCommand cmd, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
        final String accession = cmd.getAccession();

        try {
            if (listener != null) {
                listener.setProgress("Unloading");
                listener.setAccession(accession);
            }

            final Experiment experiment = experimentDAO.getExperimentByAccession(accession);
            if (experiment == null)
                throw new AtlasLoaderException("Can't find experiment to unload");
            netCDFDAO.deleteExperiment(experiment);
            experimentDAO.delete(experiment);
        } catch (DataAccessException e) {
            throw new AtlasLoaderException("DB error while unloading experiment " + accession, e);
        }
    }
}
