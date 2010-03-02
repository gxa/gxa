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
        
        SolrQuery q = new SolrQuery("*:*");
        QueryResponse qr = solr.query(q);
        assertNotNull(qr.getResults());
        assertTrue(qr.getResults().getNumFound() > 0);
    }


    @Test
    public void testExptIndex() throws Exception {
        SolrServer solr = new EmbeddedSolrServer(getContainer(), "expt");
        SolrQuery q = new SolrQuery("*:*");
        QueryResponse qr = solr.query(q);

        assertNotNull(qr.getResults());
        assertTrue(qr.getResults().getNumFound() > 0);
    }


    @Test
    public void testPropsIndex() throws Exception {
        SolrServer solr = new EmbeddedSolrServer(getContainer(), "properties");
        SolrQuery q = new SolrQuery("*:*");
        QueryResponse qr = solr.query(q);

        assertNotNull(qr.getResults());
        assertTrue(qr.getResults().getNumFound() > 0);
    }

}
