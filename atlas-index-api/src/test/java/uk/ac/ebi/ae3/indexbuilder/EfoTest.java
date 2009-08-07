package uk.ac.ebi.ae3.indexbuilder;

import junit.framework.TestCase;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author pashky
 */
public class EfoTest extends TestCase {

    public void testEfoLoaded() {
        Efo efo = Efo.getEfo();
        assertNotNull(efo);
        assertEquals(1640, efo.getAllTerms().size());
    }

    public void testEfoRoot() {
        Efo efo = Efo.getEfo();
        assertTrue(efo.hasTerm("EFO_0000001"));

        Efo.Term term = efo.getTermById("EFO_0000001");
        assertNotNull(term.getId());
        assertEquals("EFO_0000001", term.getId());
        assertNotNull(term.getTerm());
        assertEquals("experimental factor", term.getTerm());
        assertTrue(term.isExpandable());
    }

    public void testEfo787() {
        Efo efo = Efo.getEfo();

        Efo.Term efo787 = efo.getTermById("EFO_0000787");
        assertNotNull(efo787);
        Efo.Term efo298 = efo.getTermById("EFO_0000298");
        assertNotNull(efo298);
        Efo.Term efo806 = efo.getTermById("EFO_0000806");
        assertNotNull(efo806);

        Collection<Efo.Term> children;

        children = efo.getTermChildren("EFO_0000787");
        assertTrue(children.contains(efo806));

        children = efo.getTermChildren("EFO_0000806");
        assertTrue(children.contains(efo298));
    }

    public void testBranches() {
        Efo efo = Efo.getEfo();

        Efo.Term term;
        term = efo.getTermById("EFO_0000635");
        assertTrue(term.isBranchRoot());
        term = efo.getTermById("EFO_0000634");
        assertTrue(term.isBranchRoot());
        term = efo.getTermById("EFO_0000321");
        assertTrue(term.isBranchRoot());
    }

    private boolean isTermInCollection(Collection<Efo.Term> coll, String id) {
        for(Efo.Term t : coll)
            if(id.equals(t.getId()))
                return true;
        return false;
    }

    public void testSearch() {
        Efo efo = Efo.getEfo();

        assertTrue(efo.searchTermPrefix("organ").contains("EFO_0000635"));
        assertFalse(efo.searchTermPrefix("organ").contains("EFO_0000298"));
        assertTrue(efo.searchTermPrefix("organ").contains("EFO_0000634"));
        assertTrue(efo.searchTermPrefix("cell").contains("EFO_0000321"));
    }

    public void testParentPaths() {
        Efo efo = Efo.getEfo();

        Collection<List<Efo.Term>> result = efo.getTermParentPaths("EFO_0000298", true);
        assertFalse(result.isEmpty());
        Collection<Efo.Term> path = result.iterator().next();
        assertTrue(isTermInCollection(path, "EFO_0000806"));
        assertTrue(isTermInCollection(path, "EFO_0000787"));
    }

    public void testRoots() {
        Efo efo = Efo.getEfo();

        assertTrue(efo.getRootIds().contains("EFO_0000001"));        
    }

    public void testSubTree() {
        Efo efo = Efo.getEfo();

        Set<String> ids = efo.getTermParents("EFO_0000872", true);
        Collection<Efo.Term> result = efo.getSubTree(ids);
        assertTrue(isTermInCollection(result, "EFO_0000870"));
        assertTrue(isTermInCollection(result, "EFO_0000635"));
        assertTrue(!isTermInCollection(result, "EFO_0000001"));
    }

    public void testTermsWithoutId() {
        Efo efo = Efo.getEfo();

        Set<String> terms = efo.getAllTermIds();
        assertFalse(terms.contains(""));
    }
}
