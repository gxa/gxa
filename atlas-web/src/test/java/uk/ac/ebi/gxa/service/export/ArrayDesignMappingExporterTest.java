package uk.ac.ebi.gxa.service.export;

import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.gxa.dao.ArrayDesignDAO;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ArrayDesignMappingExporterTest {

    private ArrayDesignMappingExporter exporter;

    @Before
    public void setUp() {
        ArrayDesignDAO arrayDesignDAOMock = mock(ArrayDesignDAO.class);
        Map<String, String> mapping = new HashMap<String, String>();
        mapping.put("de1", "gene1");
        mapping.put("de2", "gene2");
        when(arrayDesignDAOMock.getDesignElementGeneAccMapping(anyString())).thenReturn(mapping);

        exporter = new ArrayDesignMappingExporter();
        exporter.setArrayDesignDAO(arrayDesignDAOMock);
    }

    @Test
    public void testGenerateDataAsString() throws Exception {
        String result = exporter.generateDataAsString("ArrayDesign1");
        assertEquals("{\"de2\":\"gene2\",\"de1\":\"gene1\"}", result);

    }


}
