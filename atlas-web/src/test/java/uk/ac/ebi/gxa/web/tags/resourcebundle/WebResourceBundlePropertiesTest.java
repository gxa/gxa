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

package uk.ac.ebi.gxa.web.tags.resourcebundle;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * @author Olga Melnichuk
 */
public class WebResourceBundlePropertiesTest {

    @Test
    public void defaultValuesTest() {
        WebResourceBundleProperties props = new WebResourceBundleProperties();
        assertFalse(props.isDebugOn());
        assertEquals(props.getBundlePath(WebResourceType.CSS), "");
        assertEquals(props.getBundlePath(WebResourceType.JS), "");
    }

    @Test
    public void loadFromNullTest() throws IOException {
        WebResourceBundleProperties props = new WebResourceBundleProperties();
        props.load((InputStream) null);
        assertFalse(props.isDebugOn());
        assertEquals(props.getBundlePath(WebResourceType.CSS), "");
        assertEquals(props.getBundlePath(WebResourceType.JS), "");
    }

    @Test
    public void loadFromPropertiesFileTest() throws IOException {
        WebResourceBundleProperties props = new WebResourceBundleProperties();
        props.load(WebResourceBundlePropertiesTest.class.getResourceAsStream("resourcebundle.properties"));
        assertTrue(props.isDebugOn());
        assertEquals(props.getBundlePath(WebResourceType.CSS), "/css");
        assertEquals(props.getBundlePath(WebResourceType.JS), "/js");
    }

    @Test
    public void loadFromPropertiesObjectTest() {
        Properties otherProperties = new Properties();
        otherProperties.setProperty("resourcebundle.debug", "true");
        otherProperties.setProperty("resourcebundle.path." + WebResourceType.CSS, "/styles");
        otherProperties.setProperty("resourcebundle.path." + WebResourceType.JS, "/scripts");

        WebResourceBundleProperties props = new WebResourceBundleProperties();
        props.load(otherProperties);
        assertTrue(props.isDebugOn());
        assertEquals(props.getBundlePath(WebResourceType.CSS), "/styles");
        assertEquals(props.getBundlePath(WebResourceType.JS), "/scripts");

        otherProperties.setProperty("resourcebundle.debug", "false");
        assertTrue(props.isDebugOn());
    }
}
