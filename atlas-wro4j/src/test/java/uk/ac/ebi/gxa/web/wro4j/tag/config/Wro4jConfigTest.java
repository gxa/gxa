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

package uk.ac.ebi.gxa.web.wro4j.tag.config;

import org.junit.Test;
import uk.ac.ebi.gxa.web.wro4j.tag.WebResource;
import uk.ac.ebi.gxa.web.wro4j.tag.WebResourceType;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Olga Melnichuk
 */
public class Wro4jConfigTest {

    @Test
    public void resourceBundleContentsTest() throws Wro4jConfigException {
        Wro4jConfig config = new Wro4jConfig();
        config.load(Wro4jConfigTest.class.getResourceAsStream("wro.xml"));

        assertResourceBundleContains(config, "css-only-resources", Arrays.asList("/style-1.css", "/style-2.css"));
        assertResourceBundleContains(config, "js-only-resources", Arrays.asList("/script-1.js", "/script-2.js"));
        assertResourceBundleContains(config, "mixed-resources-1", Arrays.asList("/script-1.js", "/script-2.js", "/style-1.css", "/style-2.css"));
        assertResourceBundleContains(config, "mixed-resources-2", Arrays.asList("/script-1.js", "/script-2.js", "/style-1.css", "/style-2.css"));
        assertResourceBundleContains(config, "mixed-resources-3", Arrays.asList("/script-1.js", "/script-2.js", "/script-3.js", "/style-1.css", "/style-2.css", "/style-3.css"));

        assertResourceBundleContains(config, "mixed-resources-1", Arrays.asList("/script-1.js", "/script-2.js"), WebResourceType.JS);
        assertResourceBundleContains(config, "mixed-resources-1", Arrays.asList("/style-1.css", "/style-2.css"), WebResourceType.CSS);

        assertTrue(config.hasResources("css-only-resources", WebResourceType.CSS));
        assertFalse(config.hasResources("css-only-resources", WebResourceType.JS));

        assertTrue(config.hasResources("js-only-resources", WebResourceType.JS));
        assertFalse(config.hasResources("js-only-resources", WebResourceType.CSS));
    }

    private void assertResourceBundleContains(Wro4jConfig config, String bundleName, List<String> contents) throws Wro4jConfigException {
        assertResourceBundleContains(config, bundleName, contents, WebResourceType.values());
    }

    private void assertResourceBundleContains(Wro4jConfig config, String bundleName, List<String> contents,WebResourceType... types) throws Wro4jConfigException {
        Collection<WebResource> resources = config.getResources(bundleName, Arrays.asList(types));
        assertEquals(contents.size(), resources.size());
        for(WebResource res : resources) {
            assertTrue(contents.contains(res.getSrc()));
        }
    }

    @Test
    public void cycleGroupReferencesTest() throws Wro4jConfigException {
        Wro4jConfig config = new Wro4jConfig();
        config.load(Wro4jConfigTest.class.getResourceAsStream("wro-with-cycle-refs.xml"));

        assertNotConfigured(config, "resources-4");

        assertCycleReference(config, "resources-1");
        assertCycleReference(config, "resources-2");
        assertCycleReference(config, "resources-3");
    }

    private void assertCycleReference(Wro4jConfig config, String groupName) {
        try {
            config.getResources(groupName, Arrays.asList(WebResourceType.values()));
            fail();
        } catch (Wro4jConfigException e) {
            //OK
        }
    }

    private void assertNotConfigured(Wro4jConfig config, String groupName) {
        try {
            config.hasResources(groupName, WebResourceType.CSS);
            fail();
        } catch (Wro4jConfigException e) {
            // OK
        }
    }
}
