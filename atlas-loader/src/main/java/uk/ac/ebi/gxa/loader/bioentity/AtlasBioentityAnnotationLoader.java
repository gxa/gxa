package uk.ac.ebi.gxa.loader.bioentity;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import uk.ac.ebi.gxa.dao.BioEntityDAO;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.LoadBioentityCommand;
import uk.ac.ebi.gxa.loader.service.AtlasLoaderServiceListener;
import uk.ac.ebi.microarray.atlas.model.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.BioEntity;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * User: nsklyar
 * Date: Oct 21, 2010
 */
public class AtlasBioentityAnnotationLoader {

    private BioEntityDAO bioEntityDAO;

    private TransactionTemplate transactionTemplate;

    // logging
    final private Logger log = LoggerFactory.getLogger(this.getClass());

    public void process(LoadBioentityCommand command, final AtlasLoaderServiceListener listener) throws AtlasLoaderException {

        final List<String[]> geneTranscriptMapping = new ArrayList<String[]>();
        final Set<List<String>> transcriptProperties = new HashSet<List<String>>();
        final Set<List<String>> geneProperties = new HashSet<List<String>>();

        final Set<BEPropertyValue> bePropertyValues = new HashSet<BEPropertyValue>();

        final Set<BioEntity> transcripts = new HashSet<BioEntity>();
        final Set<BioEntity> genes = new HashSet<BioEntity>();


        reportProgress(listener, "Start parsing bioentity annotations from  " + command.getUrl());

        CSVReader csvReader = null;
        try {

            log.info("Starting to parse bioentity annotations from " + command.getUrl());

            csvReader = new CSVReader(new InputStreamReader(command.getUrl().openStream()), '\t', '"');

            String organism = readValue("organism", command.getUrl(), csvReader);
            final String source = readValue("source", command.getUrl(), csvReader);
            final String version = readValue("version", command.getUrl(), csvReader);

            final String transcriptField = readValue("bioentity", command.getUrl(), csvReader);
            final String geneField = readValue("gene", command.getUrl(), csvReader);

            String[] headers = csvReader.readNext();


            final Map<Integer, String> dbRefToColumn = new HashMap<Integer, String>(headers.length);
            int geneColumnIndex = -1;

            for (int i = 0; i < headers.length; i++) {
                dbRefToColumn.put(i, headers[i]);
                if (headers[i].equals(geneField)) {
                    geneColumnIndex = i;
                }
            }

            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                    getBioEntityDAO().writeProperties(new HashSet(dbRefToColumn.values()));
                }
            });

            if (geneColumnIndex < 0)
                log.info("Gene coulumn is not present in the annotation file");

            String[] line;
            int count = 0;

            while ((line = csvReader.readNext()) != null) {
                String identifier = line[0];

                if (StringUtils.isNotBlank(identifier)) {

                    String geneName = null;
                    if (geneColumnIndex > -1) {
                        geneName = line[geneColumnIndex];
                    }
                    //parse properties
                    for (int i = 1; i < line.length; i++) {
                        String[] values = StringUtils.split(line[i], "|");
                        if (values != null) {
                            for (String value : values) {
                                String propertyName = dbRefToColumn.get(i);
                                if (StringUtils.isNotBlank(value) && value.length() < 1000 && !"NA".equals(value)) {
                                    List<String> tnsProperty = new ArrayList<String>(3);
                                    tnsProperty.add(identifier);
                                    tnsProperty.add(propertyName);
                                    tnsProperty.add(value);
                                    transcriptProperties.add(tnsProperty);

                                    BEPropertyValue propertyValue = new BEPropertyValue(value, propertyName);
                                    bePropertyValues.add(propertyValue);


                                    if (geneColumnIndex > -1) {
                                        List<String> gProperty = new ArrayList<String>(3);
                                        gProperty.add(geneName);
                                        gProperty.add(propertyName);
                                        gProperty.add(value);
                                        geneProperties.add(gProperty);
                                    }

                                    if (propertyName.equalsIgnoreCase("Organism")) {
                                        organism = value;
                                    }

                                } else {
                                    log.debug("Value is too long: " + value);
                                }
                            }
                        }
                    }

                    //create transcript gene mapping
                    if (geneColumnIndex > -1) {
                        String[] gnToTns = new String[2];
                        gnToTns[0] = line[geneColumnIndex];
                        gnToTns[1] = identifier;
                        geneTranscriptMapping.add(gnToTns);

                        BioEntity gene = new BioEntity(geneName);
                        gene.setType(geneField);
                        gene.setSpecies(organism);
                        genes.add(gene);
                    }

                    BioEntity transcript = new BioEntity(identifier);
                    transcript.setType(transcriptField);
                    transcript.setSpecies(organism);
                    transcripts.add(transcript);

                    count++;
                }

                if (count % 5000 == 0) {
                    log.info("Parsed " + count + " bioentities with annotations");
                }
//                if (count > 20000) break;

            }

            log.info("Parsed " + count + " bioentities with annotations");

            reportProgress(listener, "Start wirting" + count + " bioentity annotations for " + organism);
            final String finalOrganism = organism;
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                    reportProgress(listener, "Wirting " + transcripts.size() + " transcripts for " + finalOrganism);
                    getBioEntityDAO().writeBioentities(transcripts);
                    reportProgress(listener, "Wirting " + genes.size() + " genes for " + finalOrganism);
                    getBioEntityDAO().writeBioentities(genes);
                    reportProgress(listener, "Wirting " + bePropertyValues.size() + " property values " + finalOrganism);
                    getBioEntityDAO().writePropertyValues(bePropertyValues);
                }
            });

            final int finalGeneColumnIndex = geneColumnIndex;
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                    reportProgress(listener, "Wirting " + transcriptProperties.size() + " properties for trasncripts " + finalOrganism);
                    getBioEntityDAO().writeBioEntityToPropertyValues(transcriptProperties, transcriptField, source, version);
                    if (finalGeneColumnIndex > -1) {
                        reportProgress(listener, "Wirting " + geneProperties.size() + " properties for genes " + finalOrganism);
                        getBioEntityDAO().writeBioEntityToPropertyValues(geneProperties, geneField, source, version);
                        reportProgress(listener, "Wirting " + geneTranscriptMapping.size() + " transcript to gene mappings " + finalOrganism);
                        getBioEntityDAO().writeGeneToTranscriptRelations(geneTranscriptMapping, transcriptField, geneField, source, version);
                    }
                }
            });

        } catch (IOException e) {
            log.error("Problem when reading bioentity annotations file " + command.getUrl());
        } finally {
            log.info("Finished reading from " + command.getUrl() + ", closing");
            closeQuietly(csvReader);
        }

    }

    private String readValue(String type, URL adURL, CSVReader csvReader) throws IOException, AtlasLoaderException {
        String[] line = csvReader.readNext();
        if (!type.equalsIgnoreCase(line[0])) {
            log.error(type + " is not specified");
            throw new AtlasLoaderException(type + " is not specified in " + adURL + " file");
        }
        return line[1];
    }

    private void reportProgress(AtlasLoaderServiceListener listener, String report) {
        if (listener != null)
            listener.setProgress(report);
    }

    public BioEntityDAO getBioEntityDAO() {
        if (bioEntityDAO == null) {
            throw new IllegalStateException("BioEntityDAO is not set.");
        }
        return bioEntityDAO;
    }

    public void setBioEntityDAO(BioEntityDAO bioEntityDAO) {
        this.bioEntityDAO = bioEntityDAO;
    }

    public void setTxManager(PlatformTransactionManager txManager) {
        Assert.notNull(txManager, "The 'transactionManager' argument must not be null.");
        this.transactionTemplate = new TransactionTemplate(txManager);

    }
}
