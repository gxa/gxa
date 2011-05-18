package uk.ac.ebi.gxa.loader.bioentity;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.loader.UpdateAnnotationCommand;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.service.AtlasLoaderServiceListener;
import uk.ac.ebi.microarray.atlas.model.bioentity.AnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioMartAnnotationSource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * nsklyar
 * Date: 11/04/2011
 */
public class EnsemblAnnotationLoader extends AtlasBioentityAnnotationLoader {

    // logging
    final private Logger log = LoggerFactory.getLogger(this.getClass());

    private BioMartService bioMartService = new BioMartService();

    private void updateAnnotations(BioMartAnnotationSource annSrc) throws AtlasLoaderException {

//        Organism organism = bioEntityDAO.getEnsemblOrganismByEnsName(ensOrganism);
//
//        if (organism == null) {
//            throw new AtlasLoaderException("Organism " + ensOrganism + "is not found in the DB");
//        }
//
//        Software annSrc = organism.getAnnotationSource(BioEntityType.ENSGENE);

        CSVReader csvReader = null;
        try {
            //ToDo: move all the validation checks to different method
            //ToDo: add validation for organism name

            List<String> failedAttributes = bioMartService.validateAttributeNames(annSrc.getUrl(), annSrc.getDatasetName(),
                    annSrc.getMartToAtlasProperties().keySet());

            if (failedAttributes.size() > 0) {
                throw new AtlasLoaderException("BioEntity properties: " + failedAttributes + " are not in the BioMart anymore " + " for annotation source " + annSrc.getDisplayName());
            }

            String ensemblVersion = bioMartService.getDataSetVersion(annSrc.getUrl(), annSrc.getName());

            if (StringUtils.isNotEmpty(ensemblVersion) && StringUtils.isNotEmpty(annSrc.getVersion()) && ensemblVersion.equals(annSrc.getVersion())) {
                log.info("Ensembl versions in BioMart and in Atlas are the same!");

            }

//            this.software = new Software(annSrc.getName(), ensemblVersion);

//            Map<String, String> ensPropertyToPropertyName = bioEntityDAO.getEnsPropertyToPropertyName();

            boolean beExist = false;


            this.targetOrganism = annSrc.getOrganism().getAtlasName();
            reportProgress("Reading Ensembl annotations for organism " + targetOrganism);

            for (String ensProperty : annSrc.getMartToAtlasProperties().keySet()) {
                URL url = bioMartService.getPropertyURL(annSrc.getPropertyURLLocation(ensProperty));
                if (url != null) {
                    String atlasProperty = annSrc.getMartToAtlasProperties().get(ensProperty);
                    reportProgress("Reading " + atlasProperty + " for " + targetOrganism);
                    csvReader = new CSVReader(new InputStreamReader(url.openStream()), '\t', '"');
                    readProperty(csvReader, targetOrganism, atlasProperty, beExist);
                    csvReader.close();
                    beExist = true;
                }
            }

            writeBioentitiesAndAnnotations(BioEntityType.ENSTRANSCRIPT.value(), BioEntityType.ENSGENE.value());

        } catch (IOException e) {
            throw new AtlasLoaderException("Cannot update annotations for Organism " + annSrc.getDatasetName(), e);
        } catch (BioMartAccessException e) {
            throw new AtlasLoaderException("Cannot update annotations for Organism " + annSrc.getDatasetName(), e);
        } finally {
            closeQuietly(csvReader);
        }
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

            //ToDo: add BE.name
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
        String annotationSrcId = command.getAccession();
        AnnotationSource annotationSource = annSrcDAO.getById(Long.parseLong(annotationSrcId));
        if (annotationSource == null) {
            throw new AtlasLoaderException("No annotation source with id " + annotationSrcId);
        }
        //ToDo: find better way for this check, or avoid this, by having a reference to the service in AnnSrc object itself
        if (! (annotationSource instanceof BioMartAnnotationSource)) {
            throw new AtlasLoaderException("Wrong type of annotation source " + annotationSource.getDisplayName());
        }

        updateAnnotations((BioMartAnnotationSource)annotationSource);
    }
}
