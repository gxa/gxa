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

package ae3.service.structuredquery;

import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.ac.ebi.gxa.index.AbstractOnceIndexTest;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.properties.ResourceFileStorage;

import java.util.Iterator;

import static ae3.service.structuredquery.Constants.GENE_PROPERTY_NAME;
import static org.junit.Assert.*;

/**
 * @author pashky
 */
public class GenePropValueListHelperTest extends AbstractOnceIndexTest {

    private static AtlasGenePropertyService service;

    @BeforeClass
    public static  void initContainer() throws Exception {
        service = new AtlasGenePropertyService();
        service.setSolrServerAtlas(new EmbeddedSolrServer(getContainer(), "atlas"));

        ResourceFileStorage storage = new ResourceFileStorage();
        storage.setResourcePath("atlas.properties");
        AtlasProperties atlasProperties = new AtlasProperties();
        atlasProperties.setStorage(storage);
        
        service.setAtlasProperties(atlasProperties);
    }

    @AfterClass
    public static void shutdownContainer() throws Exception {
        service = null;
    }

    @Test
    public void testAutocompleteLimit() {
        Iterable<AutoCompleteItem> ac = service.autoCompleteValues("interproterm", "Transcription factor", 1);
        assertNotNull(ac);
        Iterator<AutoCompleteItem> i = ac.iterator();
        assertNotNull(i);
        assertTrue(i.hasNext());
        AutoCompleteItem aci = i.next();
        assertTrue(aci.getValue().toLowerCase().startsWith("transcription factor"));
        assertTrue(aci.getCount() > 0);
        assertTrue(aci.getProperty().equals("interproterm"));
        assertFalse(i.hasNext());
    }

    @Test
    public void testAutocompleteUnlimit() {
        Iterable<AutoCompleteItem> ac = service.autoCompleteValues("interproterm", "Transcription factor", -1);
        assertNotNull(ac);
        Iterator<AutoCompleteItem> i = ac.iterator();
        assertNotNull(i);
        assertTrue(i.hasNext());
        AutoCompleteItem aci = i.next();
        assertTrue(aci.getValue().toLowerCase().startsWith("transcription factor"));
        assertTrue(aci.getCount() > 0);
        assertTrue(aci.getProperty().equals("interproterm"));
        assertTrue(i.hasNext());
        aci = i.next();
        assertTrue(aci.getValue().toLowerCase().startsWith("transcription factor"));
        assertTrue(aci.getCount() > 0);
        assertTrue(aci.getProperty().equals("interproterm"));
    }

    @Test
    public void testAutocompleteName() {
        Iterable<AutoCompleteItem> ac = service.autoCompleteValues(GENE_PROPERTY_NAME, "C36C9.2", -1);
        assertNotNull(ac);
        Iterator<AutoCompleteItem> i = ac.iterator();
        assertNotNull(i);
        assertTrue(i.hasNext());
        AutoCompleteItem aci = i.next();
        assertTrue(aci.getValue().toLowerCase().startsWith("c36c9.2"));
        assertTrue(aci.getCount() > 0);
        assertTrue(aci.getProperty().equals(GENE_PROPERTY_NAME));
    }
}
