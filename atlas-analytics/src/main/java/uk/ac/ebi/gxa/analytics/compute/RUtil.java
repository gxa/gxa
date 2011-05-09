package uk.ac.ebi.gxa.analytics.compute;

import com.google.common.io.Resources;

import java.io.IOException;
import java.nio.charset.Charset;

public class RUtil {
    public static String getRCodeFromResource(String resourcePath) throws ComputeException {
        try {
            return Resources.toString(RUtil.class.getClassLoader().getResource(resourcePath), Charset.defaultCharset());
        } catch (IOException e) {
            throw new ComputeException("Error while reading in R code from " + resourcePath, e);
        }
    }
}
