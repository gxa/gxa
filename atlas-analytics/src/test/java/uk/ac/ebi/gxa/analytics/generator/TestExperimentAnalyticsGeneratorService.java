package uk.ac.ebi.gxa.analytics.generator;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 15-Nov-2009
 */
public class TestExperimentAnalyticsGeneratorService extends TestCase {
    public void testGetRCodeFromResource() throws IOException {
        try {// open a stream to the resource
            InputStream in = getClass().getClassLoader().getResourceAsStream("R/analytics.R");

            // create a reader to read in code
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            String rCode = sb.toString();
//            System.out.println(rCode);
        }
        catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }
}
