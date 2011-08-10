package uk.ac.ebi.gxa.annotator.loader.file;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import uk.ac.ebi.gxa.annotator.AtlasAnnotationException;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityDAO;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.DesignElement;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

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

    protected final List<DesignElement> designElements = new ArrayList<DesignElement>();
    protected final Set<BioEntity> bioentities = new HashSet<BioEntity>();
    protected final Set<List<String>> deTobeMappings = new HashSet<List<String>>();

    protected Software software;
    protected ArrayDesign arrayDesign;
    // logging
    final private Logger log = LoggerFactory.getLogger(this.getClass());
    private AnnotationSourceDAO annSrcDAO;

    public void process(LoadArrayDesignMappingCommand command) throws AtlasAnnotationException {
        URL url = command.getUrl();
        CSVReader csvReader = null;


        try {
            csvReader = new CSVReader(new InputStreamReader(url.openStream()), '\t', '"');

            final ArrayDesign arrayDesign = new ArrayDesign(
                readValue("Array Design Accession", url, csvReader)
            );
            arrayDesign.setName(readValue("Array Design Name", url, csvReader));
            arrayDesign.setType(readValue("Array Design Type", url, csvReader));
            arrayDesign.setProvider(readValue("Array Design Provider", url, csvReader));

            software = annSrcDAO.findOrCreateSoftware(readValue("Mapping Software Name", url, csvReader), readValue("Mapping Software Version", url, csvReader));

            String organismName = readValue("Organism", url, csvReader);
            if (StringUtils.isEmpty(organismName))
                organismName = "unknown";

            Organism organism = annSrcDAO.findOrCreateOrganism(organismName);

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

                        BioEntity bioEntity = createBioEntity(organism, bioentityType, de);
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
                                        BioEntity bioEntity = createBioEntity(organism, bioentityType, value);
                                        bioentities.add(bioEntity);

                                        //Each element contains [design element, bioentity Id]
                                        List<String> de2be = new ArrayList<String>(2);
                                        de2be.add(de);
                                        de2be.add(value);
                                        deTobeMappings.add(de2be);


                                        //read organismName if available
                                        if (line.length > 2) {
                                            Organism deOrganism = annSrcDAO.findOrCreateOrganism(line[2]);
                                            bioEntity.setOrganism(deOrganism);
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

            final BioEntityType finalBioentityType = annSrcDAO.findOrCreateBioEntityType(bioentityType);
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                    bioEntityDAO.writeArrayDesign(arrayDesign, software);
                    bioEntityDAO.writeDesignElements(designElements, arrayDesign);
                    bioEntityDAO.writeBioentities(bioentities);
                    bioEntityDAO.writeDesignElementBioentityMappings(deTobeMappings, finalBioentityType, software,
                            arrayDesign);
                }
            });

        } catch (IOException e) {
            log.error("Problem when reading array design file " + url);
        } finally {
            log.info("Finished reading from " + url + ", closing");
            closeQuietly(csvReader);
        }


    }

    private BioEntity createBioEntity(Organism organism, String bioentityType, String de) {
        BioEntity bioEntity = new BioEntity(de, bioEntityDAO.findOrCreateBioEntityType(bioentityType));
        bioEntity.setOrganism(organism);
        return bioEntity;
    }

    private String readValue(String type, URL adURL, CSVReader csvReader) throws IOException, AtlasAnnotationException {
        String[] line = csvReader.readNext();
        if (!type.equalsIgnoreCase(line[0])) {
            log.error("Required field " + type + " is not specified");
            throw new AtlasAnnotationException("Required field " + type + " is not specified in " + adURL + " file");
        }
        return line[1];
    }

    public void setBioEntityDAO(BioEntityDAO bioEntityDAO) {
        this.bioEntityDAO = bioEntityDAO;
    }

    public void setTxManager(PlatformTransactionManager txManager) {
        Assert.notNull(txManager, "The 'transactionManager' argument must not be null.");
        this.transactionTemplate = new TransactionTemplate(txManager);

    }

    public void setAnnSrcDAO(AnnotationSourceDAO annSrcDAO) {
        this.annSrcDAO = annSrcDAO;
    }
}
