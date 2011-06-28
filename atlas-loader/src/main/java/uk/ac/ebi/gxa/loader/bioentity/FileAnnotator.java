package uk.ac.ebi.gxa.loader.bioentity;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.LoadBioentityCommand;
import uk.ac.ebi.gxa.loader.dao.AnnotationDAO;
import uk.ac.ebi.gxa.loader.service.AtlasLoaderServiceListener;
import uk.ac.ebi.microarray.atlas.model.annotation.FileAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * User: nsklyar
 * Date: 13/04/2011
 */
public class FileAnnotator extends AtlasBioentityAnnotator {
    // logging
    final private Logger log = LoggerFactory.getLogger(this.getClass());

    private String transcriptField;
    private String geneField;

    protected FileAnnotator(AnnotationDAO annotationDAO, TransactionTemplate transactionTemplate) {
        super(annotationDAO, transactionTemplate);
    }

    public void process(URL url, final AtlasLoaderServiceListener listener) throws AtlasLoaderException {

        setListener(listener);

        reportProgress("Start parsing bioentity annotations from  " + url);

        CSVReader csvReader = null;
        try {

            log.info("Starting to parse bioentity annotations from " + url);

            csvReader = new CSVReader(new InputStreamReader(url.openStream()), '\t', '"');

            initFields(url, csvReader);

            String[] headers = csvReader.readNext();

            int geneColumnIndex = -1;
            List<BioEntityProperty> properties = new ArrayList<BioEntityProperty>();

            for (int i = 0; i < headers.length; i++) {
                properties.add(i, new BioEntityProperty(null, headers[i]));
                if (headers[i].equals(geneField)) {
                    geneColumnIndex = i;
                }
            }

            saveProperties(properties);

            if (geneColumnIndex < 0)
                log.info("Gene coulumn is not present in the annotation file");

            String[] line;
            int count = 0;

            while ((line = csvReader.readNext()) != null) {
                String beIdentifier = line[0];

                if (StringUtils.isNotBlank(beIdentifier)) {

                    String geneIdentifier = null;
                    if (geneColumnIndex > -1) {
                        geneIdentifier = line[geneColumnIndex];
                    }
                    String geneName = null;
                    //parse properties
                    for (int i = 1; i < line.length; i++) {
                        String[] values = StringUtils.split(line[i], "|");

                        BioEntityProperty property = properties.get(i);
                        if (values != null) {
                            for (String value : values) {
                                addPropertyValue(beIdentifier, geneIdentifier, property, value);
                            }
                        }
                        
                        if (property.getName().equalsIgnoreCase("Organism") && StringUtils.isNotBlank(line[i])) {
                            this.targetOrganism = annotationDAO.findOrCreateOrganism(line[i]);
                        }

                        if (BioEntity.NAME_PROPERTY_SYMBOL.equalsIgnoreCase(property.getName()) ||
                                BioEntity.NAME_PROPERTY_MIRBASE.equalsIgnoreCase(property.getName())) {
                            geneName = line[i];
                        }
                    }

                    //create transcript gene mapping
                    if (geneIdentifier != null) {
                        addTranscriptGeneMapping(beIdentifier, geneIdentifier);
                        addGene(targetOrganism, geneField, geneIdentifier, geneName);
                    }

                    addTransctipt(targetOrganism, transcriptField, beIdentifier);

                    count++;
                }

                if (count % 5000 == 0) {
                    log.info("Parsed " + count + " bioentities with annotations");
                }
            }

            log.info("Parsed " + count + " bioentities with annotations");

            writeBioentitiesAndAnnotations(transcriptField, geneField);

        } catch (IOException e) {
            log.error("Problem when reading bioentity annotations file " + url);
        } finally {
            log.info("Finished reading from " + url + ", closing");
            closeQuietly(csvReader);
        }

    }

    private void initFields(URL url, CSVReader csvReader) throws IOException, AtlasLoaderException {
        this.targetOrganism = annotationDAO.findOrCreateOrganism(readValue("organism", url, csvReader));
        String sourceName = readValue("source", url, csvReader);
        String version = readValue("version", url, csvReader);

        transcriptField = readValue("bioentity", url, csvReader);
        geneField = readValue("gene", url, csvReader);

        Software software = new Software(null, sourceName, version);
        this.annotationSource = new FileAnnotationSource(software, targetOrganism, url.getFile());
        annotationDAO.saveAnnSrc(annotationSource);
    }

    private String readValue(String type, URL adURL, CSVReader csvReader) throws IOException, AtlasLoaderException {
        String[] line = csvReader.readNext();
        if (!type.equalsIgnoreCase(line[0])) {
            log.error(type + " is not specified");
            throw new AtlasLoaderException(type + " is not specified in " + adURL + " file");
        }
        return line[1];
    }

     protected void saveProperties(final List<BioEntityProperty> properties) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                for (BioEntityProperty property : properties) {
                    annotationDAO.saveProperty(property);
                }
            }
        });
    }
}
