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

package uk.ac.ebi.gxa.efo;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author pashky
 */
public class EfoTest {

    static Efo efo;

    @BeforeClass
    public static void before() throws URISyntaxException {

        efo = new EfoImpl();
        efo.setUri(new URI("resource:META-INF/efo.owl"));
        System.setProperty("entityExpansionLimit", "100000000");

    }

    @AfterClass
    public static void after() {
        efo.close();
    }

    @Test
    public void testLoadTwice() {

        try {
            Efo efo = new EfoImpl();
            efo.setUri(new URI("resource:META-INF/efo.owl"));
            efo.load();

            int termSize = efo.getAllTerms().size();
            assertNotNull(efo);

            // wait a bit
            synchronized (this) {
                try {
                    wait(2000);
                } catch (InterruptedException e) {
                    // ignore
                }
            }

            // load again
            efo.load();
            assertNotNull(efo);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail();
        }
    }


    @Test
    public void testLoadAgain() {

        try {
            Efo efo = new EfoImpl();
            efo.setUri(new URI("resource:META-INF/efo.owl"));
            efo.load();

            int termSize = efo.getAllTerms().size();
            assertNotNull(efo);

            // wait a bit
            synchronized (this) {
                try {
                    wait(2000);
                } catch (InterruptedException e) {
                    // ignore
                }
            }

            // load again
            efo.load();
            assertNotNull(efo);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testGetVersion() {
        assertNotNull(efo);
        //assertEquals("1.2.3", efo.getVersion());
        assertTrue(efo.getVersionInfo().length() > 0);
    }

    @Test
    public void testEfoLoaded() {
        assertNotNull(efo);
    }

    @Test
    public void testBranches() {
        EfoTerm term;
        term = efo.getTermById("EFO_0000635");
        assertTrue(term.isBranchRoot());
        term = efo.getTermById("EFO_0000324");
        assertTrue(term.isBranchRoot());
    }

    @Test
    public void testSearchPrefix() {
        assertTrue(isTermInCollection(efo.searchTermPrefix("organ"), "EFO_0000635"));
        assertFalse(isTermInCollection(efo.searchTermPrefix("organ"), "EFO_0000298"));
        assertTrue(isTermInCollection(efo.searchTermPrefix("joint"), "EFO_0000948"));
        assertTrue(isTermInCollection(efo.searchTermPrefix("cell"), "EFO_0000322"));
    }

    @Test
    public void testSearch() {
        assertTrue(isTermInCollection(efo.searchTerm("cell line"), "EFO_0000322"));

        {
            final Collection<EfoTerm> result = efo.searchTerm("EFO_0000322");
            assertEquals(1, result.size());
            assertTrue(isTermInCollection(result, "EFO_0000322"));
        }

        {
            final Collection<EfoTerm> result = efo.searchTerm("efo_0000322");
            assertEquals(1, result.size());
            assertTrue(isTermInCollection(result, "EFO_0000322"));
        }
    }

    @Test
    public void testParentPaths() {
        Collection<List<EfoTerm>> result = efo.getTermParentPaths("EFO_0000298", true);
        assertFalse(result.isEmpty());
        Collection<EfoTerm> path = result.iterator().next();
        assertTrue(isTermInCollection(path, "EFO_0003858"));
        assertTrue(isTermInCollection(path, "EFO_0000787"));
    }

    @Test
    public void testRoots() {
        assertTrue(efo.getRootIds().contains("Other"));
    }

    @Test
    public void testSubTree() {
        Set<String> ids = efo.getTermParents("EFO_0000872", true);
        Collection<EfoTerm> result = efo.getSubTree(ids);
        assertTrue(isTermInCollection(result, "EFO_0000870"));
        assertTrue(isTermInCollection(result, "EFO_0000787"));
        assertTrue(isTermInCollection(result, "EFO_0000635"));
        assertTrue(!isTermInCollection(result, "EFO_0000001"));
    }

    private boolean isTermInCollection(Collection<EfoTerm> coll, String id) {
        for (EfoTerm t : coll) {
            if (id.equals(t.getId())) {
                return true;
            }
        }
        return false;
    }
}
