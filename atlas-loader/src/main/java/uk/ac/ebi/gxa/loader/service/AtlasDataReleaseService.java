package uk.ac.ebi.gxa.loader.service;

import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.DataReleaseCommand;
import uk.ac.ebi.gxa.loader.DefaultAtlasLoader;
import uk.ac.ebi.gxa.loader.utils.ZipUtil;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Nov 22, 2010
 * Time: 6:46:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class AtlasDataReleaseService extends AtlasLoaderService<DataReleaseCommand> {
    AtlasNetCDFDAO atlasNetCDFDAO;
    static File dataRepo;
    static AtlasDAO atlasDAO;
    
    public void setAtlasDataRepo(File dataRepo){
        AtlasDataReleaseService.dataRepo = dataRepo;
    }
    public AtlasDataReleaseService(){
        //Spring happy
         super(null);
    }
    public AtlasDataReleaseService(DefaultAtlasLoader loader) {
        super(loader);
    }
    public void setAtlasDAO(AtlasDAO atlasDAO){
        AtlasDataReleaseService.atlasDAO = atlasDAO;
    }

    public void process(DataReleaseCommand command, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
        try{
            atlasNetCDFDAO = new AtlasNetCDFDAO();
            atlasNetCDFDAO.setAtlasDataRepo(dataRepo);
            String accession = command.getAccession();
            File directory = atlasNetCDFDAO.getDataDirectory(accession);

            File exportFolder = new File(dataRepo, "export");
            if(!exportFolder.exists())
                exportFolder.mkdirs();

            ZipUtil.compress(directory.getPath(), (new File(exportFolder, accession+".zip")).getPath());

            atlasDAO.setExperimentReleaseDate(accession);
        }                                                                                                             
        catch(Exception ex){
            throw new AtlasLoaderException("can not release data for experiment:"+ex.getMessage());
        }
    }
}
