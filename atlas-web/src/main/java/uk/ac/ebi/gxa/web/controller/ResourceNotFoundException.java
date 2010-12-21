package uk.ac.ebi.gxa.web.controller;

/**
 * Thrown when the asked resource (e.g. experiment, gene or other data) not found.
 *
 * @author Olga Melnichuk
 *         Date: Dec 1, 2010
 */
public class ResourceNotFoundException extends Exception {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
