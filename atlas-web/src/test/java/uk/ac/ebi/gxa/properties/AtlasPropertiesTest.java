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

import java.util.*;

/**
 * @author pashky
 */
public class AtlasPropertiesTest {
    AtlasProperties props;

    @Before
    public void setup() {
        ResourceFileStorage storage = new ResourceFileStorage();
        storage.setResourcePath("atlas.properties");

        props = new AtlasProperties();
        props.setStorage(storage);
    }

    @Test
    public void test_getAvailablePropertyNames() {
        Collection<String> propnames = props.getAvailablePropertyNames();
        assertNotNull(propnames);
        assertFalse(propnames.isEmpty());
        assertTrue(propnames.containsAll(Arrays.asList("atlas.feedback.from.address,atlas.drilldowns.mingenes,atlas.dump.geneidentifiers.filename,atlas.gene.autocomplete.ids.limit,atlas.gene.drilldowns,atlas.query.pagesize,atlas.gene.autocomplete.names.limit,atlas.dump.geneidentifiers,atlas.query.expsPerGene,atlas.gene.list.cache.autogenerate,atlas.feedback.subject,atlas.data.release,atlas.gene.autocomplete.names,atlas.gene.autocomplete.ids,atlas.feedback.smtp.host,atlas.gene.autocomplete.descs,atlas.query.listsize,atlas.feedback.to.address,atlas.dump.ebeye.filename".split(","))));
    }

    @Test
    public void test_getValue() {
        assertEquals("smtp.ebi.ac.uk", props.getFeedbackSmtpHost());

        assertEquals(100, props.getQueryPageSize());

        List<String> vals = props.getQueryDrilldownGeneFields();
        assertNotNull(vals);
        assertFalse(vals.isEmpty());
        assertTrue(vals.containsAll(Arrays.asList("disease,goterm,interproterm,keyword,proteinname".split(","))));

        assertEquals("", props.getProperty("unknwonnonexistentneverever"));
    }

    @Test
    public void test_setValue() {
        props.setProperty("dummy", "hoppa");
        assertEquals("hoppa", props.getProperty("dummy"));

        props.setProperty("atlas.feedback.smtp.host", "localhost");
        assertEquals("localhost", props.getFeedbackSmtpHost());
    }

    @Test
    public void test_listeners() {
        final boolean notified[] = new boolean[1];
        AtlasPropertiesListener listener = new AtlasPropertiesListener() {
            public void onAtlasPropertiesUpdate(AtlasProperties atlasProperties) {
                notified[0] = true;
            }
        };
        
        props.registerListener(listener);
        props.setProperty("dummy", "hoppa");
        assertTrue(notified[0]);
        notified[0] = false;

        props.reload();
        assertTrue(notified[0]);
        notified[0] = false;

        props.unregisterListener(listener);
        props.setProperty("dummy", "hsadasd");
        assertFalse(notified[0]);
        props.reload();
        assertFalse(notified[0]);
    }

    @Test
    public void test_reload() {
        props.setProperty("atlas.feedback.smtp.host", "localhost");
        props.reload();
        assertEquals("smtp.ebi.ac.uk", props.getFeedbackSmtpHost());
    }
}
