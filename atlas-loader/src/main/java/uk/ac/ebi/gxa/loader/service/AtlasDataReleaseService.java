package uk.ac.ebi.gxa.loader.service;

import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.DataReleaseCommand;
import uk.ac.ebi.gxa.loader.DefaultAtlasLoader;
import uk.ac.ebi.gxa.loader.utils.ZipUtil;

import java.io.File;

public class AtlasDataReleaseService extends AtlasLoaderService {
    public AtlasDataReleaseService(DefaultAtlasLoader loader) {
        super(loader);
    }

    public void process(DataReleaseCommand command) throws AtlasLoaderException {
        try {
            String accession = command.getAccession();
            File directory = getAtlasNetcdfDAO().getDataDirectory(accession);

            File exportFolder = new File(getAtlasNetcdfDAO().getAtlasDataRepo(), "export");
            if (!exportFolder.exists() && !exportFolder.mkdirs()) {
                throw new AtlasLoaderException("can not create export folder " + exportFolder);
            }

            ZipUtil.compress(directory, new File(exportFolder, accession + ".zip"));

            getAtlasDAO().setExperimentReleaseDate(accession);
        } catch (Exception ex) {
            throw new AtlasLoaderException("can not release data for experiment:" + ex.getMessage());
        }
    }
}
