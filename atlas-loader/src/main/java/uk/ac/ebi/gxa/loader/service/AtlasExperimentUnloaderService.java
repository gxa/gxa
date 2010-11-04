package uk.ac.ebi.gxa.loader.service;

import org.springframework.dao.DataAccessException;
import uk.ac.ebi.gxa.loader.DefaultAtlasLoader;
import uk.ac.ebi.gxa.loader.UnloadExperimentCommand;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.gxa.utils.FileUtil;

import java.io.File;
import java.util.List;

/**
 * @author pashky
 */
public class AtlasExperimentUnloaderService extends AtlasLoaderService<UnloadExperimentCommand> {
    public AtlasExperimentUnloaderService(DefaultAtlasLoader atlasLoader) {
        super(atlasLoader);
    }

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

            List<ArrayDesign> arrayDesigns = getAtlasDAO().getArrayDesignByExperimentAccession(accession);

            getAtlasDAO().deleteExperiment(accession);
            FileUtil.deleteDirectory(getAtlasNetCDFDirectory(accession));
        } catch(DataAccessException e) {
            throw new AtlasLoaderException("DB error while unloading experiment " + accession, e);
        }
    }
}
