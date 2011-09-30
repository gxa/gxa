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

package uk.ac.ebi.gxa.annotator.loader.biomart;

import junit.framework.TestCase;
import org.junit.Test;
import uk.ac.ebi.gxa.utils.Pair;

import java.util.Collection;

/**
 * @author Nataliya Sklyar
 */
public class BioMartDbDAOTest extends TestCase {
    protected BioMartDbDAO bioMartDbDAO;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        bioMartDbDAO = new BioMartDbDAO("ensembldb.ensembl.org:5306");
    }

    @Test
    public void testGetSynonyms() throws Exception {
        Collection<Pair<String, String>> synonyms = bioMartDbDAO.getSynonyms("gallus_gallus", "63");
        assertEquals(1030, synonyms.size());
    }

    @Test
    public void testfindDBName() throws Exception {
        String dbName = bioMartDbDAO.findSynonymsDBName("homo_sapiens", "63");
        assertEquals("homo_sapiens_core_63_37", dbName);
    }
}
