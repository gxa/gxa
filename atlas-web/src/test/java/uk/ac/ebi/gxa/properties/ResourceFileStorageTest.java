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
        assertEquals("time,individual,age,ALL", storage.getProperty("atlas.facet.ignore.efs"));
    }

    @Test
    public void test_setProperty() {
        // new property
        storage.setProperty("dummy", "hoppa");
        assertEquals("hoppa", storage.getProperty("dummy"));

        // existing property
        storage.setProperty("atlas.facet.ignore.efs", "bugoga");
        assertEquals("bugoga", storage.getProperty("atlas.facet.ignore.efs"));
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
}
