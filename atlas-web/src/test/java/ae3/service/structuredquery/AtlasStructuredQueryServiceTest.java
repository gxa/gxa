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

import ae3.dao.GeneSolrDAO;
import ae3.service.AtlasBitIndexQueryService;
import ae3.service.AtlasStatisticsQueryService;
import com.google.common.collect.Multiset;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.gxa.dao.ExperimentDAO;
import uk.ac.ebi.gxa.dao.PropertyDAO;
import uk.ac.ebi.gxa.dao.PropertyValueDAO;
import uk.ac.ebi.gxa.data.AtlasDataDAO;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.efo.EfoImpl;
import uk.ac.ebi.gxa.exceptions.UnexpectedException;
import uk.ac.ebi.gxa.index.AbstractOnceIndexTest;
import uk.ac.ebi.gxa.index.StatisticsStorageFactory;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.properties.ResourceFileStorage;
import uk.ac.ebi.gxa.statistics.Attribute;
import uk.ac.ebi.gxa.statistics.EfvAttribute;
import uk.ac.ebi.gxa.statistics.StatisticsStorage;
import uk.ac.ebi.gxa.statistics.StatisticsType;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static uk.ac.ebi.gxa.statistics.StatisticsType.*;

/**
 * @author pashky
 */
public class AtlasStructuredQueryServiceTest extends AbstractOnceIndexTest {

    AtlasStructuredQueryService service;

    @Before
    public void createService() throws Exception {
        EmbeddedSolrServer solrServerAtlas = new EmbeddedSolrServer(getContainer(), "atlas");
        EmbeddedSolrServer serverProp = new EmbeddedSolrServer(getContainer(), "properties");

        ResourceFileStorage storage = new ResourceFileStorage();
        storage.setResourcePath("atlas.properties");
        AtlasProperties atlasProperties = new AtlasProperties();
        atlasProperties.setStorage(storage);

        Efo efo = new EfoImpl();
        efo.setUri(new URI("resource:META-INF/efo.owl"));

        GeneSolrDAO geneSolrDAO = new GeneSolrDAO();
        geneSolrDAO.setGeneSolr(solrServerAtlas);

        ExperimentDAO experimentDAO = createMock(ExperimentDAO.class);
        expect(experimentDAO.getById(anyLong())).andReturn(new Experiment(2L, "EXPERIMENT"));
        replay(experimentDAO);

        String bitIndexResourceName = "bitstats";
        File bitIndexResourcePath = new File(this.getClass().getClassLoader().getResource(bitIndexResourceName).toURI());
        StatisticsStorageFactory statisticsStorageFactory = new StatisticsStorageFactory(bitIndexResourceName);
        statisticsStorageFactory.setAtlasIndex(new File(bitIndexResourcePath.getParent()));
        StatisticsStorage statisticsStorage = statisticsStorageFactory.createStatisticsStorage();
        AtlasStatisticsQueryService atlasStatisticsQueryService = new AtlasBitIndexQueryService(bitIndexResourceName);
        atlasStatisticsQueryService.setStatisticsStorage(statisticsStorage);

        Property prop1 = Property.createProperty("organism_part");
        Property prop2 = Property.createProperty("disease_state");
        Property prop3 = Property.createProperty("cell_type");
        Property prop4 = Property.createProperty("cell_line");
        Property prop5 = Property.createProperty("compound");
        Property prop6 = Property.createProperty("developmental_stage");
        Property prop7 = Property.createProperty("infect");
        Property prop8 = Property.createProperty("phenotype");


        PropertyDAO propertyDAO = createMock(PropertyDAO.class);

        // Set DAS factors and example values
        expect(propertyDAO.getAll()).andReturn(Arrays.asList(prop1, prop2, prop3, prop4, prop5, prop6, prop7, prop8)).anyTimes();
        expect(propertyDAO.getByName("organism_part")).andReturn(prop1).anyTimes();
        expect(propertyDAO.getByName("disease_state")).andReturn(prop2).anyTimes();
        expect(propertyDAO.getByName("cell_type")).andReturn(prop2).anyTimes();
        expect(propertyDAO.getByName("cell_line")).andReturn(prop2).anyTimes();
        expect(propertyDAO.getByName("compound")).andReturn(prop2).anyTimes();
        expect(propertyDAO.getByName("developmental_stage")).andReturn(prop2).anyTimes();
        expect(propertyDAO.getByName("infect")).andReturn(prop2).anyTimes();
        expect(propertyDAO.getByName("phenotype")).andReturn(prop2).anyTimes();
        replay(propertyDAO);

        PropertyValueDAO propertyValueDAO = createMock(PropertyValueDAO.class);
        expect(propertyValueDAO.findValuesForProperty("organism_part")).andReturn(Collections.singletonList(new PropertyValue(1L, prop1, "heart"))).anyTimes();
        expect(propertyValueDAO.findValuesForProperty("disease_state")).andReturn(Collections.singletonList(new PropertyValue(1L, prop1, "breast carcinoma"))).anyTimes();
        expect(propertyValueDAO.findValuesForProperty("cell_type")).andReturn(Collections.singletonList(new PropertyValue(1L, prop1, "hematopoietic stem cell"))).anyTimes();
        expect(propertyValueDAO.findValuesForProperty("cell_line")).andReturn(Collections.singletonList(new PropertyValue(1L, prop1, "HeLa"))).anyTimes();
        expect(propertyValueDAO.findValuesForProperty("compound")).andReturn(Collections.singletonList(new PropertyValue(1L, prop1, "tamoxifen"))).anyTimes();
        expect(propertyValueDAO.findValuesForProperty("developmental_stage")).andReturn(Collections.singletonList(new PropertyValue(1L, prop1, "embryo"))).anyTimes();
        expect(propertyValueDAO.findValuesForProperty("infect")).andReturn(Collections.singletonList(new PropertyValue(1L, prop1, "uninfected"))).anyTimes();
        expect(propertyValueDAO.findValuesForProperty("phenotype")).andReturn(Collections.singletonList(new PropertyValue(1L, prop1, "not specified"))).anyTimes();
        replay(propertyValueDAO);

        AtlasEfvService efvService = new AtlasEfvService();
        efvService.setAtlasStatisticsQueryService(atlasStatisticsQueryService);
        efvService.setSolrServerProp(serverProp);
        efvService.setAtlasProperties(atlasProperties);
        efvService.setPropertyDAO(propertyDAO);
        efvService.setPropertyValueDAO(propertyValueDAO);

        AtlasEfoService efoService = new AtlasEfoService();
        efoService.setEfo(efo);
        efoService.setAtlasStatisticsQueryService(atlasStatisticsQueryService);

        AtlasGenePropertyService gpService = new AtlasGenePropertyService();
        gpService.setAtlasProperties(atlasProperties);
        gpService.setSolrServerAtlas(solrServerAtlas);

        AtlasDataDAO atlasDataDAO = new AtlasDataDAO();

        service = new AtlasStructuredQueryService();
        service.setSolrServerAtlas(solrServerAtlas);
        service.setSolrServerProp(serverProp);
        service.setExperimentDAO(experimentDAO);
        service.setEfoService(efoService);
        service.setEfvService(efvService);
        service.setEfo(efo);
        service.setAtlasProperties(atlasProperties);
        service.setGenePropService(gpService);
        service.setAtlasDataDAO(atlasDataDAO);
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
    public void test_doStructuredAtlasQueryWithCondition() {
        AtlasStructuredQuery query = new AtlasStructuredQueryBuilder()
                .andGene("ENSMUSG00000020275")
                .query(1);
        appendCondition(query);
        AtlasStructuredQueryResult result = service.doStructuredAtlasQuery(query);

        assertNotNull(result);
        assertTrue(result.getSize() > 0);
    }

    @Test(expected = UnexpectedException.class)
    public void test_doStructuredAtlasQueryWithNoConditions() {
        AtlasStructuredQuery query = new AtlasStructuredQueryBuilder()
                .andGene("ENSMUSG00000020275")
                .query(1);
        AtlasStructuredQueryResult result = service.doStructuredAtlasQuery(query);

        assertNotNull(result);
        assertTrue(result.getSize() > 0);
    }

    @Test
    public void test_doStructuredAtlasQueryWithEmptyCondition() {
        AtlasStructuredQuery query = new AtlasStructuredQueryBuilder()
                .andGene("ENSMUSG00000020275")
                .query(1);
        appendEmptyCondition(query);
        AtlasStructuredQueryResult result = service.doStructuredAtlasQuery(query);

        assertNotNull(result);
        assertTrue(result.getSize() > 0);
    }



    @Test
    public void test_getStats() {
        Map<StatisticsType, HashMap<String, Multiset<Integer>>> scoresCache = service.getScoresCache();

        int bioEntityId = 516248;  // identifier: ENSMUSG00000020275; name: Rel)
        Attribute hematopoieticStemCellEfv = new EfvAttribute("cell_type", "hematopoietic stem cell");
        boolean showNonDEData = true;
        UpdownCounter counter = service.getStats(scoresCache, hematopoieticStemCellEfv, bioEntityId,
                Collections.singleton(bioEntityId), showNonDEData);
        assertFalse(counter.isZero());
        assertTrue(counter.getNoStudies() > 0 || counter.getNones() > 0);
        assertTrue(counter.getMpvDn() != 1 || counter.getMpvUp() != 1); // At least one of up/down min pVals should have been populated

        // Now check that the counts were stored in cache
        Multiset<Integer> upCounts = service.getScoresFromCache(scoresCache, UP, hematopoieticStemCellEfv.getValue());
        Multiset<Integer> downCounts = service.getScoresFromCache(scoresCache, DOWN, hematopoieticStemCellEfv.getValue());
        Multiset<Integer> nonDECounts = service.getScoresFromCache(scoresCache, NON_D_E, hematopoieticStemCellEfv.getValue());
        assertTrue(upCounts.entrySet().size() > 0 || downCounts.entrySet().size() > 0 || nonDECounts.entrySet().size() > 0);
    }

    private void appendCondition(AtlasStructuredQuery query) {
        ExpFactorQueryCondition cond = new ExpFactorQueryCondition();
        cond.setFactor("organism_part");
        cond.setExpression(QueryExpression.UP_DOWN);
        cond.setFactorValues(Collections.singletonList("heart"));
        cond.setMinExperiments(1);
        query.setConditions(Collections.singletonList(cond));
    }

    private void appendEmptyCondition(AtlasStructuredQuery query) {
        ExpFactorQueryCondition cond = new ExpFactorQueryCondition();
        cond.setFactor("");
        cond.setExpression(QueryExpression.UP_DOWN);
        cond.setFactorValues(Collections.<String>emptyList());
        cond.setMinExperiments(1);
        query.setConditions(Collections.singletonList(cond));
    }
}
