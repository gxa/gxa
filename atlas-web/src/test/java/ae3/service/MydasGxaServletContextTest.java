package ae3.service;

import com.google.common.io.CharStreams;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.properties.ResourceFileStorage;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * JUnit tests for MydasGxaServletContext
 */
public class MydasGxaServletContextTest {
    private static final String SOURCE = "This is a test string with ${atlas.dasbase} placeholder in it.";
    private static final String RESULT = "This is a test string with http://www.ebi.ac.uk/gxa placeholder in it.";
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
        InputStream is = sc.convertStringToInputStream(SOURCE);
        assertNotNull(is);
        String result = CharStreams.toString(new InputStreamReader(is));
        assertNotNull(result);
        assertEquals("String-InputStream-Reader-String conversion is broken", SOURCE, result);
    }

    @Test
    public void testReplaceRegex() throws IOException {
        InputStream is = sc.convertStringToInputStream(SOURCE);
        InputStream result = sc.filter(is);
        assertNotNull(result);
        assertEquals("Conversion results are invalid", RESULT, CharStreams.toString(new InputStreamReader(result)));
    }
}
