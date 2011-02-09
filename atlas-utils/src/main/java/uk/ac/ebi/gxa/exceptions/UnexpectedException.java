package uk.ac.ebi.gxa.exceptions;

/**
 * An exception which generally shouldn't have happened.
 */
public class UnexpectedException extends RuntimeException {
    UnexpectedException(String message) {
        super(message);
    }

    UnexpectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
