package uk.ac.ebi.gxa.index.builder.service;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 16-Dec-2009
 */
public class InvalidExperimentException extends RuntimeException {
    public InvalidExperimentException() {
        super();
    }

    public InvalidExperimentException(String message) {
        super(message);
    }

    public InvalidExperimentException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidExperimentException(Throwable cause) {
        super(cause);
    }
}
