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

import ae3.dao.GeneSolrDAO;
import ae3.service.AtlasStatisticsQueryService;
import ae3.service.structuredquery.UpdownCounter;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrDocument;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.gxa.index.AbstractOnceIndexTest;
import uk.ac.ebi.gxa.statistics.*;
import uk.ac.ebi.gxa.utils.EfvTree;

import java.util.*;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author pashky
 */
public class AtlasGeneTest extends AbstractOnceIndexTest {
    private AtlasGene gene;

    @Before
    public void initGene() throws Exception {
        GeneSolrDAO geneSolrDAO = new GeneSolrDAO();
        geneSolrDAO.setGeneSolr(new EmbeddedSolrServer(getContainer(), "atlas"));
        gene = geneSolrDAO.getGeneByIdentifier("ENSMUSG00000020275").getGene();
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
    public void test_getHeatMapRows() {
        // Mock atlasStatisticsQueryService
        AtlasStatisticsQueryService atlasStatisticsQueryService = EasyMock.createMock(AtlasStatisticsQueryService.class);

        // Inject required functionality
        EfvAttribute attr = new EfvAttribute("cell_type", "B220+ b cell", StatisticsType.UP_DOWN);
        ExperimentInfo ei = new ExperimentInfo("E-MTAB-25", 411512559l);
        ExperimentResult exp = new ExperimentResult(ei);
        exp.setPValTstatRank(PTRank.of(0.007f, 0));
        EasyMock.expect(atlasStatisticsQueryService.getScoringEfvsForBioEntity(gene.getGeneId(), StatisticsType.UP_DOWN)).andReturn(Collections.<EfvAttribute>singletonList(attr));

        EasyMock.expect(atlasStatisticsQueryService.getExperimentsSortedByPvalueTRank(gene.getGeneId(), attr, -1, -1)).andReturn(Collections.<ExperimentResult>singletonList(exp));
        attr.setStatType(StatisticsType.NON_D_E);

        EasyMock.expect(atlasStatisticsQueryService.getExperimentCountsForBioEntity(attr, gene.getGeneId())).andReturn(1);

        // Prepare injected functionality for test
        EasyMock.replay(atlasStatisticsQueryService);

        // Test
        EfvTree<UpdownCounter> rows = gene.getHeatMap(Arrays.asList("age,dose,time,individual".split(",")), atlasStatisticsQueryService, true);

        assertNotNull(rows);
        assertTrue(rows.getNumEfvs() > 0);
    }


}
