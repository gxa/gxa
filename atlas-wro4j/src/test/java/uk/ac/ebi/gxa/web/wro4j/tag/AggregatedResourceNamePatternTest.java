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

/**
 * @author Olga Melnichuk
 */
public class AggregatedResourceNamePatternTest {

    @Test
    public void defaultPatternTest() {
        AggregatedResourceNamePattern pattern = new AggregatedResourceNamePattern(null, ResourceType.CSS);
        assertEquals("test\\.css", pattern.pattern("test"));

        pattern = new AggregatedResourceNamePattern(null, ResourceType.JS);
        assertEquals("test\\.js", pattern.pattern("test"));
    }

    @Test
    public void simplePatternTest() {
        String p = "@groupName@-345.@extension@";
        AggregatedResourceNamePattern pattern = new AggregatedResourceNamePattern(p, ResourceType.CSS);
        assertEquals("test-345\\.css", pattern.pattern("test"));

        pattern = new AggregatedResourceNamePattern(p, ResourceType.JS);
        assertEquals("test-345\\.js", pattern.pattern("test"));
    }

    @Test
    public void invalidTypeTest() {
        try {
            new AggregatedResourceNamePattern(null, null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected outcome
        }
    }
}
