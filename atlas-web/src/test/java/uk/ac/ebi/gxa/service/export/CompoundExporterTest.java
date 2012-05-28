/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.service.export;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.gxa.dao.PropertyValueDAO;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.properties.ResourceFileStorage;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

/**
 * User: nsklyar
 * Date: 23/05/2012
 */
public class CompoundExporterTest {

    private final static Map<String, PropertyValue> CUT_TO_FULL_PROPERTY_VALUES = new HashMap<String, PropertyValue>();

    static {
        CUT_TO_FULL_PROPERTY_VALUES.put("TGF beta 1", new PropertyValue(null, Property.createProperty("compound"), "TGF beta 1 5 nanograms per milliliter"));
        CUT_TO_FULL_PROPERTY_VALUES.put("indole-3-acetic acid + dexamethasone", new PropertyValue(null, Property.createProperty("compound"), "indole-3-acetic acid + dexamethasone"));
        CUT_TO_FULL_PROPERTY_VALUES.put("triiodothyronine", new PropertyValue(null, Property.createProperty("compound"), "triiodothyronine 0.1 micrograms"));
        CUT_TO_FULL_PROPERTY_VALUES.put("siVFp(-992)m1", new PropertyValue(null, Property.createProperty("compound"), "siVFp(-992)m1"));
        CUT_TO_FULL_PROPERTY_VALUES.put("wortmannin", new PropertyValue(null, Property.createProperty("compound"), "wortmannin 0.00000001 molar"));
        CUT_TO_FULL_PROPERTY_VALUES.put("test 5 bla", new PropertyValue(null, Property.createProperty("compound"), "test 5 bla 0.00034 molar per day"));
        CUT_TO_FULL_PROPERTY_VALUES.put("wortmannin", new PropertyValue(null, Property.createProperty("compound"), "wortmannin "));
        CUT_TO_FULL_PROPERTY_VALUES.put("ATF", new PropertyValue(null, Property.createProperty("compound"), "ATF "));
        CUT_TO_FULL_PROPERTY_VALUES.put("atf", new PropertyValue(null, Property.createProperty("compound"), "atf 5 kg per day"));
    }

    private CompoundExporter service = new CompoundExporter();

    @Before
    public void setUp() throws Exception {
        service.setPropertyValueDAO(getPropertyValueDAO());
        service.setAtlasProperties(getAtlasProperties());
    }

    @Test
    public void testGenerateDataAsString() throws Exception {
        String result = "ATF\n" +
                "indole-3-acetic acid + dexamethasone\n" +
                "siVFp(-992)m1\n" +
                "test 5 bla\n" +
                "TGF beta 1\n" +
                "triiodothyronine\n" +
                "wortmannin";
        assertEquals(result, service.generateDataAsString());
    }

    @Test
    public void testCutOfDoseAndUnit() throws Exception {
        for (Map.Entry<String, PropertyValue> entry : CUT_TO_FULL_PROPERTY_VALUES.entrySet()) {
            assertEquals(entry.getKey(), service.cutOfDoseAndUnit(entry.getValue().getValue()));
        }

    }

    @Test
    public void testFilter() throws Exception {
        assertFalse(service.filter("compound 1"));
        assertFalse(service.filter("untreated control"));
        assertFalse(service.filter("untreated"));
        assertFalse(service.filter("N/A"));
        assertFalse(service.filter(""));
        assertFalse(service.filter("-"));
    }

    private PropertyValueDAO getPropertyValueDAO() {
        final PropertyValueDAO mock = EasyMock.createMock(PropertyValueDAO.class);
        EasyMock.expect(mock.findValuesForProperty("compound")).andReturn(new ArrayList<PropertyValue>(CUT_TO_FULL_PROPERTY_VALUES.values())).anyTimes();
        EasyMock.replay(mock);
        return mock;
    }

    private AtlasProperties getAtlasProperties() {
        ResourceFileStorage storage = new ResourceFileStorage();
        storage.setResourcePath("atlas.properties");
        AtlasProperties atlasProperties = new AtlasProperties();
        atlasProperties.setStorage(storage);
        return  atlasProperties;
    }

}
