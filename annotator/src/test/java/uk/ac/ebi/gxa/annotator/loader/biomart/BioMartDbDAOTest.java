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

import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import uk.ac.ebi.gxa.utils.Pair;

import java.util.Collection;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Nataliya Sklyar
 */
public class BioMartDbDAOTest {

    @Test
    public void testGetSynonyms() throws BioMartException {
        Collection<Pair<String, String>> synonyms = validBioMartDao().getSynonyms("gallus_gallus", "67");
        assertTrue(synonyms.size() > 15000);
    }

    @Test
    public void testFindDBName() {
        String dbName = validBioMartDao().findSynonymsDBName("homo_sapiens", "63");
        assertEquals("homo_sapiens_core_63_37", dbName);
    }

    @Test
    public void testInvalidDbUrl() {
        assertInvalidDbUrl("ensembldb.ensem", "xenopus_tropicalis", "66");
    }

    @Test
    public void testInvalidDbName() throws Exception {
        assertInvalidDbName("xenopus_tropis", "66");
        assertInvalidDbName("", "66");
    }

    @Test
    public void testDbConnectionTest() {
        assertInvalidDbConnection("ensembldb.ensem", "xenopus_tropicalis", "66");
        assertInvalidDbConnection("ensembldb.ensembl.org:5306", "xenopus_tropis", "66");
        assertInvalidDbConnection("ensembldb.ensembl.org:5306", "", "66");
    }

    private static BioMartDbDAO validBioMartDao() {
        return new BioMartDbDAO("ensembldb.ensembl.org:5306");
    }

    private static void assertInvalidDbUrl(String url, String dbName, String version) {
        try {
            (new BioMartDbDAO(url)).findSynonymsDBName(dbName, version);
            fail("No DataAccessException thrown for invalid DB url: " + url);
        } catch (DataAccessException e) {
            // ok
        }
    }

    private static void assertInvalidDbName(String dbName, String version) {
        try {
            validBioMartDao().findSynonymsDBName(dbName, version);
            fail("No IncorrectResultSizeDataAccessException thrown for invalid DB name/version: " + dbName + "/" + version);
        } catch (IncorrectResultSizeDataAccessException e) {
            // ok
        }
    }

    private static void assertInvalidDbConnection(String url, String dbName, String version) {
        try {
            (new BioMartDbDAO(url)).testConnection(dbName, version);
            fail("No BioMartException thrown for invalid DB settings: url=[" + url + "], dbName=[" + dbName + "], version=[" + version + "]");
        }  catch (BioMartException e) {
            // ok
        }
    }
}
