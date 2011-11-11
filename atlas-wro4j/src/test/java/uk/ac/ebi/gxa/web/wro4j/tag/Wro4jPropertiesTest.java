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
package uk.ac.ebi.gxa.web.wro4j.tag;

import org.junit.Test;
import ro.isdc.wro.model.resource.ResourceType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class Wro4jPropertiesTest {
    @Test
    public void testProperties() {
        final Wro4jTagProperties properties = new Wro4jTagProperties();
        assertEquals("test value for wro4j.tag.aggregation.path.JS",
                properties.getResourcePath(ResourceType.JS));
        assertEquals("test value for wro4j.tag.aggregation.path.CSS",
                properties.getResourcePath(ResourceType.CSS));
        assertEquals(new BundleNameTemplate("test value for wro4j.tag.aggregation.name.pattern"),
                properties.getNameTemplate());
        assertEquals(true, properties.isDebugOn());
    }

    @Test
    public void testInvalidConfigPath() {
        try {
            new Wro4jTagProperties("somewhere");
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            // expected
        }
    }
}
