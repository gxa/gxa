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
import uk.ac.ebi.gxa.statistics.StatisticsStorage;

import java.io.File;
import java.net.URI;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author pashky
 */
public class AtlasStructuredQueryServiceTest extends AbstractOnceIndexTest {

    AtlasStructuredQueryService service;

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
        efvService.setSolrServerExpt(expt);
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
}
