package uk.ac.ebi.gxa.model;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Oct 27, 2009
 * Time: 2:16:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class GxaException extends Exception{
    public GxaException(String message){
        super(message);
    }

    public GxaException(Throwable e) {
        super(e);
    }

    public GxaException(String message, Throwable e) {
        super(message, e);
    }
}
