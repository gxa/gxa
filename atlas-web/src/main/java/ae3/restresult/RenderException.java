package ae3.restresult;

/**
 * REST renderer process exception to be used in classes, implementing RestResultRenderer interace
 * @author pashky
 */
public class RenderException extends Exception {
    public RenderException() {

    }
    public RenderException(String message) {
        super(message);
    }
    public RenderException(String message, Throwable cause) {
        super(message, cause);
    }
    public RenderException(Throwable cause) {
        super(cause);
    }
}
