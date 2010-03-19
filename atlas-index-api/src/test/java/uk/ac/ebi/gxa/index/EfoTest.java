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

package uk.ac.ebi.gxa.index;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.efo.EfoTerm;

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

        efo = new Efo();
        efo.setUri(new URI("resource:META-INF/efo.owl"));
    }

    @AfterClass
    public static void after() {
        efo.close();
    }

    public static class ResourceHttpServer extends Thread {

        private int port;
        private final String resource;
        private boolean stop = false;
        private ServerSocket serverSocket;

        public ResourceHttpServer(int startPort, String resource) {
            this.resource = resource;
            for (port = startPort; port < startPort + 1000; ++port) {
                try {
                    serverSocket = new ServerSocket(port);
                    break;
                }
                catch (IOException e) {
                    // continue
                }
            }
        }

        public int getPort() {
            return port;
        }

        @Override
        public void run() {
            try {
                while (!stop) {
                    Socket sock = serverSocket.accept();

                    BufferedReader in
                            = new BufferedReader(
                            new InputStreamReader(
                                    sock.getInputStream()));
                    String first = in.readLine();

                    String status = "HTTP/1.0 200 OK\r\nContent-Type: text/xml\r\n\r\n";
                    byte[] buf = status.getBytes("UTF-8");
                    sock.getOutputStream().write(buf, 0, buf.length);

                    InputStream is = getClass().getClassLoader().getResourceAsStream(resource);
                    buf = new byte[1024];
                    int len = 0;
                    while ((len = is.read(buf)) >= 0) {
                        sock.getOutputStream().write(buf, 0, len);
                    }
                    sock.getOutputStream().flush();
                    sock.close();
                }
                serverSocket.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void kill() {
            stop = true;
        }
    }

    @Test
    public void testExternalSource() throws URISyntaxException {

        ResourceHttpServer server = new ResourceHttpServer(12345, "META-INF/efo.owl");
        server.start();

        Efo efo = new Efo();
        efo.setUri(new URI("http://localhost:" + server.getPort() + "/efo.owl"));
        assertTrue(efo.getAllTerms().size() > 0);

        server.kill();
    }

    @Test
    public void testLoadTwice() {
        try {
            Efo efo = new Efo();
            efo.setUri(new URI("resource:META-INF/efo.owl"));
            int termSize = efo.getAllTerms().size();
            assertNotNull(efo);
            assertEquals(1641, termSize);

            // wait a bit
            synchronized (this) {
                try {
                    wait(2000);
                }
                catch (InterruptedException e) {
                    // ignore
                }
            }

            // load again
            efo.load();
            assertNotNull(efo);
            assertEquals(1641, termSize);
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testGetVersion() {
        assertNotNull(efo);
        assertEquals("1.2.3", efo.getVersion());
        assertTrue(efo.getVersionInfo().length() > 0);
    }

    @Test
    public void testEfoLoaded() {
        assertNotNull(efo);
        assertEquals(1641, efo.getAllTerms().size());
    }

    @Test
    public void testEfoRoot() {
        assertTrue(efo.hasTerm("EFO_0000001"));

        EfoTerm term = efo.getTermById("EFO_0000001");
        assertNotNull(term.getId());
        assertEquals("EFO_0000001", term.getId());
        assertNotNull(term.getTerm());
        assertEquals("experimental factor", term.getTerm());
        assertTrue(term.isExpandable());
    }

    @Test
    public void testEfo787() {

        EfoTerm efo787 = efo.getTermById("EFO_0000787");
        assertNotNull(efo787);
        EfoTerm efo298 = efo.getTermById("EFO_0000298");
        assertNotNull(efo298);
        EfoTerm efo806 = efo.getTermById("EFO_0000806");
        assertNotNull(efo806);

        Collection<EfoTerm> children;

        children = efo.getTermChildren("EFO_0000787");
        assertTrue(children.contains(efo806));

        children = efo.getTermChildren("EFO_0000806");
        assertTrue(children.contains(efo298));
    }

    @Test
    public void testBranches() {
        EfoTerm term;
        term = efo.getTermById("EFO_0000635");
        assertTrue(term.isBranchRoot());
        term = efo.getTermById("EFO_0000634");
        assertTrue(term.isBranchRoot());
        term = efo.getTermById("EFO_0000321");
        assertTrue(term.isBranchRoot());
    }

    @Test
    public void testSearchPrefix() {
        assertTrue(efo.searchTermPrefix("organ").contains("EFO_0000635"));
        assertFalse(efo.searchTermPrefix("organ").contains("EFO_0000298"));
        assertTrue(efo.searchTermPrefix("organ").contains("EFO_0000634"));
        assertTrue(efo.searchTermPrefix("cell").contains("EFO_0000321"));
    }

    @Test
    public void testSearch() {
        assertTrue(isTermInCollection(efo.searchTerm("cell"), "EFO_0000321"));

        {
            final Collection<EfoTerm> result = efo.searchTerm("EFO_0000321");
            assertEquals(1, result.size());
            assertTrue(isTermInCollection(result, "EFO_0000321"));
        }

        {
            final Collection<EfoTerm> result = efo.searchTerm("efo_0000321");
            assertEquals(1, result.size());
            assertTrue(isTermInCollection(result, "EFO_0000321"));
        }
    }

    @Test
    public void testParentPaths() {
        Collection<List<EfoTerm>> result = efo.getTermParentPaths("EFO_0000298", true);
        assertFalse(result.isEmpty());
        Collection<EfoTerm> path = result.iterator().next();
        assertTrue(isTermInCollection(path, "EFO_0000806"));
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
