/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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
package uk.ac.ebi.gxa.properties;

import static junit.framework.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Arrays;

/**
 * @author pashky
 */
public class ResourceFileStorageTest {

    ResourceFileStorage storage;

    @Before
    public void setup() {
        storage = new ResourceFileStorage();
        storage.setResourcePath("atlas.properties");
    }

    @Test
    public void test_getProperty() {
        assertEquals("time,individual,age,ALL", storage.getProperty("atlas.ignore.efs.facet"));
    }

    @Test
    public void test_setProperty() {
        // new property
        storage.setProperty("dummy", "hoppa");
        assertEquals("hoppa", storage.getProperty("dummy"));

        // existing property
        storage.setProperty("atlas.ignore.efs.facet", "bugoga");
        assertEquals("bugoga", storage.getProperty("atlas.ignore.efs.facet"));
    }

    @Test
    public void test_isWritePersistent() {
        assertFalse(storage.isWritePersistent());
    }

    @Test
    public void test_reload() {
        storage.setProperty("dummy", "hoppa");
        storage.reload();
        assertNull(storage.getProperty("dummy"));
    }

    @Test
    public void test_getAvailablePropertyNames() {
        Collection<String> propnames = storage.getAvailablePropertyNames();
        assertNotNull(propnames);
        assertFalse(propnames.isEmpty());
        assertTrue(propnames.containsAll(Arrays.asList("atlas.feedback.from.address,atlas.drilldowns.mingenes,atlas.dump.geneidentifiers.filename,atlas.gene.autocomplete.ids.limit,atlas.gene.drilldowns,atlas.query.pagesize,atlas.gene.autocomplete.names.limit,atlas.dump.geneidentifiers,atlas.query.expsPerGene,atlas.gene.list.cache.autogenerate,atlas.feedback.subject,atlas.data.release,atlas.gene.autocomplete.names,atlas.gene.autocomplete.ids,atlas.feedback.smtp.host,atlas.gene.autocomplete.descs,atlas.query.listsize,atlas.feedback.to.address,atlas.dump.ebeye.filename".split(","))));
    }
}
