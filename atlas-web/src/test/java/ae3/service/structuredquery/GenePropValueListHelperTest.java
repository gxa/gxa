package ae3.service.structuredquery;

import ae3.AtlasAbstractTest;
import ae3.util.AtlasProperties;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.Iterator;

import junit.framework.TestCase;
import uk.ac.ebi.ae3.indexbuilder.AbstractOnceIndexTest;

/**
 * @author pashky
 */
public class GenePropValueListHelperTest extends AbstractOnceIndexTest {

    private static CoreContainer container;
    private static IValueListHelper service;

    @BeforeClass
    public static  void initContainer() throws Exception {
        container = new CoreContainer(getSolrPath().toString(), new File(getSolrPath(), "solr.xml"));
        service = new GenePropValueListHelper(new EmbeddedSolrServer(container, "atlas"));
    }

    @AfterClass
    public static void shutdownContainer() throws Exception {
        service = null;
        container.shutdown();
    }

    @Test
    public void testListAllValues() {
        Iterable<String> all = service.listAllValues("goterm");
        assertNotNull(all);
        assertNotNull(all.iterator());
        assertTrue(all.iterator().hasNext());
    }

    @Test
    public void testListAllValuesName() {
        Iterable<String> all = service.listAllValues("name");
        assertNotNull(all);
        assertNotNull(all.iterator());
        assertTrue(all.iterator().hasNext());
    }

    @Test
    public void testListAllValuesCrap() {
        Iterable<String> all = service.listAllValues("unknowncrap");
        assertNotNull(all);
        assertNotNull(all.iterator());
        assertFalse(all.iterator().hasNext());
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
        Iterable<AutoCompleteItem> ac = service.autoCompleteValues("name", "asp", -1, null);
        assertNotNull(ac);
        Iterator<AutoCompleteItem> i = ac.iterator();
        assertNotNull(i);
        assertTrue(i.hasNext());
        AutoCompleteItem aci = i.next();
        assertTrue(aci.getValue().toLowerCase().startsWith("asp"));
        assertTrue(aci.getCount() > 0);
        assertTrue(aci.getProperty().equals("name"));
    }
}
