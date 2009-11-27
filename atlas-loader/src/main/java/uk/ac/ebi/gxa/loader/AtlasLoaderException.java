package uk.ac.ebi.gxa.loader;

/**
 * An exception whenever a problem occurs in an {@link AtlasLoader}
 *
 * @author Tony Burdett
 * @date 27-Nov-2009
 */
public class AtlasLoaderException extends Exception {
    public AtlasLoaderException() {
        super();
    }

    public AtlasLoaderException(String message) {
        super(message);
    }

    public AtlasLoaderException(String message, Throwable cause) {
        super(message, cause);
    }

    public AtlasLoaderException(Throwable cause) {
        super(cause);
    }
}
