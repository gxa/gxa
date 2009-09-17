package ae3.servlet.structuredquery.result;

import ae3.restresult.RestOut;

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
