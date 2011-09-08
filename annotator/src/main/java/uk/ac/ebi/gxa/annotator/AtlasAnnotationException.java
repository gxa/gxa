package uk.ac.ebi.gxa.annotator;

/**
 * User: nsklyar
 * Date: 02/08/2011
 */
public class AtlasAnnotationException extends Exception{
    public AtlasAnnotationException(String s) {
        super(s);
    }

    public AtlasAnnotationException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public AtlasAnnotationException(Throwable throwable) {
        super(throwable);
    }
}
