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

    public void process(LoadBioentityCommand command, AtlasLoaderServiceListener listener) throws AtlasLoaderException {

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

            String transcriptField = readValue("bioentity", command.getUrl(), csvReader);
            String geneField = readValue("gene", command.getUrl(), csvReader);

            String[] headers = csvReader.readNext();


            Map<Integer, String> dbRefToColumn = new HashMap<Integer, String>(headers.length);
            int geneColumnIndex = -1;

            for (int i = 0; i < headers.length; i++) {
                dbRefToColumn.put(i, headers[i]);
                if (headers[i].equals(geneField)) {
                    geneColumnIndex = i;
                }
            }

            getBioEntityDAO().writeProperties(dbRefToColumn.values());

            if (geneColumnIndex < 0)
                throw new AtlasLoaderException("Gene coulumn is not present in the annotation file");

            String[] line;
            int count = 0;

            while ((line = csvReader.readNext()) != null) {
                String identifier = line[0];

                if (StringUtils.isNotBlank(identifier)) {

                    String geneName = line[geneColumnIndex];
                    //parse properties
                    for (int i = 1; i < line.length; i++) {
                        String[] values = StringUtils.split(line[i], "|");
                        if (values != null) {
                            for (String value : values) {
                                if (StringUtils.isNotBlank(value) && value.length() < 255 && !"NA".equals(value)) {
                                    List<String> tnsProperty = new ArrayList<String>(3);
                                    tnsProperty.add(identifier);
                                    tnsProperty.add(dbRefToColumn.get(i));
                                    tnsProperty.add(value);
                                    transcriptProperties.add(tnsProperty);

                                    BEPropertyValue propertyValue = new BEPropertyValue(value, dbRefToColumn.get(i));
                                    bePropertyValues.add(propertyValue);


                                    List<String> gProperty = new ArrayList<String>(3);
                                    gProperty.add(geneName);
                                    gProperty.add(dbRefToColumn.get(i));
                                    gProperty.add(value);
                                    geneProperties.add(gProperty);

                                } else {
                                    log.debug("Value is too long: " + value);
                                }
                            }
                        }
                    }

                    //create transcript gene mapping
                    String[] gnToTns = new String[2];
                    gnToTns[0] = line[geneColumnIndex];
                    gnToTns[1] = identifier;
                    geneTranscriptMapping.add(gnToTns);

                    BioEntity transcript = new BioEntity(identifier);
                    transcript.setType(transcriptField);
                    transcript.setOrganism(organism);
                    transcripts.add(transcript);

                    BioEntity gene = new BioEntity(geneName);
                    gene.setType(geneField);
                    gene.setOrganism(organism);
                    genes.add(gene);

                    count++;
                }

                if (count % 5000 == 0) {
                    log.info("Parsed " + count + " bioentities with annotations");
                }
//                if (count > 20000) break;

            }

            log.info("Parsed " + count + " bioentities with annotations");

            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                    getBioEntityDAO().writeBioentities(transcripts);
                    getBioEntityDAO().writeBioentities(genes);
                    getBioEntityDAO().writePropertyValues(bePropertyValues);
                    getBioEntityDAO().writeBioEntityToPropertyValues(transcriptProperties, source, version);
                    getBioEntityDAO().writeBioEntityToPropertyValues(geneProperties, source, version);
                    getBioEntityDAO().writeGeneToTranscriptRelations(geneTranscriptMapping, source, version);
                }
            });


        } catch (IOException e) {
            log.error("Problem when reading bioentity annotations file " + command.getUrl());
        } finally {
            log.info("Finished reading from " + command.getUrl() + ", closing");
            closeQuietly(csvReader);
        }

    }

    //    public void process(LoadBioentityCommand command, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
//
//
//        bundle = parseAnnotations(command.getUrl(), listener);
//
//        if (bundle == null) {
//            throw new AtlasLoaderException("Cannot parse bioentity annotations from " + command.getUrl());
//        }
//
//        writeBioentities(bundle, listener);
//        if (command.isUpdateVirtualDesign()) {
//            reportProgress(listener, "writing virtual array design");
//            String adName = createADName(bundle.getOrganism(), bundle.getSource(), bundle.getVersion());
//            String adAcc = createADAccession(bundle.getOrganism(), bundle.getSource(), bundle.getVersion());
//            String provider = createProvider(bundle.getSource(), bundle.getVersion());
//            DesignElementMappingBundle deBundle = new DesignElementMappingBundle(bundle.getSource(), bundle.getVersion(), adName, adAcc, "virtual", provider);
//
//            writeDesignElements(deBundle);
//        }
//    }
//
//    private BioentityBundle parseAnnotations(URL adURL, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
//
//        reportProgress(listener, "Start parsing bioentity annotations from  " + adURL);
//
//        bundle = null;
//
//        CSVReader csvReader = null;
//        try {
//
//            log.info("Starting to parse bioentity annotations from " + adURL);
//
//            csvReader = new CSVReader(new InputStreamReader(adURL.openStream()), '\t', '"');
//
//            bundle = new BioentityBundle();
//
//            bundle.setOrganism(readValue("organism", adURL, csvReader));
//            bundle.setSource(readValue("source", adURL, csvReader));
//            bundle.setVersion(readValue("version", adURL, csvReader));
//
//            bundle.setBioentityField(readValue("bioentity", adURL, csvReader));
//            bundle.setGeneField(readValue("gene", adURL, csvReader));
//
//
//            String[] headers = csvReader.readNext();
//
//
//            Map<Integer, String> dbRefToColumn = new HashMap<Integer, String>(headers.length);
//
//            //Start from the 2nd column because the 1st one has transcripts IDs
//            for (int i = 1; i < headers.length; i++) {
//                dbRefToColumn.put(i, headers[i]);
//            }
//
//            //optimized: put values not in Map, but in array, which is used in AtlasDao
//            //read reference
//            String[] line;
//            int count = 0;
//            List<Object[]> batch = new ArrayList<Object[]>(4000000);
//            List<Object[]> propBatch = new ArrayList<Object[]>(250000);
//
//            while ((line = csvReader.readNext()) != null) {
//                String de = line[0];
//                StringBuffer properties = new StringBuffer();
//                if (StringUtils.isNotBlank(de)) {
//                    for (int i = 1; i < line.length; i++) {
//                        String[] values = StringUtils.split(line[i], "|");
//                        if (values != null) {
//                            for (String value : values) {
//                                if (StringUtils.isNotBlank(value) && value.length() < 255) {
//                                    String[] batchValues = new String[3];
//                                    batchValues[0] = de;
//                                    batchValues[1] = dbRefToColumn.get(i);
//                                    batchValues[2] = value;
//                                    batch.add(batchValues);
//                                } else {
//                                    log.debug("Value is too long: " + value);
//                                }
//                            }
//                        }
//                        properties.append(line[i]);
//                        properties.append("\t");
//                    }
//                    String[] batchValues = new String[2];
//                    batchValues[0] = de;
//                    batchValues[1] = properties.toString();
//                    propBatch.add(batchValues);
//                    count++;
//                }
//
//                if (count % 5000 == 0) {
//                    log.info("Parsed " + count + " bioentities with annotations");
//                }
//
////                if (count > 90000) break;
//            }
//
//            log.info("Parsed " + count + " bioentities with annotations");
//        } catch (IOException e) {
//            log.error("Problem when reading bioentity annotations file " + adURL);
//        } finally {
//            log.info("Finished reading from " + adURL + ", closing");
//            closeQuietly(csvReader);
//        }
//
//        reportProgress(listener, "Parsing done. Starting bioentities loading");
//        return bundle;
//    }
//
    private String readValue(String type, URL adURL, CSVReader csvReader) throws IOException, AtlasLoaderException {
        String[] line = csvReader.readNext();
        if (!type.equalsIgnoreCase(line[0])) {
            log.error(type + " is not specified");
            throw new AtlasLoaderException(type + " is not specified in " + adURL + " file");
        }
        return line[1];
    }

//    private void writeBioentities(BioentityBundle bundle, AtlasLoaderServiceListener listener) {
//
//        getBioEntityDAO().writeBioentityBundle1(bundle);
//
//        reportProgress(listener, "writing done");
//    }
//
//    private void writeDesignElements(DesignElementMappingBundle bundle) {
//        getBioEntityDAO().writeVirtualArrayDesign(bundle, "transcript");
//    }

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
