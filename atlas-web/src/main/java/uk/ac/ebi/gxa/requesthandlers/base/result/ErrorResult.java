package uk.ac.ebi.gxa.requesthandlers.base.result;

import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;

/**
 * @author pashky
*/
public class ErrorResult {
    private String error;

    public ErrorResult(String error) {
        this.error = error;
    }

    public @RestOut
    String getError() {
        return error;
    }
}
