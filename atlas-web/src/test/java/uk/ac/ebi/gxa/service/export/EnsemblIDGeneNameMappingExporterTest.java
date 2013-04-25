package uk.ac.ebi.gxa.service.export;

import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityDAO;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EnsemblIDGeneNameMappingExporterTest {

    private EnsemblIDGeneNameMappingExporter exporter;

    @Before
    public void setUp() {
        BioEntityDAO daoMock = mock(BioEntityDAO.class);
        Map<String, String> mapping = new HashMap<String, String>();
        mapping.put("id1", "gene1");
        mapping.put("id2", "gene2");
        when(daoMock.getGeneNames(anyString())).thenReturn(mapping);

        exporter = new EnsemblIDGeneNameMappingExporter();
        exporter.setBioEntityDAO(daoMock);
    }

    @Test
    public void testGenerateDataAsString() throws Exception {
        String result = exporter.generateDataAsString("organism1");
        assertTrue(result.contains("\"id2\":\"gene2\""));
        assertTrue(result.contains("\"id1\":\"gene1\""));

    }


}
