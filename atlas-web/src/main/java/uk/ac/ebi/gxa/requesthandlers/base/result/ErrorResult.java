package uk.ac.ebi.gxa.requesthandlers.base.result;

import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;

import java.io.StringWriter;
import java.io.PrintWriter;

import org.apache.commons.lang.StringUtils;

/**
 * @author pashky
*/
public class ErrorResult {
    private String error;

    public ErrorResult(String error) {
        this.error = error;
    }

    private static String exceptionToString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        e.printStackTrace(pw);
        pw.flush();
        sw.flush();
        String[] strings = sw.getBuffer().toString().split("(\r?\n)+");
        return StringUtils.join(strings, '\n', 0, strings.length > 10 ? 10 : strings.length)
                + (strings.length > 10 ? "\n..." : "");
    }


    public ErrorResult(Exception e) {
        this.error = "Exception occured: " + exceptionToString(e);
    }

    public @RestOut
    String getError() {
        return error;
    }
}
