package uk.ac.ebi.gxa.dao.exceptions;

/**
 * Exception thrown when the object was requested by a "strong" ID (e.g. surrogate ID, accession, or identifier)
 * and was not found in the database.
 * <p/>
 * Please do not confuse with {@link org.hibernate.ObjectNotFoundException} which is an unrecoverable situation
 * and therefore invalidates the session. This one isn't and doesn't.
 */
public class RecordNotFoundException extends DataAccessException {
    public RecordNotFoundException() {
    }

    public RecordNotFoundException(String message) {
        super(message);
    }

    public RecordNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public RecordNotFoundException(Throwable cause) {
        super(cause);
    }
}
