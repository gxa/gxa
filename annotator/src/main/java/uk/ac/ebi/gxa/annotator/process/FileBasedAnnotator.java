package uk.ac.ebi.gxa.annotator.process;

import uk.ac.ebi.gxa.annotator.AtlasAnnotationException;
import uk.ac.ebi.gxa.annotator.loader.AtlasBioEntityDataWriter;
import uk.ac.ebi.gxa.annotator.loader.biomart.AnnotationSourceAccessException;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityAnnotationData;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityAnnotationDataBuilder;
import uk.ac.ebi.gxa.annotator.loader.filebased.GeneSigConnection;
import uk.ac.ebi.gxa.annotator.model.genesigdb.GeneSigAnnotationSource;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * User: nsklyar
 * Date: 05/12/2011
 */
public class FileBasedAnnotator  extends Annotator<GeneSigAnnotationSource>{

    public FileBasedAnnotator(GeneSigAnnotationSource annSrc, AtlasBioEntityDataWriter beDataWriter) {
        super(annSrc, beDataWriter);
    }

    @Override
    public void updateAnnotations() {
        try {
            //Create a list with biomart attribute names for bioentity types of  annotation source
            BETypeExternalAttributesHandler attributesHandler = new BETypeExternalAttributesHandler(annSrc);
            BioEntityAnnotationDataBuilder builder = new BioEntityAnnotationDataBuilder();
            AnnotationParser<BioEntityAnnotationData> parser = AnnotationParser.initParser(attributesHandler.getTypes(), builder);
            parser.setSeparator(annSrc.getSeparator());

            GeneSigConnection martConnection = annSrc.createConnection();

            reportProgress("Reading properties from Annotation Source " + annSrc.getName());
            //read properties
            parser.parsePropertyValues(attributesHandler.getBioEntityProperties(),
                    martConnection.getURL(), true);

            final BioEntityAnnotationData data = parser.getData();

            beDataWriter.writePropertyValues(data.getPropertyValues(), listener);
            beDataWriter.writeBioEntityToPropertyValues(data, annSrc, true, listener);

            reportSuccess("Update annotations from Annotation Source " + annSrc.getName() + " completed");
        } catch (AtlasAnnotationException e) {
            reportError(e);
        } catch (AnnotationSourceAccessException e) {
            reportError(new AtlasAnnotationException("Cannot read annotations from URL " + annSrc.getUrl() +
                    " for AnnSrc " + annSrc.getName(), e));
        } 
    }

    @Override
    public void updateMappings() {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " doesn't support method updateMappings ");
    }

}
