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
import uk.ac.ebi.microarray.atlas.model.AssayProperty;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Date: 07/08/2012
 */
public class AssayDAOTest extends AtlasDAOTestCase {
    @Test
    public void testGetAssaysByProperty() throws Exception {

    }

    @Test
    public void testGetAssaysByPropertyValue() throws Exception {

    }

    @Test
    public void testGetAssayPropertiesByPropertyExactMatchTrue() throws Exception {
        List<AssayProperty> result = assayDAO.getAssayPropertiesByProperty("prop2", true);

        assertTrue(result.size() > 0);

        for (AssayProperty assayProperty : result) {
            assertEquals("prop2", assayProperty.getName());
        }

    }

    @Test
    public void testGetAssayPropertiesByPropertyExactMatchTrueNoMatchFound() {
        List<AssayProperty> result;
        result = assayDAO.getAssayPropertiesByProperty("prop", true);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetAssayPropertiesByPropertyExactMatchFalse() throws Exception {
        List<AssayProperty> result = assayDAO.getAssayPropertiesByProperty("rop", false);
        assertTrue(result.size() > 0);

        for (AssayProperty assayProperty : result) {
            assertTrue(assayProperty.getName().contains("rop"));
        }

        result = assayDAO.getAssayPropertiesByProperty("not in db", false);
        assertTrue(result.isEmpty());
    }


    @Test
    public void testGetAssayPropertiesByPropertyValueExactMatchTrue() throws Exception {
        final List<AssayProperty> result = assayDAO.getAssayPropertiesByPropertyValue("prop2", "value002", true);
        assertEquals(2, result.size());

        //List contains different AssayProperties
        assertFalse(result.get(0).getId().equals(result.get(1).getId()));

        for (AssayProperty assayProperty : result) {
            assertEquals("prop2", assayProperty.getName());
            assertEquals("value002", assayProperty.getValue());
        }

    }

    @Test
    public void testGetAssayPropertiesByPropertyValueExactMatchTrueNoMatch() throws Exception {
        final List<AssayProperty> result = assayDAO.getAssayPropertiesByPropertyValue("prop2", "mo match", true);

        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetAssayPropertiesByPropertyValueExactMatchFalse() throws Exception {
        final List<AssayProperty> result = assayDAO.getAssayPropertiesByPropertyValue("prop2", "value", false);
        
        assertTrue(result.size() > 0);

        for (AssayProperty assayProperty : result) {
            assertEquals("prop2", assayProperty.getName());
            assertTrue(assayProperty.getValue().contains("value"));
        }
    }

    @Test
    public void testGetAssayPropertiesByOntologyTerm() throws Exception {

    }
}
