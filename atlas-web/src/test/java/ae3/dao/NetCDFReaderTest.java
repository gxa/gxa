package ae3.dao;

import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import ae3.model.ExperimentalData;
import ae3.model.Assay;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

/**
 * @author pashky
 */
public class NetCDFReaderTest {
    @Test
    public void testLoadExperiment() throws IOException, URISyntaxException {

        ExperimentalData expData = NetCDFReader.loadExperiment(getTestNCPath(), 645932669);
        assertNotNull(expData);
        assertEquals(1, expData.getArrayDesigns().size());

        assertEquals(0, expData.getExpressionsForGene(123456).size());
        assertEquals(expData.getAssays().size(), expData.getExpressionsForGene(281616865).size());
    }

    @Test
    public void testMultiArrayDesign() throws IOException, URISyntaxException {

        ExperimentalData expData = NetCDFReader.loadExperiment(getTestNCPath(), 824359618);
        assertNotNull(expData);
        assertEquals(2, expData.getArrayDesigns().size());
        
        assertEquals(0, expData.getExpressionsForGene(123456).size());
        assertTrue(expData.getAssays().size() > expData.getExpressionsForGene(169991224).size());
        assertTrue(expData.getAssays().size() > expData.getExpressionsForGene(175824562).size());
    }

    private String getTestNCPath() throws URISyntaxException {
        // won't work for JARs, networks and stuff, but so far so good...
        return new File(getClass().getClassLoader().getResource("dummy.txt").toURI()).getParentFile().getAbsolutePath();
    }
}
