package uk.ac.ebi.gxa.loader.bioentity;

import au.com.bytecode.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.dao.AnnotationDAO;
import uk.ac.ebi.gxa.loader.service.AtlasLoaderServiceListener;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.annotation.AnnotationSource;
import uk.ac.ebi.microarray.atlas.model.annotation.BioMartAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.annotation.BioMartArrayDesign;
import uk.ac.ebi.microarray.atlas.model.annotation.BioMartProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Iterables.toArray;
import static com.google.common.io.Closeables.closeQuietly;

/**
 * nsklyar
 * Date: 11/04/2011
 */
public class EnsemblAnnotator extends AtlasBioentityAnnotator {

    // logging
    final private Logger log = LoggerFactory.getLogger(this.getClass());

    protected EnsemblAnnotator(AnnotationDAO annotationDAO, TransactionTemplate transactionTemplate) {
        super(annotationDAO, transactionTemplate);
    }

    public void updateAnnotations(String annotationSrcId, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
        setListener(listener);

        BioMartAnnotationSource annSrc = fetchAnnotationSource(annotationSrcId);
        CSVReader csvReader = null;
        try {
            BioMartConnection martConnection = BioMartConnectionFactory.createConnectionForAnnSrc(annSrc);

            this.targetOrganism = annSrc.getOrganism();
            reportProgress("Reading Ensembl annotations for organism " + targetOrganism.getName());

            //Create a list with biomart attribute names for bioentity types of  annotation source
            BETypeMartAttributesHandler attributesHandler = new BETypeMartAttributesHandler(annSrc);

            csvReader = readBioentities(csvReader, martConnection, attributesHandler);

            for (BioMartProperty bioMartProperty : annSrc.getBioMartProperties()) {
                //List of Attributes contains for example: {"ensembl_gene_id", "ensembl_transcript_id", "external_gene_id"}
                List<String> attributes = new ArrayList<String>(attributesHandler.getMartBEIdentifiers());
                attributes.add(bioMartProperty.getName());

                URL url = martConnection.getAttributesURL(attributes);
                if (url != null) {
                    reportProgress("Reading property " + bioMartProperty.getBioEntityProperty().getName() + " (" + bioMartProperty.getName() + ") for " + targetOrganism.getName());
                    csvReader = new CSVReader(new InputStreamReader(url.openStream()), '\t', '"');
                    readProperty(csvReader, bioMartProperty.getBioEntityProperty(), attributesHandler);
                    csvReader.close();
                }
            }

            writeBioentitiesAndAnnotations();
            annSrc.setApplied(true);

        } catch (IOException e) {
            throw new AtlasLoaderException("Cannot update annotations for Organism " + annSrc.getDatasetName(), e);
        } catch (BioMartAccessException e) {
            throw new AtlasLoaderException("Cannot update annotations for Organism " + annSrc.getDatasetName(), e);
        } finally {
            closeQuietly(csvReader);
        }
    }

    public void updateMappings(String annotationSrcId, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
        setListener(listener);

        BioMartAnnotationSource annSrc = fetchAnnotationSource(annotationSrcId);
        CSVReader csvReader = null;
        try {
            BioMartConnection martConnection = BioMartConnectionFactory.createConnectionForAnnSrc(annSrc);

            this.targetOrganism = annSrc.getOrganism();
            reportProgress("Reading Ensembl design element mappings for organism " + targetOrganism.getName());

            BETypeMartAttributesHandler attributesHandler = new BETypeMartAttributesHandler(annSrc);

            csvReader = readBioentities(csvReader, martConnection, attributesHandler);
            writeBioentitiesToDB();

            for (BioMartArrayDesign bioMartArrayDesign : annSrc.getBioMartArrayDesigns()) {
                List<String> attributes = new ArrayList<String>(attributesHandler.getMartBEIdentifiers());
                attributes.add(bioMartArrayDesign.getName());

                URL url = martConnection.getAttributesURL(attributes);
                if (url != null) {
                    reportProgress("Reading design elements for " + bioMartArrayDesign.getArrayDesign().getAccession() + " (" + bioMartArrayDesign.getName() + ") for " + targetOrganism.getName());
                    csvReader = new CSVReader(new InputStreamReader(url.openStream()), '\t', '"');
                    String[] line;
                    while ((line = csvReader.readNext()) != null) {
                        if (line.length < attributesHandler.martBEIdentifiers.size() + 1 || line[0].contains("Exception")) {
                            log.info("Cannot get property " + bioMartArrayDesign.getName());
                            break;
                        }
                        parseDesignElement(attributesHandler, line);
                    }
                    csvReader.close();
                }

                writeDesignElementBEMappingsToDB(bioMartArrayDesign.getArrayDesign());
            }

        } catch (IOException e) {
            throw new AtlasLoaderException("Cannot update annotations for Organism " + annSrc.getDatasetName(), e);
        } catch (BioMartAccessException e) {
            throw new AtlasLoaderException("Cannot update annotations for Organism " + annSrc.getDatasetName(), e);
        } finally {
            closeQuietly(csvReader);
        }
    }

    private BioMartAnnotationSource fetchAnnotationSource(String annotationSrcId) throws AtlasLoaderException {
        AnnotationSource annotationSource = annotationDAO.getAnnSrcById(Long.parseLong(annotationSrcId));
        if (annotationSource == null) {
            throw new AtlasLoaderException("No annotation source with id " + annotationSrcId);
        }

        this.annotationSource = annotationSource;
        return (BioMartAnnotationSource) annotationSource;
    }

   private CSVReader readBioentities(CSVReader csvReader, BioMartConnection martConnection, BETypeMartAttributesHandler attributesHandler) throws BioMartAccessException, IOException {

       List<String> martBENames = attributesHandler.getMartBEIdentifiersAndNames();

        URL beURL = martConnection.getAttributesURL(martBENames);
        if (beURL != null) {
            reportProgress("Reading bioentities for " + targetOrganism.getName());
            csvReader = new CSVReader(new InputStreamReader(beURL.openStream()), '\t', '"');
            String[] line;
            while ((line = csvReader.readNext()) != null) {
                if (line.length < attributesHandler.getMartBEIdentifiersAndNames().size() || line[0].contains("Exception")) {
                    log.info("Cannot get attributes " + attributesHandler.getMartBEIdentifiersAndNames() + " for organism " + targetOrganism);
                    break;
                }

                parseBioentities(targetOrganism, attributesHandler, line);
            }
            csvReader.close();
        }
        return csvReader;
    }

    private void parseBioentities(Organism organism, BETypeMartAttributesHandler attributesHandler, String[] line) {
        List<BioEntity> rowBioEntities = new ArrayList<BioEntity>(attributesHandler.getTypes().length);
        BioEntity gene = null;

        int columnCount = 0;
        for (BioEntityType type : attributesHandler.getTypes()) {
            String beIdentifier = line[columnCount++];
            String beName = line[columnCount++];
            BioEntity bioEntity = addBioEntity(beIdentifier, beName, type, organism);

            //we need it to create be2be relations. Saving only gene -> bioentity
            //ToDo: to be decided if we need to keep be2be relations and in which form
            if (type.getName().equals(BioEntityType.ENSGENE)) {
                gene = bioEntity;
            } else {
                rowBioEntities.add(bioEntity);
            }
        }

        if (gene != null) {
            for (BioEntity rowBioEntity : rowBioEntities) {
                addGeneBioEntityMapping(gene, rowBioEntity);
            }
        }
    }

    private void readProperty(CSVReader csvReader, BioEntityProperty property, BETypeMartAttributesHandler attributesHandler) throws IOException {

        String[] line;
        while ((line = csvReader.readNext()) != null) {
            if (line.length < attributesHandler.martBEIdentifiers.size() + 1 || line[0].contains("Exception")) {
                log.info("Cannot get property " + property.getName());
                break;
            }

            BEPropertyValue propertyValue = new BEPropertyValue(property, line[attributesHandler.martBEIdentifiers.size()]);
            int count = 0;
            for (BioEntityType type : attributesHandler.getTypes()) {
                addPropertyValue(line[count++], type, propertyValue);
            }

        }
    }

    private void parseDesignElement(BETypeMartAttributesHandler attributesHandler, String[] line) {
        String deAccession = line[attributesHandler.martBEIdentifiers.size()];
        int count = 0;
        for (BioEntityType type : attributesHandler.getTypes()) {
            addBEDesignElementMapping(line[count++], type, deAccession);
        }
    }

    private static class BETypeMartAttributesHandler {

        private final BioEntityType[] types;
        private final Set<BioMartProperty> bioMartProperties;
        private List<String> martBEIdentifiersAndNames = new ArrayList<String>();
        private List<String> martBEIdentifiers = new ArrayList<String>();

        private BETypeMartAttributesHandler(BioMartAnnotationSource annSrc) throws AtlasLoaderException {
            this.types = toArray(annSrc.getTypes(), BioEntityType.class);
            this.bioMartProperties = annSrc.getBioMartProperties();
            initMartAttributes();
        }

        /**
         * Initialize a List of mart attributes corresponding to BioEntityType's identifier and name, keeping an order
         * of BioEntityTypes
         *
         * @return
         */
        private void initMartAttributes() throws AtlasLoaderException {
            for (int i = 0; i < types.length; i++) {
                BioEntityType type = types[i];
                Set<String> idPropertyNames = getBioMartPropertyNamesForProperty(type.getIdentifierProperty());
                if (idPropertyNames.size() < 1) {
                    throw new AtlasLoaderException("Annotation source not valid ");
                }
                martBEIdentifiersAndNames.add(getFirst(idPropertyNames, null));
                martBEIdentifiers.add(getFirst(idPropertyNames, null));

                Set<String> namePropertyNames = getBioMartPropertyNamesForProperty(type.getNameProperty());
                if (namePropertyNames.size() < 1) {
                    throw new AtlasLoaderException("Annotation source not valid ");
                }
                martBEIdentifiersAndNames.add(getFirst(namePropertyNames, null));

            }
        }

        /**
         * Returns a List of mart attributes corresponding to BioEntityType's identifier and name, keeping an order
         * of BioEntityTypes
         *
         * @return
         */
        public List<String> getMartBEIdentifiersAndNames() {
            return Collections.unmodifiableList(martBEIdentifiersAndNames);
        }

        public List<String> getMartBEIdentifiers() {
            return Collections.unmodifiableList(martBEIdentifiers);
        }

        public BioEntityType[] getTypes() {
            return types;
        }

        private Set<String> getBioMartPropertyNamesForProperty(BioEntityProperty beProprety) {
            Set<String> answer = new HashSet<String>(bioMartProperties.size());
            for (BioMartProperty bioMartProperty : bioMartProperties) {
                System.out.println(beProprety.equals(beProprety.getName() + " " + bioMartProperty.getBioEntityProperty()));
                if (beProprety.equals(bioMartProperty.getBioEntityProperty())) {
                    answer.add(bioMartProperty.getName());
                }
            }
            return answer;
        }

    }
}
