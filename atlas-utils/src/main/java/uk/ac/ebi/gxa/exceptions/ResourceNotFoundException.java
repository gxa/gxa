package uk.ac.ebi.gxa.exceptions;

/**
 * Thrown when the asked resource (e.g. experiment, gene or other data) not found.
 */
public class ResourceNotFoundException extends Exception {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceNotFoundException(Throwable cause) {
        super(cause.getMessage(), cause);
    }

}