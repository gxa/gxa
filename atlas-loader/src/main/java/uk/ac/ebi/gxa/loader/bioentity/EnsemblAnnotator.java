package uk.ac.ebi.gxa.loader.bioentity;

import au.com.bytecode.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;
import uk.ac.ebi.gxa.loader.UpdateAnnotationCommand;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.dao.AnnotationDAO;
import uk.ac.ebi.gxa.loader.service.AtlasLoaderServiceListener;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.annotation.AnnotationSource;
import uk.ac.ebi.microarray.atlas.model.annotation.BioMartAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.annotation.BioMartProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
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

    private void updateAnnotations(BioMartAnnotationSource annSrc) throws AtlasLoaderException {

        CSVReader csvReader = null;
        try {
            BioMartConnection martConnection = BioMartConnectionFactory.createConnectionForAnnSrc(annSrc);

            boolean beExist = false;

            this.targetOrganism = annSrc.getOrganism();
            reportProgress("Reading Ensembl annotations for organism " + targetOrganism);

            initTypeBioentityMap(annSrc.getTypes());

            //Create a list with biomart attribute names for bioentity types of  annotation source
            TypeMartAttributesHandler attributesHandler = new TypeMartAttributesHandler(annSrc);
            List<String> martBENames = attributesHandler.getMartAttributes();

            URL beURL = martConnection.getAttributesURL(martBENames);
            if (beURL != null) {
                reportProgress("Reading bioentities for " + targetOrganism);
                csvReader = new CSVReader(new InputStreamReader(beURL.openStream()), '\t', '"');
                readBioenties(csvReader, targetOrganism, attributesHandler);
                csvReader.close();
                beExist = true;
            }

            for (BioMartProperty bioMartProperty : annSrc.getBioMartProperties()) {
                URL url = martConnection.getPropertyURL(bioMartProperty.getName());
                if (url != null) {
                    reportProgress("Reading " + bioMartProperty.getBioEntityProperty().getName() + " for " + targetOrganism);
                    csvReader = new CSVReader(new InputStreamReader(url.openStream()), '\t', '"');
                    readProperty(csvReader, targetOrganism, bioMartProperty.getBioEntityProperty(), beExist);
                    csvReader.close();
                    beExist = true;
                }
            }

            writeBioentitiesAndAnnotations(BioEntityType.ENSTRANSCRIPT, BioEntityType.ENSGENE);

        } catch (IOException e) {
            throw new AtlasLoaderException("Cannot update annotations for Organism " + annSrc.getDatasetName(), e);
        } catch (BioMartAccessException e) {
            throw new AtlasLoaderException("Cannot update annotations for Organism " + annSrc.getDatasetName(), e);
        } finally {
            closeQuietly(csvReader);
        }
    }

    private void readBioenties(CSVReader csvReader, Organism organism, TypeMartAttributesHandler attributesHandler) throws IOException {

        String[] line;

        while ((line = csvReader.readNext()) != null) {
            if (line.length < attributesHandler.getMartAttributes().size() || line[0].contains("Exception")) {
                log.info("Cannot get attributes " + attributesHandler.getMartAttributes() + " for organism " + organism);
                break;
            }

            for (BioEntityType type : attributesHandler.getTypes()) {

            }
            String beIdentifier = line[1];
            String geneIdentifier = line[0];
            String value = line[2];


            addPropertyValue(beIdentifier, geneIdentifier, property, value);

            //ToDo: add BE.name
            if (property.getName().equalsIgnoreCase(BioEntity.NAME_PROPERTY_SYMBOL)) {

            }

            if (!beExist) {
                addTranscriptGeneMapping(beIdentifier, geneIdentifier);
                addGene(organism, BioEntityType.ENSGENE, geneIdentifier, null);
                addTransctipt(organism, BioEntityType.ENSTRANSCRIPT, beIdentifier);
            }


        }
    }

    private void readProperty(CSVReader csvReader, Organism organism, BioEntityProperty property, boolean beExist) throws IOException {

        String[] line;

        while ((line = csvReader.readNext()) != null) {
            if (line.length < 1 || line[0].contains("Exception")) {
                log.info("Cannot get property " + property.getName() + " for organism " + organism);
                break;
            }

            String beIdentifier = line[1];
            String geneIdentifier = line[0];
            String value = line[2];


            addPropertyValue(beIdentifier, geneIdentifier, property, value);

            //ToDo: add BE.name
            if (property.getName().equalsIgnoreCase(BioEntity.NAME_PROPERTY_SYMBOL)) {

            }

            if (!beExist) {
                addTranscriptGeneMapping(beIdentifier, geneIdentifier);
                addGene(organism, BioEntityType.ENSGENE, geneIdentifier, null);
                addTransctipt(organism, BioEntityType.ENSTRANSCRIPT, beIdentifier);
            }


        }
    }

    public void process(String annotationSrcId, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
        setListener(listener);
        AnnotationSource annotationSource = annotationDAO.getAnnSrcById(Long.parseLong(annotationSrcId));
        if (annotationSource == null) {
            throw new AtlasLoaderException("No annotation source with id " + annotationSrcId);
        }
        //ToDo: find better way for this check, or avoid this, by having a reference to the service in AnnSrc object itself
        if (!(annotationSource instanceof BioMartAnnotationSource)) {
            throw new AtlasLoaderException("Wrong type of annotation source " + annotationSource.getDisplayName());
        }

        this.annotationSource = annotationSource;
        updateAnnotations((BioMartAnnotationSource) annotationSource);
    }

    private class TypeMartAttributesHandler {

        private final BioEntityType[] types;
        private final Set<BioMartProperty> bioMartProperties;
        private  List<String> martBENames;

        private TypeMartAttributesHandler(BioMartAnnotationSource annSrc) throws AtlasLoaderException {
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
            this.martBENames = new ArrayList<String>();
            for (int i = 0; i < types.length; i++) {
                BioEntityType type = types[i];
                Set<String> idPropertyNames = getBioMartPropertyNamesForProperty(type.getIdentifierProperty());
                if (idPropertyNames.size() < 1) {
                    throw new AtlasLoaderException("Annotation source not valid ");
                }
                Set<String> namePropertyNames = getBioMartPropertyNamesForProperty(type.getNameProperty());
                if (namePropertyNames.size() < 1) {
                    throw new AtlasLoaderException("Annotation source not valid ");
                }
                martBENames.add(getFirst(namePropertyNames, null));

            }
        }
        
        /**
         * Returns a List of mart attributes corresponding to BioEntityType's identifier and name, keeping an order
         * of BioEntityTypes
         *
         * @return
         */
        public List<String> getMartAttributes() {
            return martBENames;
        }

        public BioEntityType[] getTypes() {
            return types;
        }

        private Set<String> getBioMartPropertyNamesForProperty(BioEntityProperty beProprety) {
            Set<String> answer = new HashSet<String>(bioMartProperties.size());
            for (BioMartProperty bioMartProperty : bioMartProperties) {
                if (beProprety.equals(bioMartProperty)) {
                    answer.add(bioMartProperty.getName());
                }
            }
            return answer;
        }

    }
}
