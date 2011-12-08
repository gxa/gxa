package uk.ac.ebi.gxa.annotator.loader.annotationsrc;


import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.model.AnnotationSourceClass;

/**
 * User: nsklyar
 * Date: 10/11/2011
 */
class ConverterFactory {

    @Autowired
    private BioMartAnnotationSourceConverter bioMartAnnotationSourceConverter;

    @Autowired
    private FileBasedAnnotationSourceConverter fileBasedAnnotationSourceConverter;


    public <T extends AnnotationSource> AnnotationSourceConverter getConverter(Class<T> clazz) {
        if (clazz.equals(AnnotationSourceClass.BIOMART.getClazz())) {
            return bioMartAnnotationSourceConverter;
        } else if (AnnotationSourceClass.FILE.getClazz().equals(clazz)) {
            return fileBasedAnnotationSourceConverter;
        } else
        throw new IllegalArgumentException("There is no converter for class " + clazz);
    }
}
