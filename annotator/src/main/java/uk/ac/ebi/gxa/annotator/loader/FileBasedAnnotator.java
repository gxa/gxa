package uk.ac.ebi.gxa.annotator.loader;

import uk.ac.ebi.gxa.annotator.AnnotationException;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityAnnotationData;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityAnnotationDataBuilder;
import uk.ac.ebi.gxa.annotator.model.GeneSigAnnotationSource;
import uk.ac.ebi.gxa.utils.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

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

            reportProgress("Reading properties from Annotation Source " + annSrc.getName());
            File contentAsFile = FileUtil.tempFile("genesig.tmp");
            URLContentLoader.getContentAsFile(annSrc.getUrl(), contentAsFile);

            reportProgress("Parsing properties from Annotation Source " + annSrc.getName());
            parser.parsePropertyValues(attributesHandler.getBioEntityProperties(),
                    new FileInputStream(contentAsFile), true);

            final BioEntityAnnotationData data = parser.getData();

            beDataWriter.writePropertyValues(data.getPropertyValues(), listener);
            beDataWriter.writeBioEntityToPropertyValues(data, annSrc, true, listener);

            reportSuccess("Update annotations from Annotation Source " + annSrc.getName() + " completed");
            contentAsFile.delete();
        } catch (AnnotationException e) {
            reportError(e);

        } catch (FileNotFoundException e) {
            reportError(new AnnotationException("Cannot read annotations from URL " + annSrc.getUrl() +
                    " for AnnSrc " + annSrc.getName(), e));
        }
    }

    @Override
    public void updateMappings() {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " doesn't support method updateMappings ");
    }

}
