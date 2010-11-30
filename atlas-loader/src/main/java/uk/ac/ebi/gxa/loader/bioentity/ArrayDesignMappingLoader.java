package uk.ac.ebi.gxa.loader.bioentity;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.DefaultAtlasLoader;
import uk.ac.ebi.gxa.loader.LoadArrayDesignMappingCommand;
import uk.ac.ebi.gxa.loader.service.AtlasLoaderService;
import uk.ac.ebi.gxa.loader.service.AtlasLoaderServiceListener;
import uk.ac.ebi.microarray.atlas.model.DesignElementMappingBundle;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
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
        DesignElementMappingBundle mappingBundle = parseMappings(command);

        writeMapping(mappingBundle, listener);


    }

    private void writeMapping(DesignElementMappingBundle mappingBundle, AtlasLoaderServiceListener listener) {
        getAtlasDAO().writeDesignElementMappings(mappingBundle);
    }

    private DesignElementMappingBundle parseMappings(LoadArrayDesignMappingCommand command) throws AtlasLoaderException {
        URL url = command.getUrl();
        DesignElementMappingBundle mappingBundle = null;
        CSVReader csvReader = null;
        try {
            csvReader = new CSVReader(new InputStreamReader(url.openStream()), '\t', '"');
            mappingBundle = new DesignElementMappingBundle();

            mappingBundle.setAdName(readValue("Array Design Name", url, csvReader));
            mappingBundle.setAdAccession(readValue("Array Design Accession", url, csvReader));
            mappingBundle.setAdType(readValue("Array Design Type", url, csvReader));
            mappingBundle.setAdProvider(readValue("Array Design Provider", url, csvReader));
            mappingBundle.setSwName(readValue("Mapping Software Name", url, csvReader));
            mappingBundle.setSwVersion(readValue("Mapping Software Version", url, csvReader));


            String[] line;
            int count = 0;
            //Each element contains [design element, bioentity Id]
            List<Object[]> batch = new ArrayList<Object[]>(3000000);
            while ((line = csvReader.readNext()) != null) {
                String de = line[0];

                if (StringUtils.isNotBlank(de)) {
                    for (int i = 1; i < line.length; i++) {
                        String[] values = StringUtils.split(line[i], ",");
                        if (values != null) {
                            for (String value : values) {
                                if (StringUtils.isNotBlank(value)) {
                                    String[] batchValues = new String[2];
                                    batchValues[0] = de;
                                    batchValues[1] = value;
                                    batch.add(batchValues);
                                }
                            }
                        }
                    }
                    count++;
                }

                if (count % 5000 == 0) {
                    getLog().info("Parsed " + count + " design elements");
                }
            }

            mappingBundle.setBatch(batch);
            getLog().info("Parsed " + count + " design element");
        } catch (IOException e) {
            getLog().error("Problem when reading array design file " + url);
        } finally {
            try {
                getLog().info("Finished reading from " + url + ", closing");
                if (csvReader != null) {
                    csvReader.close();
                }
            }
            catch (IOException e) {
                getLog().info("Problem when closing CSVReader for " + url);
            }
        }

        return mappingBundle;
    }

    private String readValue(String type, URL adURL, CSVReader csvReader) throws IOException, AtlasLoaderException {
        String[] line = csvReader.readNext();
        if (!type.equalsIgnoreCase(line[0])) {
            getLog().error("Required field " + type + " is not specified");
            throw new AtlasLoaderException("Required field " + type + " is not specified in " + adURL + " file");
        }
        return line[1];
    }
}
