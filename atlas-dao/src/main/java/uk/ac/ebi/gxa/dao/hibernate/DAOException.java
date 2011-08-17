package uk.ac.ebi.gxa.dao.hibernate;

/**
 * Thrown when DAO's getByName() method returns no results
 *
 * @author Robert Petryszak
 *         Date: Aug 17, 2011
 */
public class DAOException extends Exception {
    public DAOException(String message) {
        super(message);
    }
}
