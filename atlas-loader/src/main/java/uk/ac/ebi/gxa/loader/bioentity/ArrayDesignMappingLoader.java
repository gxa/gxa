package uk.ac.ebi.gxa.loader.bioentity;

import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.DefaultAtlasLoader;
import uk.ac.ebi.gxa.loader.LoadArrayDesignMappingCommand;
import uk.ac.ebi.gxa.loader.service.AtlasLoaderService;
import uk.ac.ebi.gxa.loader.service.AtlasLoaderServiceListener;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

/**
 * User: Nataliya Sklyar
 * Date: Nov 10, 2010
 */
public class ArrayDesignMappingLoader extends AtlasLoaderService<LoadArrayDesignMappingCommand> {

    public ArrayDesignMappingLoader(DefaultAtlasLoader atlasLoader) {
        super(atlasLoader);
    }

    @Override
    public void process(LoadArrayDesignMappingCommand command, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
//        Properties properties = new Properties();
//        try {
//            properties.load(new FileInputStream(command.getAdAccMappingFile()));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        URL url = command.getUrl();
//        String file = url.getFile();

    }
}
