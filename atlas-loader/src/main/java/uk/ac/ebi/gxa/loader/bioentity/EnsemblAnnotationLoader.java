package uk.ac.ebi.gxa.loader.bioentity;

import au.com.bytecode.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.loader.UpdateAnnotationCommand;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
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

import static com.google.common.io.Closeables.closeQuietly;

/**
 * nsklyar
 * Date: 11/04/2011
 */
public class EnsemblAnnotationLoader extends AtlasBioentityAnnotationLoader {

    // logging
    final private Logger log = LoggerFactory.getLogger(this.getClass());

//    private BioMartService bioMartService;

//    public void setBioMartService(BioMartService bioMartService) {
//        this.bioMartService = bioMartService;
//    }

    private void updateAnnotations(BioMartAnnotationSource annSrc) throws AtlasLoaderException {

        CSVReader csvReader = null;
        try {
            BioMartConnection martConnection = BioMartConnectionFactory.createConnectionForAnnSrc(annSrc);

            boolean beExist = false;

            this.targetOrganism = annSrc.getOrganism();
            reportProgress("Reading Ensembl annotations for organism " + targetOrganism);

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

    public void process(UpdateAnnotationCommand command, AtlasLoaderServiceListener listener) throws AtlasLoaderException {
        setListener(listener);
        String annotationSrcId = command.getAccession();
        AnnotationSource annotationSource = annSrcDAO.getById(Long.parseLong(annotationSrcId));
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
}
