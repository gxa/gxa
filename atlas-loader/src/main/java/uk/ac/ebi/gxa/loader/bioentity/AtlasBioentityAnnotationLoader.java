package uk.ac.ebi.gxa.loader.bioentity;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.DefaultAtlasLoader;
import uk.ac.ebi.gxa.loader.LoadBioentityCommand;
import uk.ac.ebi.gxa.loader.service.AtlasLoaderService;
import uk.ac.ebi.gxa.loader.service.AtlasLoaderServiceListener;
import uk.ac.ebi.gxa.model.BioentityBundle;
import uk.ac.ebi.gxa.model.DesignElementBundle;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

/**
 * User: nsklyar
 * Date: Oct 21, 2010
 */
public class AtlasBioentityAnnotationLoader extends AtlasLoaderService<LoadBioentityCommand> {

    public AtlasBioentityAnnotationLoader(DefaultAtlasLoader atlasLoader) {
        super(atlasLoader);
    }

    @Override
    public void process(LoadBioentityCommand command, AtlasLoaderServiceListener listener) throws AtlasLoaderException {


        BioentityBundle bundle = parseAnnotations(command.getUrl(), listener);

        if (bundle == null) {
            throw new AtlasLoaderException("Cannot parse bioentity annotations from " + command.getUrl());
        }

        writeBioentities(bundle, listener);

        //ToDo:  create/update corresponding array desing
//        if (command.isUpdateVirtualDesign()) {
//            writeDesignElements(new DesignElementBEBundle(bundle), listener);
//        }
    }

    private void reportProgress(AtlasLoaderServiceListener listener, String report) {
        if (listener != null)
            listener.setProgress(report);
    }

    private BioentityBundle parseAnnotations(URL adURL, AtlasLoaderServiceListener listener) throws AtlasLoaderException {

        reportProgress(listener, "Start parsing bioentity annotations from  " + adURL);

        BioentityBundle bundle = null;

        CSVReader csvReader = null;
        try {

            getLog().info("Starting to parse bioentity annotations from " + adURL);

            csvReader = new CSVReader(new InputStreamReader(adURL.openStream()), '\t', '"');

            bundle = new BioentityBundle();

            bundle.setOrganism(readValue("organism", adURL, csvReader));
            bundle.setSource(readValue("source", adURL, csvReader));
            bundle.setVersion(readValue("version", adURL, csvReader));


            String[] headers = csvReader.readNext();


            Map<Integer, String> dbRefToColumn = new HashMap<Integer, String>(headers.length);

            //Start from the 2nd column because the 1st one has transcripts IDs
            for (int i = 1; i < headers.length; i++) {
                dbRefToColumn.put(i, headers[i]);
            }

            //ToDo: can be optimized (avoid iteration tow times): put values not in Map, but in array, which is used in AtlasDao
            //read reference
            String[] line;
            int count = 0;
            while ((line = csvReader.readNext()) != null) {
                String de = line[0];
                                
                if (StringUtils.isNotEmpty(de)) {
                    Map<String, List<String>> entries = new HashMap<String, List<String>>(30);
                    for (int i = 1; i < line.length; i++) {
                        String[] values = StringUtils.split(line[i], ",");
                        if (values != null) {
                            entries.put(dbRefToColumn.get(i), Arrays.asList(values));
                        }
                    }

                    bundle.addBEAnnotations(de, entries);
                    count++;
                }
                
                if (count % 5000 == 0) {
                    getLog().info("Parsed " + count + " bioentities with annotations");
                }

            }
            getLog().info("Parsed " + count + " design element with annotations");
        } catch (IOException e) {
            getLog().error("Problem when reading virtual array design file " + adURL);
        } finally {
            try {
                getLog().info("Finished reading from " + adURL + ", closing");
                if (csvReader != null) {
                    csvReader.close();
                }
            }
            catch (IOException e) {
                getLog().info("Problem when closing CSVReader for " + adURL);
            }
        }

        reportProgress(listener, "Parsing done. Starting AD loading");
        return bundle;
    }

    private String readValue(String type, URL adURL, CSVReader csvReader) throws IOException, AtlasLoaderException {
        String[] line = csvReader.readNext();
        if (!type.equalsIgnoreCase(line[0])) {
            getLog().error("Organism is not specified");
            throw new AtlasLoaderException("Organism is not specified in " + adURL + " file");
        }
        return line[1];
    }

    private void writeBioentities(BioentityBundle bundle, AtlasLoaderServiceListener listener) {

        System.out.println("bundle.getBeAnnotations().size() = " + bundle.getBeAnnotations().size());
        getAtlasDAO().writeBioentityBundle(bundle);

        reportProgress(listener, "writing done");
    }

    private void writeDesignElements(DesignElementBundle bundle, AtlasLoaderServiceListener listener) {
        getAtlasDAO().writeDesignElenements(bundle);
    }

}
