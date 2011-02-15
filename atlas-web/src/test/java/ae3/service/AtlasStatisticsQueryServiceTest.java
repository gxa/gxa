package ae3.service;

import ae3.model.AtlasGene;
import com.google.common.collect.Multiset;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.index.StatisticsStorageFactory;
import uk.ac.ebi.gxa.statistics.*;

import java.io.File;
import java.util.*;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: 2/7/11
 * Time: 12:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class AtlasStatisticsQueryServiceTest {

    private static AtlasStatisticsQueryService atlasStatisticsQueryService;
    private long geneId;
    private Attribute hematopoieticCellEfo;
    private Attribute hematopoieticStemCellEfo;
    private Attribute hematopoieticStemCellEfv;
    private Experiment E_GEOD_1493;

    static {
        try {
            String bitIndexResourceName = "bitstats";
            File bitIndexResourcePath = new File(AtlasGene.class.getClassLoader().getResource(bitIndexResourceName).toURI());
            StatisticsStorageFactory statisticsStorageFactory = new StatisticsStorageFactory(bitIndexResourceName);
            statisticsStorageFactory.setAtlasIndex(new File(bitIndexResourcePath.getParent()));
            StatisticsStorage statisticsStorage = statisticsStorageFactory.createStatisticsStorage();
            atlasStatisticsQueryService = new AtlasBitIndexQueryService(bitIndexResourceName);
            atlasStatisticsQueryService.setStatisticsStorage(statisticsStorage);
        } catch (Exception e) {
        }
    }

    @Before
    public void initGene() throws Exception {
        geneId = 169968252l;  // identifier: ENSMUSG00000020275; name: Rel)
        hematopoieticStemCellEfo = new Attribute("EFO_0000527", StatisticsQueryUtils.EFO, StatisticsType.UP_DOWN);
        hematopoieticCellEfo = new Attribute("EFO_0002436", StatisticsQueryUtils.EFO, StatisticsType.UP_DOWN);
        hematopoieticStemCellEfv = new Attribute("cell_type", "hematopoietic stem cell");
        E_GEOD_1493 = new Experiment("E-GEOD-1493", "570556674");
    }


    @Test
    public void test_getExperimentCountsForGene() {

        Efo efo = EasyMock.createMock(Efo.class);
        atlasStatisticsQueryService.setEfo(efo);
        EasyMock.expect(efo.getTermAndAllChildrenIds(EasyMock.eq(hematopoieticCellEfo.getValue()))).andReturn(Collections.<String>singleton(hematopoieticStemCellEfo.getValue()));
        EasyMock.replay(efo);
        int upExpCount = atlasStatisticsQueryService.getExperimentCountsForGene(
                hematopoieticCellEfo.getValue(), StatisticsType.UP, StatisticsQueryUtils.EFO, geneId);

        efo = EasyMock.createMock(Efo.class);
        atlasStatisticsQueryService.setEfo(efo);
        EasyMock.expect(efo.getTermAndAllChildrenIds(EasyMock.eq(hematopoieticCellEfo.getValue()))).andReturn(Collections.<String>singleton(hematopoieticStemCellEfo.getValue()));
        EasyMock.replay(efo);
        int downExpCount = atlasStatisticsQueryService.getExperimentCountsForGene(
                hematopoieticCellEfo.getValue(), StatisticsType.DOWN, StatisticsQueryUtils.EFO, geneId);

        efo = EasyMock.createMock(Efo.class);
        atlasStatisticsQueryService.setEfo(efo);
        EasyMock.expect(efo.getTermAndAllChildrenIds(EasyMock.eq(hematopoieticCellEfo.getValue()))).andReturn(Collections.<String>singleton(hematopoieticStemCellEfo.getValue()));
        EasyMock.replay(efo);
        int nonDEExpCount = atlasStatisticsQueryService.getExperimentCountsForGene(
                hematopoieticCellEfo.getValue(), StatisticsType.NON_D_E, StatisticsQueryUtils.EFO, geneId);


        assertEquals(2, upExpCount);
        assertEquals(0, downExpCount);
        assertEquals(4, nonDEExpCount);

        upExpCount = atlasStatisticsQueryService.getExperimentCountsForGene(
                hematopoieticStemCellEfv.getValue(),
                StatisticsType.UP,
                !StatisticsQueryUtils.EFO,
                geneId);

        assertEquals(2, upExpCount);

        // Test restricting query with geneId
        upExpCount = atlasStatisticsQueryService.getExperimentCountsForGene(
                hematopoieticStemCellEfv.getValue(),
                StatisticsType.UP,
                !StatisticsQueryUtils.EFO,
                geneId, Collections.singleton(geneId), null);

        assertEquals(2, upExpCount);

        // Test restricting query with a different geneId
        upExpCount = atlasStatisticsQueryService.getExperimentCountsForGene(
                hematopoieticStemCellEfv.getValue(),
                StatisticsType.UP,
                !StatisticsQueryUtils.EFO,
                geneId, Collections.singleton(geneId - 1), null);

        // Even though the search for gene id does not match gene id, the geneIndex inside the test bit index contains only geneId,
        // hence gene restriction set in which no genes can be found in geneIndex has no effect on the query.
        assertEquals(2, upExpCount);
    }

    @Test

    public void test_getStatisticsOrQuery() {

        Efo efo = EasyMock.createMock(Efo.class);
        atlasStatisticsQueryService.setEfo(efo);
        EasyMock.expect(efo.getTermAndAllChildrenIds(EasyMock.eq(hematopoieticCellEfo.getValue()))).andReturn(Collections.<String>singleton(hematopoieticStemCellEfo.getValue()));
        EasyMock.replay(efo);

        List<Attribute> orAttributes = new ArrayList<Attribute>();
        orAttributes.add(hematopoieticCellEfo);
        orAttributes.add(hematopoieticStemCellEfv);

        StatisticsQueryOrConditions<StatisticsQueryCondition> orConditions = atlasStatisticsQueryService.getStatisticsOrQuery(orAttributes);
        Set<StatisticsQueryCondition> conditions = orConditions.getConditions();
        assertEquals(21, conditions.size());

        boolean foundMapping = false;
        for (StatisticsQueryCondition condition : conditions) {
            Set<Attribute> attrs = condition.getAttributes();
            Set<Experiment> exps = condition.getExperiments();
            if (attrs.contains(hematopoieticStemCellEfv) && !exps.isEmpty() && exps.contains(E_GEOD_1493))
                foundMapping = true;
        }
        assertTrue(foundMapping);
    }

    @Test
    public void test_getExperimentsSortedByPvalueTRank() {

        List<Experiment> list = atlasStatisticsQueryService.getExperimentsSortedByPvalueTRank(
                geneId, StatisticsType.UP_DOWN, null, null, !StatisticsQueryUtils.EFO, -1, -1);
        assertNotNull(list);
        assertTrue(list.size() > 0);
        Experiment bestExperiment = list.get(0);
        assertNotNull(bestExperiment.getHighestRankAttribute());
        assertNotNull(bestExperiment.getHighestRankAttribute().getEf());

        List<Experiment> list2 = atlasStatisticsQueryService.getExperimentsSortedByPvalueTRank(
                geneId, StatisticsType.UP_DOWN, null, null, !StatisticsQueryUtils.EFO, 1, 5);
        assertNotNull(list2);
        assertEquals(5, list2.size());

        List<Experiment> list3 = atlasStatisticsQueryService.getExperimentsSortedByPvalueTRank(
                geneId, StatisticsType.UP_DOWN, "organism_part", "liver", !StatisticsQueryUtils.EFO, -1, -1);
        assertNotNull(list3);
        assertTrue(list3.size() > 0);
    }

    @Test
    public void getExperimentsForGeneAndEf() {
        assertTrue(atlasStatisticsQueryService.getExperimentsForGeneAndEf(geneId, null, StatisticsType.UP_DOWN).size() > 0);
    }

    @Test
    public void test_getIndexForGene() {
        assertEquals(new Integer(1), atlasStatisticsQueryService.getIndexForGene(geneId));
    }

    @Test
    public void test_getAttributeForIndex() {
        assertEquals(hematopoieticStemCellEfv,
                atlasStatisticsQueryService.getAttributeForIndex(atlasStatisticsQueryService.getIndexForAttribute(hematopoieticStemCellEfv)));
    }

    @Test
    public void test_getSortedGenes() {
        List<Long> sortedGenesChunk = new ArrayList<Long>();

        Efo efo = EasyMock.createMock(Efo.class);
        atlasStatisticsQueryService.setEfo(efo);
        EasyMock.expect(efo.getTermAndAllChildrenIds(EasyMock.eq(hematopoieticCellEfo.getValue()))).andReturn(Collections.<String>singleton(hematopoieticStemCellEfo.getValue()));
        EasyMock.replay(efo);

        // Set up query
        StatisticsQueryCondition statsQuery = new StatisticsQueryCondition(StatisticsType.UP_DOWN);
        statsQuery.and(atlasStatisticsQueryService.getStatisticsOrQuery(Collections.singletonList(hematopoieticCellEfo)));
        atlasStatisticsQueryService.getSortedGenes(statsQuery, 0, 5, sortedGenesChunk);
        assertEquals(1, sortedGenesChunk.size());
        assertTrue(sortedGenesChunk.contains(geneId));
    }


    @Test
    public void test_getScoringAttributesForGenes() {

        List<Multiset.Entry<Integer>> scoringAttrCounts = atlasStatisticsQueryService.getScoringAttributesForGenes(
                Collections.singleton(geneId),
                StatisticsType.UP_DOWN,
                Collections.singleton(hematopoieticStemCellEfv.getEf()));
        assertNotNull(scoringAttrCounts);
        assertEquals(27, scoringAttrCounts.size());
        for (Multiset.Entry<Integer> attrCount : scoringAttrCounts) {
            if (attrCount.getElement().equals(atlasStatisticsQueryService.getIndexForAttribute(hematopoieticStemCellEfv)))
                assertEquals(2, attrCount.getCount());
        }
    }

}
