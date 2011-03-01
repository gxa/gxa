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
import uk.ac.ebi.gxa.loader.LoadArrayDesignMappingCommand;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.BioEntity;
import uk.ac.ebi.microarray.atlas.model.DesignElement;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * User: Nataliya Sklyar
 * Date: Nov 10, 2010
 */
public class ArrayDesignMappingLoader {

    private BioEntityDAO bioEntityDAO;

    private TransactionTemplate transactionTemplate;

    // logging
    final private Logger log = LoggerFactory.getLogger(this.getClass());

    public void process(LoadArrayDesignMappingCommand command) throws AtlasLoaderException {
        URL url = command.getUrl();
        CSVReader csvReader = null;

        final List<DesignElement> designElements = new ArrayList<DesignElement>();
        final List<BioEntity> bioentities = new ArrayList<BioEntity>();
        final Set<List<String>> deTobeMappings = new HashSet<List<String>>();

        try {
            csvReader = new CSVReader(new InputStreamReader(url.openStream()), '\t', '"');

            final ArrayDesign arrayDesign = new ArrayDesign();
            arrayDesign.setName(readValue("Array Design Name", url, csvReader));
            arrayDesign.setAccession(readValue("Array Design Accession", url, csvReader));
            arrayDesign.setType(readValue("Array Design Type", url, csvReader));
            arrayDesign.setProvider(readValue("Array Design Provider", url, csvReader));

            final String swName = readValue("Mapping Software Name", url, csvReader);
            final String swVersion = readValue("Mapping Software Version", url, csvReader);

            String organism = readValue("Organism", url, csvReader);
            if (StringUtils.isEmpty(organism))
                organism = "unknown";

            String bioentityType = readValue("BioEntity Type", url, csvReader);
            if (StringUtils.isEmpty(bioentityType))
                bioentityType = "designelement";

            String[] line;
            int count = 0;

            if (StringUtils.isNotEmpty(arrayDesign.getType()) && arrayDesign.getType().equalsIgnoreCase("virtual")) {
                //parse virtual array design
                //skip line with a table header
                csvReader.readNext();
                while ((line = csvReader.readNext()) != null) {
                    String de = line[0];
                    if (StringUtils.isNotBlank(de)) {
                        DesignElement designElement = new DesignElement(de, de);
                        designElements.add(designElement);

                        BioEntity bioEntity = new BioEntity(de);
                        bioEntity.setOrganism(organism);
                        bioEntity.setType(bioentityType);
                        bioentities.add(bioEntity);

                        ArrayList<String> de2be = new ArrayList<String>();
                        de2be.add(de);
                        de2be.add(de);
                        deTobeMappings.add(de2be);
                        count++;
                    }

                    if (count % 5000 == 0) {
                        log.info("Parsed " + count + " design elements");
                    }
                }

            } else {
                //parse mappings
                while ((line = csvReader.readNext()) != null) {
                    String de = line[0];
                    if (StringUtils.isNotBlank(de)) {
                        DesignElement designElement = new DesignElement(de, de);
                        designElements.add(designElement);

                        //read bioentity identifiers
                        if (line.length > 1) {
                            String[] values = StringUtils.split(line[1], ";");
                            if (values != null) {
                                for (String value : values) {
                                    if (StringUtils.isNotBlank(value)) {
                                        BioEntity bioEntity = new BioEntity(value);
                                        bioEntity.setOrganism(organism);
                                        bioEntity.setType(bioentityType);
                                        bioentities.add(bioEntity);

                                        //Each element contains [design element, bioentity Id]
                                        List<String> de2be = new ArrayList<String>(2);
                                        de2be.add(de);
                                        de2be.add(value);
                                        deTobeMappings.add(de2be);


                                        //read organism if available
                                        if (line.length > 2) {
                                            bioEntity.setOrganism(line[2]);
                                        }
                                    }
                                }
                                count++;
                            }
                        }
                    }

                    if (count % 5000 == 0) {
                        log.info("Parsed " + count + " design elements");
                    }
                }
            }

            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                    getBioEntityDAO().writeArrayDesign(arrayDesign, swName, swVersion);
                    getBioEntityDAO().writeDesignElements(designElements, arrayDesign.getAccession());
                    getBioEntityDAO().writeBioentities(bioentities);
                    getBioEntityDAO().writeDesignElementBioentityMappings(deTobeMappings, swName, swVersion, arrayDesign.getAccession());
                }
            });

        } catch (IOException e) {
            log.error("Problem when reading array design file " + url);
        } finally {
            log.info("Finished reading from " + url + ", closing");
            closeQuietly(csvReader);
        }


    }

    private String readValue(String type, URL adURL, CSVReader csvReader) throws IOException, AtlasLoaderException {
        String[] line = csvReader.readNext();
        if (!type.equalsIgnoreCase(line[0])) {
            log.error("Required field " + type + " is not specified");
            throw new AtlasLoaderException("Required field " + type + " is not specified in " + adURL + " file");
        }
        return line[1];
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
