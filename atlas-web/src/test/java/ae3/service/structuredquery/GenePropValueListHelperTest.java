package ae3.service.structuredquery;

import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.ac.ebi.ae3.indexbuilder.AbstractOnceIndexTest;

import java.util.Iterator;

/**
 * @author pashky
 */
public class GenePropValueListHelperTest extends AbstractOnceIndexTest {

    private static AutoCompleter service;

    @BeforeClass
    public static  void initContainer() throws Exception {
        service = new GenePropValueListHelper(new EmbeddedSolrServer(getContainer(), "atlas"));
    }

    @AfterClass
    public static void shutdownContainer() throws Exception {
        service = null;
    }

    @Test
    public void testAutocompleteLimit() {
        Iterable<AutoCompleteItem> ac = service.autoCompleteValues("interproterm", "p53", 1, null);
        assertNotNull(ac);
        Iterator<AutoCompleteItem> i = ac.iterator();
        assertNotNull(i);
        assertTrue(i.hasNext());
        AutoCompleteItem aci = i.next();
        assertTrue(aci.getValue().toLowerCase().startsWith("p53"));
        assertTrue(aci.getCount() > 0);
        assertTrue(aci.getProperty().equals("interproterm"));
        assertFalse(i.hasNext());
    }

    @Test
    public void testAutocompleteUnlimit() {
        Iterable<AutoCompleteItem> ac = service.autoCompleteValues("interproterm", "p53", -1, null);
        assertNotNull(ac);
        Iterator<AutoCompleteItem> i = ac.iterator();
        assertNotNull(i);
        assertTrue(i.hasNext());
        AutoCompleteItem aci = i.next();
        assertTrue(aci.getValue().toLowerCase().startsWith("p53"));
        assertTrue(aci.getCount() > 0);
        assertTrue(aci.getProperty().equals("interproterm"));
        assertTrue(i.hasNext());
        aci = i.next();
        assertTrue(aci.getValue().toLowerCase().startsWith("p53"));
        assertTrue(aci.getCount() > 0);
        assertTrue(aci.getProperty().equals("interproterm"));
    }

    @Test
    public void testAutocompleteName() {
        Iterable<AutoCompleteItem> ac = service.autoCompleteValues(Constants.GENE_PROPERTY_NAME, "asp", -1, null);
        assertNotNull(ac);
        Iterator<AutoCompleteItem> i = ac.iterator();
        assertNotNull(i);
        assertTrue(i.hasNext());
        AutoCompleteItem aci = i.next();
        assertTrue(aci.getValue().toLowerCase().startsWith("asp"));
        assertTrue(aci.getCount() > 0);
        assertTrue(aci.getProperty().equals(Constants.GENE_PROPERTY_NAME));
    }
}
