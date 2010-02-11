package uk.ac.ebi.gxa.index;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author pashky
 */
public class TestIndexTest extends AbstractOnceIndexTest {

    @Test
    public void testAtlasIndex() throws Exception {
        SolrServer solr = new EmbeddedSolrServer(getContainer(), "atlas");
        
        SolrQuery q = new SolrQuery("id:[* TO *]");
        QueryResponse qr = solr.query(q);
        assertNotNull(qr.getResults());
        assertTrue(qr.getResults().getNumFound() > 0);
    }


    @Test
    public void testExptIndex() throws Exception {
        SolrServer solr = new EmbeddedSolrServer(getContainer(), "expt");
        SolrQuery q = new SolrQuery("id:[* TO *]");
        QueryResponse qr = solr.query(q);

        assertNotNull(qr.getResults());
        assertTrue(qr.getResults().getNumFound() > 0);
    }


    @Test
    public void testPropsIndex() throws Exception {
        SolrServer solr = new EmbeddedSolrServer(getContainer(), "properties");
        SolrQuery q = new SolrQuery("[* TO *]");
        QueryResponse qr = solr.query(q);

        assertNotNull(qr.getResults());
        assertTrue(qr.getResults().getNumFound() > 0);
    }

}
