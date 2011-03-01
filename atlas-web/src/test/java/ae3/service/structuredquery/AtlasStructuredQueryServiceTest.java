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

package ae3.service.structuredquery;

import ae3.dao.AtlasSolrDAO;
import ae3.service.AtlasBitIndexQueryService;
import ae3.service.AtlasStatisticsQueryService;
import com.google.common.collect.Multiset;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.efo.EfoImpl;
import uk.ac.ebi.gxa.index.AbstractOnceIndexTest;
import uk.ac.ebi.gxa.index.StatisticsStorageFactory;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.properties.ResourceFileStorage;
import uk.ac.ebi.gxa.statistics.Attribute;
import uk.ac.ebi.gxa.statistics.EfvAttribute;
import uk.ac.ebi.gxa.statistics.StatisticsStorage;
import uk.ac.ebi.gxa.statistics.StatisticsType;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author pashky
 */
public class AtlasStructuredQueryServiceTest extends AbstractOnceIndexTest {

    AtlasStructuredQueryService service;
    private Attribute hematopoieticStemCellEfv;

    @Before
    public void createService() throws Exception {
        EmbeddedSolrServer solrServerAtlas = new EmbeddedSolrServer(getContainer(), "atlas");
        EmbeddedSolrServer expt = new EmbeddedSolrServer(getContainer(), "expt");
        EmbeddedSolrServer serverProp = new EmbeddedSolrServer(getContainer(), "properties");

        ResourceFileStorage storage = new ResourceFileStorage();
        storage.setResourcePath("atlas.properties");
        AtlasProperties atlasProperties = new AtlasProperties();
        atlasProperties.setStorage(storage);

        Efo efo = new EfoImpl();
        efo.setUri(new URI("resource:META-INF/efo.owl"));

        AtlasSolrDAO atlasSolrDAO = new AtlasSolrDAO();
        atlasSolrDAO.setSolrServerAtlas(solrServerAtlas);
        atlasSolrDAO.setSolrServerExpt(expt);

        AtlasEfvService efvService = new AtlasEfvService();
        efvService.setSolrServerAtlas(solrServerAtlas);
        efvService.setSolrServerProp(serverProp);
        efvService.setAtlasProperties(atlasProperties);

        AtlasEfoService efoService = new AtlasEfoService();
        efoService.setEfo(efo);
        efoService.setSolrServerAtlas(solrServerAtlas);

        AtlasGenePropertyService gpService = new AtlasGenePropertyService();
        gpService.setAtlasProperties(atlasProperties);
        gpService.setSolrServerAtlas(solrServerAtlas);

        AtlasNetCDFDAO atlasNetCDFDAO = new AtlasNetCDFDAO();

        String bitIndexResourceName = "bitstats";
        File bitIndexResourcePath = new File(this.getClass().getClassLoader().getResource(bitIndexResourceName).toURI());
        StatisticsStorageFactory statisticsStorageFactory = new StatisticsStorageFactory(bitIndexResourceName);
        statisticsStorageFactory.setAtlasIndex(new File(bitIndexResourcePath.getParent()));
        StatisticsStorage<Long> statisticsStorage = statisticsStorageFactory.createStatisticsStorage();
        AtlasStatisticsQueryService atlasStatisticsQueryService = new AtlasBitIndexQueryService(bitIndexResourceName);
        atlasStatisticsQueryService.setStatisticsStorage(statisticsStorage);

        service = new AtlasStructuredQueryService();
        service.setSolrServerAtlas(solrServerAtlas);
        service.setSolrServerExpt(expt);
        service.setSolrServerProp(serverProp);
        service.setAtlasSolrDAO(atlasSolrDAO);
        service.setEfoService(efoService);
        service.setEfvService(efvService);
        service.setEfo(efo);
        service.setAtlasProperties(atlasProperties);
        service.setGenePropService(gpService);
        service.setAtlasNetCDFDAO(atlasNetCDFDAO);
        service.setAtlasStatisticsQueryService(atlasStatisticsQueryService);
    }

    @After
    public void dropService() {
        service = null;
    }

    private static boolean containsString(Iterable iter, String s) {
        for (Object o : iter)
            if (o != null && o.toString().equals(s))
                return true;
        return false;
    }

    @Test
    public void test_getGeneProperties() {
        Iterable<String> gprops = service.getGenePropertyOptions();
        assertTrue(gprops.iterator().hasNext());
        assertTrue(containsString(gprops, "gene"));
        assertTrue(containsString(gprops, "keyword"));
        assertTrue(containsString(gprops, "goterm"));
    }


    @Test
    public void test_doStructuredAtlasQuery() {
        AtlasStructuredQueryResult result = service.doStructuredAtlasQuery(
                new AtlasStructuredQueryBuilder()
                        .andGene("ENSMUSG00000020275")
                        .query()
        );

        assertNotNull(result);
        assertTrue(result.getSize() > 0);
    }

    @Test
    public void test_getStats() {
        Map<StatisticsType, HashMap<String, Multiset<Integer>>> scoresCache = service.getScoresCache();

        long geneId = 169968252l;  // identifier: ENSMUSG00000020275; name: Rel)
        hematopoieticStemCellEfv = new EfvAttribute("cell_type", "hematopoietic stem cell", StatisticsType.UP_DOWN);
        boolean showNonDEData = true;
        UpdownCounter counter = service.getStats(scoresCache, hematopoieticStemCellEfv, geneId, Collections.singleton(geneId), showNonDEData);
        assertFalse(counter.isZero());
        assertTrue(counter.getNoStudies() > 0 || counter.getNones() > 0);
        assertTrue(counter.getMpvDn() != 1 || counter.getMpvUp() != 1); // At least one of up/down min pVals should have been populated

        // Now check that the counts were stored in cache
        Multiset<Integer> upCounts = service.getScoresFromCache(scoresCache, StatisticsType.UP, hematopoieticStemCellEfv.getValue());
        Multiset<Integer> downCounts = service.getScoresFromCache(scoresCache, StatisticsType.DOWN, hematopoieticStemCellEfv.getValue());
        Multiset<Integer> nonDECounts = service.getScoresFromCache(scoresCache, StatisticsType.NON_D_E, hematopoieticStemCellEfv.getValue());
        assertTrue(upCounts.entrySet().size() > 0 || downCounts.entrySet().size() > 0 || nonDECounts.entrySet().size() > 0);

    }
}
