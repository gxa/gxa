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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.Assert.*;
import static uk.ac.ebi.gxa.web.wro4j.tag.Wro4jTagProperties.aggregationNamePatternPropertyName;
import static uk.ac.ebi.gxa.web.wro4j.tag.Wro4jTagProperties.aggregationPathPropertyName;
import static uk.ac.ebi.gxa.web.wro4j.tag.Wro4jTagProperties.debugPropertyName;

/**
 * @author Olga Melnichuk
 */
public class AggregatedResourceNamePatternTest {

    @Test
    public void defaultPatternTest() {
        AggregatedResourceNamePattern pattern = new AggregatedResourceNamePattern(null, ResourceType.CSS);
        assertTrue(pattern.compile("test").matcher("test.css").matches());

        pattern = new AggregatedResourceNamePattern(null, ResourceType.JS);
        assertTrue(pattern.compile("test").matcher("test.js").matches());
    }

    @Test
    public void simplePatternTest() {
        String p = "@groupName@-345.@extension@";
        AggregatedResourceNamePattern pattern = new AggregatedResourceNamePattern(p, ResourceType.CSS);
        assertTrue(pattern.compile("test").matcher("test-345.css").matches());

        pattern = new AggregatedResourceNamePattern(p, ResourceType.JS);
        assertTrue(pattern.compile("test").matcher("test-345.js").matches());
    }

}
