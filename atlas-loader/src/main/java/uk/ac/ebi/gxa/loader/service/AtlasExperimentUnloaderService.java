package uk.ac.ebi.gxa.loader.service;

import org.springframework.dao.DataAccessException;
import uk.ac.ebi.gxa.loader.DefaultAtlasLoader;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.io.File;
import java.util.List;

/**
 * @author pashky
 */
public class AtlasExperimentUnloaderService extends AtlasLoaderService<String> {
    public AtlasExperimentUnloaderService(DefaultAtlasLoader atlasLoader) {
        super(atlasLoader);
    }

    public void process(String accession, AtlasLoaderServiceListener listener) throws AtlasLoaderServiceException {
        try {
            if(listener != null) {
                listener.setProgress("Unloading");
                listener.setAccession(accession);
            }
            Experiment experiment = getAtlasDAO().getExperimentByAccession(accession);
            if(experiment == null)
                throw new AtlasLoaderServiceException("Can't find experiment to unload");

            List<ArrayDesign> arrayDesigns = getAtlasDAO().getArrayDesignByExperimentAccession(accession);

            getAtlasDAO().deleteExperiment(accession);

            for(ArrayDesign ad : arrayDesigns) {
                File netCdf = new File(getAtlasNetCDFRepo(), experiment.getExperimentID() + "_" + ad.getArrayDesignID() + ".nc");
                if(netCdf.exists()) {
                    if(!netCdf.delete())
                        getLog().warn("Can't delete NetCDF: " + netCdf);
                }
            }
        } catch(DataAccessException e) {
            throw new AtlasLoaderServiceException("DB error while unloading experiment " + accession, e);
        }
    }
}
