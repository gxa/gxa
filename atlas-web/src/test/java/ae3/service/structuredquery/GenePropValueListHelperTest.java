package ae3.service.structuredquery;

import ae3.AtlasAbstractTest;
import ae3.util.AtlasProperties;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.junit.Test;

import java.io.File;
import java.util.Iterator;

import junit.framework.TestCase;

/**
 * @author pashky
 */
public class GenePropValueListHelperTest extends TestCase {
    private IValueListHelper service;

    @Override
    protected void setUp() throws Exception {
        final String solrIndexLocation = AtlasProperties.getProperty("atlas.solrIndexLocation");
        final CoreContainer multiCore = new CoreContainer(solrIndexLocation,
                new File(solrIndexLocation, "solr.xml"));
        service = new GenePropValueListHelper(new EmbeddedSolrServer(multiCore,"atlas"));
    }

    @Override
    protected void tearDown() throws Exception {
        service = null;
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
        Iterable<AutoCompleteItem> ac = service.autoCompleteValues("interproterm", "p53", 1);
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
        Iterable<AutoCompleteItem> ac = service.autoCompleteValues("interproterm", "p53", -1);
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
        Iterable<AutoCompleteItem> ac = service.autoCompleteValues("name", "asp", -1);
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
