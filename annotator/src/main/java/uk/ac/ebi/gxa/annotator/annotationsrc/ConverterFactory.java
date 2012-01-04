package uk.ac.ebi.gxa.annotator.annotationsrc;


import org.springframework.beans.factory.annotation.Autowired;

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
