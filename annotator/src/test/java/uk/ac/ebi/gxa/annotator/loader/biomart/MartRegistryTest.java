package uk.ac.ebi.gxa.annotator.loader.biomart;

import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Olga Melnichuk
 * @version 1/14/12 12:29 PM
 */
public class MartRegistryTest {

    @Test
    public void testMartRegistryParser() throws IOException, SAXException, ParserConfigurationException {
        MartRegistry registry = MartRegistry.parse(MartRegistryTest.class.getResource("mart-registry.xml").openStream());

        MartRegistry.MartUrlLocation loc = registry.find("ensembl_mart_65");
        assertNotNull(loc);
        assertEquals("ensembl_mart_65", loc.getDatabase());
        assertEquals("ensembl", loc.getName());
        assertEquals("default", loc.getVirtualSchema());
    }
}
