package uk.ac.ebi.gxa.loader.bioentity;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.LoadBioentityCommand;
import uk.ac.ebi.gxa.loader.service.AtlasLoaderServiceListener;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * User: nsklyar
 * Date: 13/04/2011
 */
public class FileAnnotationLoader extends AtlasBioentityAnnotationLoader {
    // logging
    final private Logger log = LoggerFactory.getLogger(this.getClass());

    private String transcriptField;
    private String geneField;

    public void process(LoadBioentityCommand command, final AtlasLoaderServiceListener listener) throws AtlasLoaderException {

        setListener(listener);

        URL url = command.getUrl();
        reportProgress("Start parsing bioentity annotations from  " + url);

        CSVReader csvReader = null;
        try {

            log.info("Starting to parse bioentity annotations from " + url);

            csvReader = new CSVReader(new InputStreamReader(url.openStream()), '\t', '"');

            initFields(url, csvReader);

            String[] headers = csvReader.readNext();


            final Map<Integer, String> dbRefToColumn = new HashMap<Integer, String>(headers.length);
            int geneColumnIndex = -1;

            for (int i = 0; i < headers.length; i++) {
                dbRefToColumn.put(i, headers[i]);
                if (headers[i].equals(geneField)) {
                    geneColumnIndex = i;
                }
            }

            if (geneColumnIndex < 0)
                log.info("Gene coulumn is not present in the annotation file");

            String[] line;
            int count = 0;

            while ((line = csvReader.readNext()) != null) {
                String beIdentifier = line[0];

                if (StringUtils.isNotBlank(beIdentifier)) {

                    String geneName = null;
                    if (geneColumnIndex > -1) {
                        geneName = line[geneColumnIndex];
                    }
                    //parse properties
                    for (int i = 1; i < line.length; i++) {
                        String propertyName = dbRefToColumn.get(i);
                        String[] values = StringUtils.split(line[i], "|");

                        if (values != null) {
                            for (String value : values) {
                                addPropertyValue(beIdentifier, geneName, propertyName, value);
                            }
                        }
                        if (propertyName.equalsIgnoreCase("Organism") && StringUtils.isNotBlank(line[i])) {
                            setOrganism(line[i]);
                        }
                    }

                    //create transcript gene mapping
                    if (geneName != null) {
                        addTranscriptGeneMapping(beIdentifier, geneName);
                        addGene(getOrganism(), geneField, geneName);
                    }

                    addTransctipt(getOrganism(), transcriptField, beIdentifier);

                    count++;
                }

                if (count % 5000 == 0) {
                    log.info("Parsed " + count + " bioentities with annotations");
                }
            }

            log.info("Parsed " + count + " bioentities with annotations");

            writeProperties(new HashSet<String>(dbRefToColumn.values()));
            writeBioentitiesAndAnnotations(transcriptField, geneField);

        } catch (IOException e) {
            log.error("Problem when reading bioentity annotations file " + url);
        } finally {
            log.info("Finished reading from " + url + ", closing");
            closeQuietly(csvReader);
        }

    }

    private void initFields(URL url, CSVReader csvReader) throws IOException, AtlasLoaderException {
        setOrganism(readValue("organism", url, csvReader));
        setSource(readValue("source", url, csvReader));
        setVersion(readValue("version", url, csvReader));

        transcriptField = readValue("bioentity", url, csvReader);
        geneField = readValue("gene", url, csvReader);
    }

    private String readValue(String type, URL adURL, CSVReader csvReader) throws IOException, AtlasLoaderException {
        String[] line = csvReader.readNext();
        if (!type.equalsIgnoreCase(line[0])) {
            log.error(type + " is not specified");
            throw new AtlasLoaderException(type + " is not specified in " + adURL + " file");
        }
        return line[1];
    }
}
