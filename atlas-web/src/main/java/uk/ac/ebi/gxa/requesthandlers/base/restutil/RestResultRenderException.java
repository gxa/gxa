package uk.ac.ebi.gxa.requesthandlers.base.restutil;

/**
 * REST renderer process exception to be used in classes, implementing RestResultRenderer interace
 * @author pashky
 */
public class RestResultRenderException extends Exception {
    public RestResultRenderException() {

    }
    public RestResultRenderException(String message) {
        super(message);
    }
    public RestResultRenderException(String message, Throwable cause) {
        super(message, cause);
    }
    public RestResultRenderException(Throwable cause) {
        super(cause);
    }
}
