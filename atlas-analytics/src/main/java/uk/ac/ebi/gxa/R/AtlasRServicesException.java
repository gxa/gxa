package uk.ac.ebi.gxa.R;

/**
 * An exception thrown whenever the R framework fails to create, reuse or recycle an RService
 *
 * @author Tony Burdett
 * @date 17-Nov-2009
 */
public class AtlasRServicesException extends Exception {
    public AtlasRServicesException() {
        super();
    }

    public AtlasRServicesException(String s) {
        super(s);
    }

    public AtlasRServicesException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public AtlasRServicesException(Throwable throwable) {
        super(throwable);
    }
}
