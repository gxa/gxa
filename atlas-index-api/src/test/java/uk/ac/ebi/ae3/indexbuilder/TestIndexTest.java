package uk.ac.ebi.ae3.indexbuilder;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.core.CoreContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

/**
 * @author pashky
 */
public class TestIndexTest extends AbstractOnceIndexTest {

    @Test
    public void testAtlasIndex() throws Exception {
        SolrServer solr = new EmbeddedSolrServer(getContainer(), "atlas");
        
        SolrQuery q = new SolrQuery("gene_id:[* TO *]");
        QueryResponse qr = solr.query(q);
        assertNotNull(qr.getResults());
        assertTrue(qr.getResults().getNumFound() > 0);
    }


    @Test
    public void testExptIndex() throws Exception {
        SolrServer solr = new EmbeddedSolrServer(getContainer(), "expt");
        SolrQuery q = new SolrQuery("dwe_exp_id:[* TO *]");
        QueryResponse qr = solr.query(q);

        assertNotNull(qr.getResults());
        assertTrue(qr.getResults().getNumFound() > 0);
    }
}
