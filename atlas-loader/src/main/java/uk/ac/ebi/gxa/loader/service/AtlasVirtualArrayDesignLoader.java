package uk.ac.ebi.gxa.loader.service;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.gxa.loader.AtlasLoaderCommand;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.DefaultAtlasLoader;
import uk.ac.ebi.gxa.loader.LoadVirtualArrayDesignCommand;
import uk.ac.ebi.microarray.atlas.model.ArrayDesignBundle;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: nsklyar
 * Date: Oct 8, 2010
 */
public class AtlasVirtualArrayDesignLoader extends AtlasLoaderService<LoadVirtualArrayDesignCommand> {

    public AtlasVirtualArrayDesignLoader(DefaultAtlasLoader atlasLoader) {
        super(atlasLoader);
    }

    @Override
    public void process(LoadVirtualArrayDesignCommand command, AtlasLoaderServiceListener listener) throws AtlasLoaderException {

        if (listener != null)
            listener.setProgress("Start parsing array design from  " + command.getUrl());

        ArrayDesignBundle bundle = parseAnnotations(command.getUrl());
        if (bundle == null) {
            throw new AtlasLoaderException("Cannot parse virtual Array Design from " + command.getUrl());
        }

        if (listener != null)
            listener.setProgress("Parsing done. Starting AD loading");

        bundle.setType(command.getAdType());
        bundle.setGeneIdentifierNamesInPriorityOrder(command.getGeneIdentifierPriority());

        writeBundle(bundle, listener);

        if (listener != null)
            listener.setProgress("done");
    }

    private ArrayDesignBundle parseAnnotations(URL adURL) throws AtlasLoaderException {
        ArrayDesignBundle bundle = new ArrayDesignBundle();

        CSVReader csvReader = null;
        try {

            getLog().info("Starting to parse array Design from " + adURL);
            csvReader = new CSVReader(new InputStreamReader(adURL.openStream()), '\t', '"');

            bundle.setName(readValue("name", adURL, csvReader));
            bundle.setAccession(readValue("accession", adURL, csvReader));

            String[] headers = csvReader.readNext();


            Map<Integer, String> dbRefToColumn = new HashMap<Integer, String>(headers.length);

            //Start from the 2nd column because the 1st one has transcripts IDs
            for (int i = 1; i < headers.length; i++) {
                dbRefToColumn.put(i, headers[i]);
            }

            //read reference
            String[] line;
            int count = 0;
            while ((line = csvReader.readNext()) != null) {
                String de = line[0];
                Map<String, List<String>> entries = new HashMap<String, List<String>>(30);
                for (int i = 1; i < line.length; i++) {
                    String[] values = StringUtils.split(line[i], "|");
                    if (values != null) {
                        entries.put(dbRefToColumn.get(i), Arrays.asList(values));
                    }
                }
//
                bundle.addDesignElementWithEntries(de, entries);
                count++;
                if (count % 1000 == 0) {
                    getLog().info("Parsed " + count + " design element with annotations");
                }

//                if (count > 1000) break;

            }
            getLog().info("Parsed " + count + " design element with annotations");
        } catch (IOException e) {
            getLog().error("Problem when reading virtual array design file " + adURL);
        } finally {
            try {
//                log.info("Finished reading from " + file.getName() + ", closing");
                if (csvReader != null) {
                    csvReader.close();
                }
            }
            catch (IOException e) {
                getLog().info("Problem when closing CSVReader for " + adURL);
            }
        }
        return bundle;
    }

    private void writeBundle(ArrayDesignBundle bundle, AtlasLoaderServiceListener listener) {

        getAtlasDAO().writeArrayDesignBundle(bundle);
    }

    private String readValue(String type, URL adURL, CSVReader csvReader) throws IOException, AtlasLoaderException {
        String[] line = csvReader.readNext();
        if (!type.equalsIgnoreCase(line[0])) {
            getLog().error(type + " is not specified");
            throw new AtlasLoaderException(type + " is not specified in " + adURL + " file");
        }
        return line[1];
    }
}
