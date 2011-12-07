package uk.ac.ebi.gxa.annotator.process;

import uk.ac.ebi.gxa.annotator.AtlasAnnotationException;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.loader.AtlasBioEntityDataWriter;
import uk.ac.ebi.gxa.annotator.loader.biomart.BioMartParser;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityAnnotationData;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityAnnotationDataBuilder;
import uk.ac.ebi.gxa.annotator.loader.filebased.FileBasedConnection;
import uk.ac.ebi.gxa.annotator.model.genesigdb.GeneSigAnnotationSource;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityPropertyDAO;

import java.io.IOException;

/**
 * User: nsklyar
 * Date: 05/12/2011
 */
public class FileBasedAnnotator  extends Annotator<GeneSigAnnotationSource>{

    public FileBasedAnnotator(AnnotationSourceDAO annSrcDAO, BioEntityPropertyDAO propertyDAO, AtlasBioEntityDataWriter beDataWriter) {
        super(annSrcDAO, propertyDAO, beDataWriter);
    }

    public void updateAnnotations(String annotationSrcId) {

        GeneSigAnnotationSource annSrc = null;
        try {
            annSrc = fetchAnnotationSource(annotationSrcId);


            //Create a list with biomart attribute names for bioentity types of  annotation source
            BETypeMartAttributesHandler attributesHandler = new BETypeMartAttributesHandler(annSrc);
            BioEntityAnnotationDataBuilder builder = new BioEntityAnnotationDataBuilder();
            BioMartParser<BioEntityAnnotationData> parser = BioMartParser.initParser(attributesHandler.getTypes(), builder);

            FileBasedConnection martConnection = annSrc.createConnection();

            //read properties
            parser.parseBioMartPropertyValues(attributesHandler.getBioEntityProperties(), martConnection.getURL(annSrc.getUrl()), false);

//            for (ExternalBioEntityProperty entityPropertyExternal : annSrc.getExternalBioEntityProperties()) {
//                //List of Attributes contains for example: {"ensembl_gene_id", "ensembl_transcript_id", "external_gene_id"}
//                List<String> attributes = new ArrayList<String>(attributesHandler.getMartBEIdentifiers());
//                attributes.add(entityPropertyExternal.getName());
//
//
//                URL url = martConnection.getAttributesURL(attributes);
//                if (url != null) {
//                    reportProgress("Reading property " + entityPropertyExternal.getBioEntityProperty().getName() + " (" + entityPropertyExternal.getName() + ") for " + organism.getName());
//                    log.debug("Parsing property {} ", entityPropertyExternal.getBioEntityProperty().getName());
//                    long startTime = currentTimeMillis();
//
//                    parser.parseBioMartPropertyValues(entityPropertyExternal.getBioEntityProperty(), url);
//
//                    log.debug("Done. {} millseconds).\n", (currentTimeMillis() - startTime));
//                }
//            }

            final BioEntityAnnotationData data = parser.getData();

//            beDataWriter.writeBioEntities(data, listener);
            beDataWriter.writePropertyValues(data.getPropertyValues(), listener);
            beDataWriter.writeBioEntityToPropertyValues(data, annSrc, true, listener);

            reportSuccess("Update annotations from Annotation Source " + annSrc.getName() + " completed");
        } catch (AtlasAnnotationException e) {
            reportError(e);
        } catch (IOException e) {
            reportError(new AtlasAnnotationException("Cannot read annotations from URL " + annSrc.getUrl() +
                    " for AnnSrc " + annSrc.getName(), e));
        }
    }

    @Override
    protected Class<GeneSigAnnotationSource> getClazz() {
        return GeneSigAnnotationSource.class;
    }
}
