package uk.ac.ebi.gxa.loader.bioentity;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.DefaultAtlasLoader;
import uk.ac.ebi.gxa.loader.LoadBioentityCommand;
import uk.ac.ebi.gxa.loader.service.AtlasLoaderService;
import uk.ac.ebi.gxa.loader.service.AtlasLoaderServiceListener;
import uk.ac.ebi.microarray.atlas.model.BioentityBundle;
import uk.ac.ebi.microarray.atlas.model.DesignElementMappingBundle;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * User: nsklyar
 * Date: Oct 21, 2010
 */
public class AtlasBioentityAnnotationLoader extends AtlasLoaderService {

    public AtlasBioentityAnnotationLoader(DefaultAtlasLoader atlasLoader) {
        super(atlasLoader);
    }

    public void process(LoadBioentityCommand command, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
        BioentityBundle bundle = parseAnnotations(command.getUrl(), listener);
        if (bundle == null) {
            throw new AtlasLoaderException("Cannot parse bioentity annotations from " + command.getUrl());
        }

        writeBioentities(bundle, listener);
//        if (command.isUpdateVirtualDesign()) {
//            reportProgress(listener, "writing virtual array design");
//            String adName = createADName(bundle.getOrganism(), bundle.getSource(), bundle.getVersion());
//            String adAcc = createADAccession(bundle.getOrganism(), bundle.getSource(), bundle.getVersion());
//            String provider = createProvider(bundle.getSource(), bundle.getVersion());
//            DesignElementMappingBundle deBundle = new DesignElementMappingBundle(bundle.getSource(), bundle.getVersion(), adName, adAcc, "virtual", provider);
//
//            writeDesignElements(deBundle);
//        }
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

            bundle.setBioentityField(readValue("bioentity", adURL, csvReader));
            bundle.setGeneField(readValue("gene", adURL, csvReader));


            String[] headers = csvReader.readNext();


            Map<Integer, String> dbRefToColumn = new HashMap<Integer, String>(headers.length);

            //Start from the 2nd column because the 1st one has transcripts IDs
            for (int i = 1; i < headers.length; i++) {
                dbRefToColumn.put(i, headers[i]);
            }

            //optimized: put values not in Map, but in array, which is used in AtlasDao
            //read reference
            String[] line;
            int count = 0;
            List<Object[]> batch = new ArrayList<Object[]>(4000000);
            List<Object[]> propBatch = new ArrayList<Object[]>(250000);
            while ((line = csvReader.readNext()) != null) {
                String de = line[0];
                StringBuffer properties = new StringBuffer();
                if (StringUtils.isNotBlank(de)) {
                    for (int i = 1; i < line.length; i++) {
                        String[] values = StringUtils.split(line[i], "|");
                        if (values != null) {
                            for (String value : values) {
                                if (StringUtils.isNotBlank(value) && value.length() < 255) {
                                    String[] batchValues = new String[3];
                                    batchValues[0] = de;
                                    batchValues[1] = dbRefToColumn.get(i);
                                    batchValues[2] = value;
                                    batch.add(batchValues);
                                } else {
                                    getLog().debug("Value is too long: " + value);
                                }
                            }
                        }
                       properties.append(line[i]);
                       properties.append("\t");
                    }
                    String[] batchValues = new String[2];
                    batchValues[0]=de;
                    batchValues[1]= properties.toString();
                    propBatch.add(batchValues);
                    count++;
                }

                if (count % 5000 == 0) {
                    getLog().info("Parsed " + count + " bioentities with annotations");
                }

//                if (count > 90000) break;
            }

            getLog().info("bioentities with annotations batch.size() = " + batch.size());
            bundle.setBatch(batch);
            bundle.setBatchWithProp(propBatch);
            getLog().info("Parsed " + count + " bioentities with annotations");
        } catch (IOException e) {
            getLog().error("Problem when reading bioentity annotations file " + adURL);
        } finally {
            getLog().info("Finished reading from " + adURL + ", closing");
            closeQuietly(csvReader);
        }

        reportProgress(listener, "Parsing done. Starting bioentities loading");
        return bundle;
    }

    private String readValue(String type, URL adURL, CSVReader csvReader) throws IOException, AtlasLoaderException {
        String[] line = csvReader.readNext();
        if (!type.equalsIgnoreCase(line[0])) {
            getLog().error(type + " is not specified");
            throw new AtlasLoaderException(type + " is not specified in " + adURL + " file");
        }
        return line[1];
    }

    private void writeBioentities(BioentityBundle bundle, AtlasLoaderServiceListener listener) {

        getAtlasDAO().writeBioentityBundle1(bundle);

        reportProgress(listener, "writing done");
    }

    private void writeDesignElements(DesignElementMappingBundle bundle) {
        getAtlasDAO().writeVirtualArrayDesign(bundle, "transcript");
    }

    /**
     * Builds accession for virtual Array Design like for example:
     * From "homo sapiens" "Ensembl" "59" - > "HS-ENS-59"
     *
     * @param organism
     * @param source
     * @param version
     * @return
     */
    private String createADAccession(String organism, String source, String version) {
        StringBuilder sb = new StringBuilder();
        String[] orgStrs = StringUtils.split(organism, " ");
        for (String orgStr : orgStrs) {
            sb.append(StringUtils.substring(orgStr, 0, 1).toUpperCase());
        }
        sb.append("-");
        sb.append(StringUtils.substring(source, 0, 3).toUpperCase());
        sb.append("-");
        sb.append(version);

        return sb.toString();
    }

    private String createADName(String organism, String source, String version) {
        return organism + " " + source + "_v." + version;
    }

    private String createProvider(String source, String version) {
        return source + "_v." + version;
    }

    private void reportProgress(AtlasLoaderServiceListener listener, String report) {
        if (listener != null)
            listener.setProgress(report);
    }

}
