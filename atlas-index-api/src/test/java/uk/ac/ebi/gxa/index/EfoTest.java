package uk.ac.ebi.gxa.index;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.efo.EfoTerm;
import uk.ac.ebi.gxa.utils.FileUtil;
import org.junit.*;
import static org.junit.Assert.*;

/**
 * @author pashky
 */
public class EfoTest {

    static Efo efo;
    static File tempDirectory;

    @BeforeClass
    public static void before() throws URISyntaxException {
        
        efo = new Efo();
        tempDirectory = FileUtil.createTempDirectory("efoindex");
        efo.setIndexFile(tempDirectory);
        efo.setUri(new URI("resource:META-INF/efo.owl"));
    }

    @AfterClass
    public static void after() {
        efo.close();
        FileUtil.deleteDirectory(tempDirectory);
    }

    @Test
    public void testExternalSource() throws URISyntaxException {
        Efo efo = new Efo();
        File tempDirectory = FileUtil.createTempDirectory("efoindex-ext");
        efo.setIndexFile(tempDirectory);
        efo.setUri(new URI("http://efo.svn.sourceforge.net/svnroot/efo/trunk/src/efoinowl/efo.owl"));
        assertTrue(efo.getAllTerms().size() > 0);
        FileUtil.deleteDirectory(tempDirectory);
    }

    @Test
    public void testEfoLoaded() {
        assertNotNull(efo);
        assertEquals(1640, efo.getAllTerms().size());
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
        assertTrue(efo.getRootIds().contains("EFO_0000001"));
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
        for(EfoTerm t : coll)
            if(id.equals(t.getId()))
                return true;
        return false;
    }
}
