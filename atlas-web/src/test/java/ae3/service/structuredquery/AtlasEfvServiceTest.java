package ae3.service.structuredquery;

import ae3.service.AtlasStatisticsQueryService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.gxa.dao.PropertyDAO;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.properties.ResourceFileStorage;
import uk.ac.ebi.gxa.statistics.EfvAttribute;
import uk.ac.ebi.gxa.statistics.StatisticsType;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.easymock.EasyMock.*;

import static org.junit.Assert.*;

/**
 * @author Robert Petryszak
 */
@Transactional
public class AtlasEfvServiceTest {

    private final static AtlasEfvService efvService = new AtlasEfvService();

    @Before
    public void createService() throws Exception {

        ResourceFileStorage storage = new ResourceFileStorage();
        storage.setResourcePath("atlas.properties");
        AtlasProperties atlasProperties = new AtlasProperties();
        atlasProperties.setStorage(storage);

        Property prop1 = Property.createProperty("biopsy_tissue");
        Property prop2 = Property.createProperty("cell_type");

        PropertyDAO propertyDAO = createMock(PropertyDAO.class);
        expect(propertyDAO.getAll()).andReturn(Arrays.asList(prop1, prop2)).anyTimes();
        expect(propertyDAO.getByName("biopsy_tissue")).andReturn(prop1).anyTimes();
        expect(propertyDAO.getByName("cell_type")).andReturn(prop2).anyTimes();
        expect(propertyDAO.getValues(prop1)).andReturn(Collections.singletonList(new PropertyValue(1L, prop1, "lung"))).anyTimes();
        expect(propertyDAO.getValues(prop2)).andReturn(Collections.singletonList(new PropertyValue(1L, prop2, "lung"))).anyTimes();
        replay(propertyDAO);

        AtlasStatisticsQueryService atlasStatisticsQueryService = createMock(AtlasStatisticsQueryService.class);
        expect(atlasStatisticsQueryService.getBioEntityCountForEfvAttribute(new EfvAttribute("biopsy_tissue", "lung"), StatisticsType.UP_DOWN)).andReturn(1).anyTimes();
        expect(atlasStatisticsQueryService.getBioEntityCountForEfvAttribute(new EfvAttribute("cell_type", "lung"), StatisticsType.UP_DOWN)).andReturn(2).anyTimes();
        replay(atlasStatisticsQueryService);

        efvService.setAtlasStatisticsQueryService(atlasStatisticsQueryService);
        efvService.setAtlasProperties(atlasProperties);
        efvService.setPropertyDAO(propertyDAO);
    }

    @Test
    public void testAutoCompleteValues() {
        Collection<AutoCompleteItem> autocompleteItems = efvService.autoCompleteValues(null, "lung", Integer.MAX_VALUE);
        assertEquals(1, autocompleteItems.size());
        assertTrue(autocompleteItems.iterator().next().getCount() == 3l);
    }
}
