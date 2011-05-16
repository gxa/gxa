package uk.ac.ebi.gxa.dao;

/**
 * User: nsklyar
 * Date: 05/05/2011
 */
public class AtlasDataAccessException extends Exception{
    public AtlasDataAccessException() {
        super();
    }

    public AtlasDataAccessException(String s) {
        super(s);
    }

    public AtlasDataAccessException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public AtlasDataAccessException(Throwable throwable) {
        super(throwable);    
    }
}
