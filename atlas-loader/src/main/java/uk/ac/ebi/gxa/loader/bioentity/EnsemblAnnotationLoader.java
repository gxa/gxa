package uk.ac.ebi.gxa.loader.bioentity;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.dao.AtlasDataAccessException;
import uk.ac.ebi.gxa.dao.BioMartDAO;
import uk.ac.ebi.gxa.loader.UpdateAnnotationCommand;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.service.AtlasLoaderServiceListener;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;
import uk.ac.ebi.microarray.atlas.model.bioentity.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * nsklyar
 * Date: 11/04/2011
 */
public class EnsemblAnnotationLoader extends AtlasBioentityAnnotationLoader {

    // logging
    final private Logger log = LoggerFactory.getLogger(this.getClass());

    private BioMartDAO bioMartDAO = new BioMartDAO();


    private void updateAnnotationsForOrganism(String ensOrganism) throws AtlasLoaderException {

//        Organism organism = bioEntityDAO.getEnsemblOrganismByEnsName(ensOrganism);
//
//        if (organism == null) {
//            throw new AtlasLoaderException("Organism " + ensOrganism + "is not found in the DB");
//        }
//
//        Software annotationSource = organism.getAnnotationSource(BioEntityType.ENSGENE);
//
//        CSVReader csvReader = null;
//        try {
//            List<String> failedAttributes = bioMartDAO.validateAttributeNames(annotationSource.getUrl(), organism.getEnsemblName(), organism.getProperties());
//            if (failedAttributes.size() > 0) {
//                throw new AtlasLoaderException("BioEntity properties: " + failedAttributes + " are not in the BioMart anymore " + " for organism " + ensOrganism);
//            }
//
//            String ensemblVersion = bioMartDAO.getDataSetVersion(annotationSource.getUrl(), annotationSource.getName());
//
//            if (StringUtils.isNotEmpty(ensemblVersion) && StringUtils.isNotEmpty(annotationSource.getVersion()) && ensemblVersion.equals(annotationSource.getVersion())) {
//                log.info("Ensembl versions in BioMart and in Atlas are the same!");
//
//            }
//
//            this.software = new Software(annotationSource.getName(), ensemblVersion);
//
//            Map<String, String> ensPropertyToPropertyName = bioEntityDAO.getEnsPropertyToPropertyName();
//
//            boolean beExist = false;
//
//            this.targetOrganism = organism.getAtlasName();
//            reportProgress("Reading Ensembl annotations for organism " + targetOrganism);
//            for (String ensProperty : organism.getProperties()) {
//
//                URL url = bioMartDAO.getPropertyForOrganismURL(ensOrganism, ensProperty);
//                if (url != null) {
//                    String atlasProperty = ensPropertyToPropertyName.get(ensProperty);
//                    reportProgress("Reading " + atlasProperty + " for " + targetOrganism);
//                    csvReader = new CSVReader(new InputStreamReader(url.openStream()), '\t', '"');
//                    readProperty(csvReader, organism.getAtlasName(), atlasProperty, beExist);
//                    csvReader.close();
//                    beExist = true;
//                }
//
//            }
//
//            writeBioentitiesAndAnnotations(BioEntityType.ENSTRANSCRIPT.value(), BioEntityType.ENSGENE.value());
//
//        } catch (IOException e) {
//            throw new AtlasLoaderException("Cannot update annotations for Organism " + ensOrganism, e);
//        } catch (AtlasDataAccessException e) {
//            throw new AtlasLoaderException("Cannot update annotations for Organism " + ensOrganism, e);
//        } finally {
//            closeQuietly(csvReader);
//        }
    }

    private void readProperty(CSVReader csvReader, String organism, String propertyName, boolean beExist) throws IOException {

        String[] line;

        while ((line = csvReader.readNext()) != null) {
            if (line.length < 1 || line[0].contains("Exception")) {
                log.info("Cannot get property " + propertyName + " for organism " + organism);
                break;
            }

            String beIdentifier = line[1];
            String geneIdentifier = line[0];
            String value = line[2];


            addPropertyValue(beIdentifier, geneIdentifier, propertyName, value);

            if (propertyName.equalsIgnoreCase(BioEntity.NAME_PROPERTY_SYMBOL)) {
                
            }

            if (!beExist) {
                addTranscriptGeneMapping(beIdentifier, geneIdentifier);
                addGene(organism, BioEntityType.ENSGENE.value(), geneIdentifier, null);
                addTransctipt(organism, BioEntityType.ENSTRANSCRIPT.value(), beIdentifier);
            }


        }
    }

    public void process(UpdateAnnotationCommand command, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
        setListener(listener);
        updateAnnotationsForOrganism(command.getAccession());
    }
}
