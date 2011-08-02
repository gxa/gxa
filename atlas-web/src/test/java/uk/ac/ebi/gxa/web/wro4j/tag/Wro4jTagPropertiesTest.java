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
public class Wro4jTagPropertiesTest {

    @Test
    public void defaultValuesTest() {
        Wro4jTagProperties props = new Wro4jTagProperties();
        assertFalse(props.isDebugOn());
        assertEquals(props.getAggregationPath(WebResourceType.CSS), "");
        assertEquals(props.getAggregationPath(WebResourceType.JS), "");
    }

    @Test
    public void loadFromNullTest() throws IOException {
        Wro4jTagProperties props = new Wro4jTagProperties();
        props.load((InputStream) null);
        assertFalse(props.isDebugOn());
        assertEquals(props.getAggregationPath(WebResourceType.CSS), "");
        assertEquals(props.getAggregationPath(WebResourceType.JS), "");
    }

    @Test
    public void loadFromPropertiesFileTest() throws IOException {
        Wro4jTagProperties props = new Wro4jTagProperties();
        props.load(Wro4jTagPropertiesTest.class.getResourceAsStream("wro4j-tag.properties"));
        assertTrue(props.isDebugOn());
        assertEquals(props.getAggregationPath(WebResourceType.CSS), "/css");
        assertEquals(props.getAggregationPath(WebResourceType.JS), "/js");
        assertTrue(props.getAggregationNamePattern("all", WebResourceType.CSS).matcher("all-min.css").matches());
    }

    @Test
    public void loadFromPropertiesObjectTest() {
        Properties otherProperties = new Properties();
        otherProperties.setProperty(debugPropertyName(), "true");
        otherProperties.setProperty(aggregationPathPropertyName(WebResourceType.CSS), "/styles");
        otherProperties.setProperty(aggregationPathPropertyName(WebResourceType.JS), "/scripts");
        otherProperties.setProperty(aggregationNamePatternPropertyName(), "@groupName@-123\\.@extension@");

        Wro4jTagProperties props = new Wro4jTagProperties();
        props.load(otherProperties);
        assertTrue(props.isDebugOn());
        assertEquals(props.getAggregationPath(WebResourceType.CSS), "/styles");
        assertEquals(props.getAggregationPath(WebResourceType.JS), "/scripts");
        assertTrue(props.getAggregationNamePattern("all", WebResourceType.CSS).matcher("all-123.css").matches());

        otherProperties.setProperty(debugPropertyName(), "false");
        assertTrue(props.isDebugOn());
    }
}
