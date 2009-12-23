package uk.ac.ebi.ae3.indexbuilder;

import junit.framework.TestCase;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import uk.ac.ebi.ae3.indexbuilder.efo.Efo;
import uk.ac.ebi.ae3.indexbuilder.efo.EfoTerm;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import static org.junit.Assert.*;

/**
 * @author pashky
 */
public class EfoTest {

    Efo efo;

    @Before
    public void before() {
        efo = Efo.getEfo();
    }

    @After
    public void after() {
        efo.close();
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
