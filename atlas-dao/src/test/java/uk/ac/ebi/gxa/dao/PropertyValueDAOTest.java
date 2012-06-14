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

package uk.ac.ebi.gxa.dao;

import org.junit.Test;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

import java.util.Collection;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * User: nsklyar
 * Date: 23/05/2012
 */
public class PropertyValueDAOTest extends AtlasDAOTestCase {

    @Test
    public void testFind() throws Exception {
        //ToDo: implement!!!
    }

    @Test
    public void testDelete() throws Exception {
        //ToDo: implement!!!
    }

    @Test
    public void testGetOrCreatePropertyValue() throws Exception {
        //ToDo: implement!!!
    }

    @Test
    public void testGetUnusedPropertyValues() throws Exception {
        //ToDo: implement!!!
    }

    @Test
    public void testRemoveUnusedPropertyValues() throws Exception {
        //ToDo: implement!!!
    }

    @Test
    public void testFindValuesForProperty() throws Exception {
        final Collection<PropertyValue> values = propertyValueDAO.findValuesForProperty("cell_type");
        assertEquals(2, values.size());
        assertTrue(values.contains(new PropertyValue(null, Property.createProperty("cell_type"), "microglial cell")));
    }
}
