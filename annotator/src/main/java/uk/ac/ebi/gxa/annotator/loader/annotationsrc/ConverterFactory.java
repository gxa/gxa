package uk.ac.ebi.gxa.annotator.loader.annotationsrc;


import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.model.AnnotationSourceType;

/**
 * User: nsklyar
 * Date: 10/11/2011
 */
public class ConverterFactory {

    @Autowired
    private BioMartAnnotationSourceConverter bioMartAnnotationSourceConverter;

    @Autowired
    private FileBasedAnnotationSourceConverter fileBasedAnnotationSourceConverter;

    public BioMartAnnotationSourceConverter getBioMartAnnotationSourceConverter() {
        return bioMartAnnotationSourceConverter;
    }

    public FileBasedAnnotationSourceConverter getFileBasedAnnotationSourceConverter() {
        return fileBasedAnnotationSourceConverter;
    }
}
