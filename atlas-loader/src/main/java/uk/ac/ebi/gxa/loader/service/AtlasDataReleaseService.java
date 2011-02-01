package uk.ac.ebi.gxa.loader.service;

import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.DataReleaseCommand;
import uk.ac.ebi.gxa.loader.DefaultAtlasLoader;

public class AtlasDataReleaseService extends AtlasLoaderService {
    public AtlasDataReleaseService(DefaultAtlasLoader loader) {
        super(loader);
    }

    public void process(DataReleaseCommand command) throws AtlasLoaderException {
        try {
            final String accession = command.getAccession();
            getNetCDFDAO().releaseExperiment(accession);
            getAtlasDAO().setExperimentReleaseDate(accession);
        } catch (Exception ex) {
            throw new AtlasLoaderException("can not release data for experiment:" + ex.getMessage());
        }
    }
}
