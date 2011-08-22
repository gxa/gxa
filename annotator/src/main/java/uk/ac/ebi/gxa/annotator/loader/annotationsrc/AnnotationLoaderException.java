package uk.ac.ebi.gxa.annotator.loader.annotationsrc;

/**
 * User: nsklyar
 * Date: 06/07/2011
 */
public class AnnotationLoaderException extends Exception{
    public AnnotationLoaderException(String s) {
        super(s);
    }

    public AnnotationLoaderException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public AnnotationLoaderException(Throwable throwable) {
        super(throwable);
    }
}
