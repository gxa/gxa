package uk.ac.ebi.gxa.loader.service;

import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.DataReleaseCommand;
import uk.ac.ebi.gxa.loader.DefaultAtlasLoader;
import uk.ac.ebi.gxa.loader.utils.ZipUtil;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;

import java.io.File;

public class AtlasDataReleaseService extends AtlasLoaderService {
    AtlasNetCDFDAO atlasNetCDFDAO;
    File dataRepo;
    AtlasDAO atlasDAO;

    public AtlasDataReleaseService(DefaultAtlasLoader loader) {
        super(loader);
    }

    public void setAtlasNetCDFDAO(AtlasNetCDFDAO atlasNetCDFDAO) {
        this.atlasNetCDFDAO = atlasNetCDFDAO;
    }

    public void setDataRepo(File dataRepo) {
        this.dataRepo = dataRepo;
    }

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    public void process(DataReleaseCommand command) throws AtlasLoaderException {
        try {
            String accession = command.getAccession();
            File directory = atlasNetCDFDAO.getDataDirectory(accession);

            File exportFolder = new File(dataRepo, "export");
            if (!exportFolder.exists() && !exportFolder.mkdirs()) {
                throw new AtlasLoaderException("can not create export folder " + exportFolder);
            }

            ZipUtil.compress(directory.getPath(), (new File(exportFolder, accession + ".zip")).getPath());

            atlasDAO.setExperimentReleaseDate(accession);
        } catch (Exception ex) {
            throw new AtlasLoaderException("can not release data for experiment:" + ex.getMessage());
        }
    }
}
