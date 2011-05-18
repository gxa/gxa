package uk.ac.ebi.gxa.loader.bioentity;

/**
 * User: nsklyar
 * Date: 05/05/2011
 */
public class BioMartAccessException extends Exception{
    public BioMartAccessException() {
        super();
    }

    public BioMartAccessException(String s) {
        super(s);
    }

    public BioMartAccessException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public BioMartAccessException(Throwable throwable) {
        super(throwable);    
    }
}
