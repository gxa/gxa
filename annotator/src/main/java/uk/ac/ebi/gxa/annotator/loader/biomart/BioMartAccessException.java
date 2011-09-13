package uk.ac.ebi.gxa.annotator.loader.biomart;

/**
 * User: nsklyar
 * Date: 05/05/2011
 */
public class BioMartAccessException extends Exception{

    public BioMartAccessException(String s) {
        super(s);
    }

    public BioMartAccessException(String s, Throwable throwable) {
        super(s, throwable);
    }

}
