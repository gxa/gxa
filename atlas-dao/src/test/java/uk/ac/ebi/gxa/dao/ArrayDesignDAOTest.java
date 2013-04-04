/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class ArrayDesignDAOTest extends AtlasDAOTestCase {

    @Test
    public void testGetDesignElementGeneAccMapping() throws Exception {
        Map<String,String> designElementGeneAccMapping = arrayDesignDAO.getDesignElementGeneAccMapping("A-AFFY-45");
        assertEquals(1, designElementGeneAccMapping.size());
        assertTrue(designElementGeneAccMapping.containsKey("acc1"));
        assertTrue(designElementGeneAccMapping.containsValue("ENSMUSG00000020275"));
    }

    @Test
    public void testFilterToKeepUniqueMappings() throws Exception {
        Multimap<String, String> map = ArrayListMultimap.create();
        map.put("de1", "g1");
        map.put("de1", "g2");
        map.put("de3", "g3");

        Map<String, String> result = arrayDesignDAO.filterToKeepUniqueMappings(map);
        assertThat(result.size(), is(1));
        assertThat(result.containsKey("de3"), is(true));
        assertThat(result.containsValue("g3"), is(true));
    }
}
