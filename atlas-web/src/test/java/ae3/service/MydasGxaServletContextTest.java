package ae3.service;

import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.properties.ResourceFileStorage;

import java.io.IOException;
import java.io.InputStream;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: Dec 10, 2010
 * Time: 9:09:22 AM
 * To change this template use File | Settings | File Templates.
 */
public class MydasGxaServletContextTest {
    private static final String s = "This is a test string with ${atlas.dasbase} placeholder in it.";
    private static final String result = "This is a test string with http://www.ebi.ac.uk/gxa placeholder in it.";
    AtlasProperties props;
    MydasGxaServletContext sc;

    @Before
    public void setup() {
        ResourceFileStorage storage = new ResourceFileStorage();
        storage.setResourcePath("atlas.properties");

        props = new AtlasProperties();
        props.setStorage(storage);

        sc = new MydasGxaServletContext(null, props);
    }

    @Test
    public void testStringToFromInputStreamConversion() throws IOException {
        InputStream is = sc.convertStringToInputStream(s);
        assertNotNull(is);
        String result = sc.convertInputStreamToString(is);
        assertNotNull(result);
        assertTrue("'" + result + "' after conversion not equal to '" + s + "' before conversion", result.equals(s));
    }


    @Test
    public void testReplaceRegex() throws IOException {
        InputStream is = sc.convertStringToInputStream(s);
        InputStream resultIS = sc.replaceRegex(is, sc.DASBASE_PLACEHOLDER_REGEX, props.getDasBase());
        assertNotNull(resultIS);
        String resultS = sc.convertInputStreamToString(resultIS);
        assertTrue("'" + resultS + "' after conversion not equal to expected result'" + result + "'", resultS.equals(result));

    }

}
