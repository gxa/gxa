package uk.ac.ebi.gxa.annotator.loader.listner;

import java.util.List;

/**
 * User: nsklyar
 * Date: 01/08/2011
 */
public class AnnotationLoaderEvent {
    private List<Throwable> errors;

    /**
     * An AnnotationLoaderEvent that represents a completion following a failure.
     * Clients should supply the error that resulted in the failure.
     *
     * @param errors the list of errors that occurred, causing the fail
     */
    public AnnotationLoaderEvent(List<Throwable> errors) {
        this.errors = errors;
    }

    public List<Throwable> getErrors() {
        return errors;
    }

}
