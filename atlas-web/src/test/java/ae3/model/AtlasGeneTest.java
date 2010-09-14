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

package ae3.model;

import uk.ac.ebi.gxa.index.AbstractOnceIndexTest;
import uk.ac.ebi.gxa.index.GeneExpressionAnalyticsTable;
import ae3.dao.AtlasSolrDAO;
import ae3.service.structuredquery.UpdownCounter;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.gxa.utils.EfvTree;
import uk.ac.ebi.microarray.atlas.model.ExpressionAnalysis;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.commons.lang.StringUtils;

import java.util.*;

import static junit.framework.Assert.assertEquals;

/**
 * @author pashky
 */
public class AtlasGeneTest  extends AbstractOnceIndexTest {
    private AtlasGene gene;

    @Before
    public void initGene() {
        AtlasSolrDAO atlasSolrDAO = new AtlasSolrDAO();
        atlasSolrDAO.setSolrServerAtlas(new EmbeddedSolrServer(getContainer(), "atlas"));
        atlasSolrDAO.setSolrServerExpt(new EmbeddedSolrServer(getContainer(), "expt"));
        gene = atlasSolrDAO.getGeneByIdentifier("ENSMUSG00000020275").getGene();
    }

    @Test
    public void test_getGeneSpecies() {
        assertNotNull(gene.getGeneSpecies());
        assertEquals("Mus musculus", gene.getGeneSpecies());
    }

    @Test
    public void test_getGene() {
        assertNotNull(gene);
    }

    @Test
    public void test_getGeneIds() {
        assertNotNull(gene.getGeneId());
        assertTrue(gene.getGeneId().matches("^[0-9]+$"));
    }

    @Test
    public void test_getGeneName() {
        assertNotNull(gene.getGeneName());
        assertEquals("Rel", gene.getGeneName());
    }

    @Test
    public void test_getGeneIdentifier() {
        assertNotNull(gene.getGeneIdentifier());
        assertEquals("ENSMUSG00000020275", gene.getGeneIdentifier());
    }

    @Test
    public void test_getGeneSolrDocument() {
        SolrDocument solrdoc = gene.getGeneSolrDocument();
        assertNotNull(solrdoc);
        assertTrue(solrdoc.getFieldNames().contains("id"));
    }

    @Test
    public void test_highlighting() {
        Map<String, List<String>> highlights = new HashMap<String, List<String>>();
        highlights.put("property_synonym", Arrays.asList("<em>ASPM</em>", "MCPH5", "RP11-32D17.1-002", "hCG_2039667"));
        gene.setGeneHighlights(highlights);
        assertTrue(gene.getHilitPropertyValue("synonym").matches(".*<em>.*"));
    }

   @Test
    public void test_getExperimentsTable() {
        GeneExpressionAnalyticsTable et = gene.getExpressionAnalyticsTable();
        assertTrue(et.getAll().iterator().hasNext());
    }

    @Test
    public void test_getNumberOfExperiments() {
        assertTrue(gene.getNumberOfExperiments() > 0);
    }

    @Test
    public void test_getHeatMapRows() {
        EfvTree<UpdownCounter> rows = gene.getHeatMap(Arrays.asList("age,dose,time,individual".split(",")));
        assertNotNull(rows);
        assertTrue(rows.getNumEfvs() > 0);
    }

    @Test
    public void test_getTopFVs() {
        Collection<ExpressionAnalysis> efvs = gene.getTopFVs(174501824);
        assertNotNull(efvs);

        float pv = 0;
        for(ExpressionAnalysis t : efvs) {
            assertTrue(pv <= t.getPValAdjusted());
            pv = t.getPValAdjusted();
            System.out.println(t.toString());
        }
    }

    @Test
    public void test_getHighestRankEF() {
        Pair<String,Float> hef = gene.getHighestRankEF(174501824);
        assertNotNull(hef);
        assertTrue(hef.getSecond() >= 0);
        assertTrue(hef.getFirst().matches(".*[A-Za-z]+.*"));
    }
}
