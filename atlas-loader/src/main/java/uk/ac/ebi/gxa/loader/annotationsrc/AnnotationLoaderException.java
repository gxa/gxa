package uk.ac.ebi.gxa.loader.annotationsrc;

/**
 * User: nsklyar
 * Date: 06/07/2011
 */
public class AnnotationLoaderException extends Exception{
    public AnnotationLoaderException(String s) {
        super(s);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public AnnotationLoaderException(String s, Throwable throwable) {
        super(s, throwable);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public AnnotationLoaderException(Throwable throwable) {
        super(throwable);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
