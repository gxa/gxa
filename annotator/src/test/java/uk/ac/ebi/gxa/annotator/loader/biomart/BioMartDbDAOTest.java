/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.annotator.loader.biomart;

import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.gxa.utils.Pair;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Nataliya Sklyar
 */
public class BioMartDbDAOTest {
    private BioMartDbDAO bioMartDbDAO;

    @Before
    public void setUp() throws Exception {
        bioMartDbDAO = new BioMartDbDAO("ensembldb.ensembl.org:5306");
    }

    @Test
    public void testGetSynonyms() throws Exception {
        Collection<Pair<String, String>> synonyms = bioMartDbDAO.getSynonyms("gallus_gallus", "63");
        assertEquals(1030, synonyms.size());
    }

    @Test
    public void testFindDBName() throws Exception {
        String dbName = bioMartDbDAO.findSynonymsDBName("homo_sapiens", "63");
        assertEquals("homo_sapiens_core_63_37", dbName);
    }

    @Test
    public void testValidateConnection1() throws Exception {
        final BioMartDbDAO bioMartDbDAO1 = new BioMartDbDAO("ensembldb.ensem");
        String message = bioMartDbDAO1.validateConnection("xenopus_tropicalis", "66");
        assertFalse(message.isEmpty());
        assertTrue("Fails with invalid URL", message.contains("Invalid url"));


        final BioMartDbDAO bioMartDbDAO3 = new BioMartDbDAO("ensembldb.ensembl.org:5306");
        message = bioMartDbDAO3.validateConnection("xenopus_tropicalis", "66");
        assertTrue(message.isEmpty());
    }

    @Test
    public void testValidateConnection2() throws Exception {
        String message = bioMartDbDAO.validateConnection("xenopus_tropis", "66");
        assertFalse(message.isEmpty());
        assertTrue("Fails with Invalid database name", message.contains("Invalid database name"));

        message = bioMartDbDAO.validateConnection("", "66");
        assertFalse(message.isEmpty());
        assertTrue("Fails with Invalid database name", message.contains("Invalid database name"));

    }
}
