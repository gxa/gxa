package uk.ac.ebi.gxa.annotator.loader.biomart;

import junit.framework.TestCase;
import org.junit.Test;
import uk.ac.ebi.gxa.utils.Pair;

import java.util.HashSet;

/**
 * User: nsklyar
 * Date: 11/08/2011
 */
public class BioMartDbDAOTest extends TestCase {

    protected BioMartDbDAO bioMartDbDAO;


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        bioMartDbDAO = new BioMartDbDAO("ensembldb.ensembl.org:5306");
    }

    @Test
    public void testGetSynonyms() throws Exception{
        HashSet<Pair<String, String>> synonyms = bioMartDbDAO.getSynonyms("gallus_gallus", "63");
        assertEquals(1030, synonyms.size());
    }

    @Test
    public void testfindDBName() throws Exception {
        String dbName = bioMartDbDAO.findDBName("homo_sapiens", "63");
        assertEquals("homo_sapiens_core_63_37", dbName);
    }

}
