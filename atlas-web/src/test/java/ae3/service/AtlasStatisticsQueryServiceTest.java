package ae3.service;

import ae3.model.AtlasGene;
import com.google.common.collect.Multiset;
import it.uniroma3.mat.extendedset.ConciseSet;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.index.StatisticsStorageFactory;
import uk.ac.ebi.gxa.statistics.*;

import java.io.File;
import java.util.*;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;
import static uk.ac.ebi.gxa.exceptions.LogUtil.logUnexpected;

public class AtlasStatisticsQueryServiceTest {

    private static AtlasStatisticsQueryService atlasStatisticsQueryService;
    private static StatisticsStorage<Long> statisticsStorage;
    private long bioentityId;
    private Attribute hematopoieticCellEfo;
    private Attribute hematopoieticStemCellEfo;
    private EfvAttribute hematopoieticStemCellEfv;
    private ExperimentInfo E_MTAB_62;

    static {
        try {
            String bitIndexResourceName = "bitstats";
            File bitIndexResourcePath = new File(AtlasGene.class.getClassLoader().getResource(bitIndexResourceName).toURI());
            StatisticsStorageFactory statisticsStorageFactory = new StatisticsStorageFactory(bitIndexResourceName);
            statisticsStorageFactory.setAtlasIndex(new File(bitIndexResourcePath.getParent()));
            statisticsStorage = statisticsStorageFactory.createStatisticsStorage();
            atlasStatisticsQueryService = new AtlasBitIndexQueryService(bitIndexResourceName);
            atlasStatisticsQueryService.setStatisticsStorage(statisticsStorage);
        } catch (Exception e) {
            throw logUnexpected("Cannot init tests", e);
        }
    }

    @Before
    public void initGene() throws Exception {
        bioentityId = 838592l;  // identifier: ENSG00000162924; name: REL)
        hematopoieticStemCellEfo = new EfoAttribute("EFO_0000527", StatisticsType.UP_DOWN);
        hematopoieticCellEfo = new EfoAttribute("EFO_0002436", StatisticsType.UP_DOWN);
        hematopoieticStemCellEfv = new EfvAttribute("369_groups", "hematopoietic stem cell", StatisticsType.UP_DOWN);
        E_MTAB_62 = new ExperimentInfo("E-MTAB-62", 1036809468l);
    }


    @Test
    public void test_getExperimentCountsForGene() {

        Efo efo = EasyMock.createMock(Efo.class);
        atlasStatisticsQueryService.setEfo(efo);
        EasyMock.expect(efo.getTermAndAllChildrenIds(EasyMock.eq(hematopoieticCellEfo.getValue()), EasyMock.eq(Integer.MAX_VALUE))).andReturn(Collections.<String>singleton(hematopoieticStemCellEfo.getValue()));
        EasyMock.replay(efo);
        hematopoieticCellEfo.setStatType(StatisticsType.UP);
        int upExpCount = atlasStatisticsQueryService.getExperimentCountsForGene(hematopoieticCellEfo, bioentityId);

        efo = EasyMock.createMock(Efo.class);
        atlasStatisticsQueryService.setEfo(efo);
        EasyMock.expect(efo.getTermAndAllChildrenIds(EasyMock.eq(hematopoieticCellEfo.getValue()), EasyMock.eq(Integer.MAX_VALUE))).andReturn(Collections.<String>singleton(hematopoieticStemCellEfo.getValue()));
        EasyMock.replay(efo);
        hematopoieticCellEfo.setStatType(StatisticsType.DOWN);
        int downExpCount = atlasStatisticsQueryService.getExperimentCountsForGene(hematopoieticCellEfo, bioentityId);

        efo = EasyMock.createMock(Efo.class);
        atlasStatisticsQueryService.setEfo(efo);
        EasyMock.expect(efo.getTermAndAllChildrenIds(EasyMock.eq(hematopoieticCellEfo.getValue()), EasyMock.eq(Integer.MAX_VALUE))).andReturn(Collections.<String>singleton(hematopoieticStemCellEfo.getValue()));
        EasyMock.replay(efo);
        hematopoieticCellEfo.setStatType(StatisticsType.NON_D_E);
        int nonDEExpCount = atlasStatisticsQueryService.getExperimentCountsForGene(hematopoieticCellEfo, bioentityId);


        assertTrue(upExpCount > 0);
        assertEquals(0, downExpCount);
        assertTrue(nonDEExpCount > 0);

        hematopoieticStemCellEfv.setStatType(StatisticsType.UP);
        upExpCount = atlasStatisticsQueryService.getExperimentCountsForGene(
                hematopoieticStemCellEfv,
                bioentityId);

        assertEquals(1, upExpCount);

        // Test restricting query with bioentityId
        upExpCount = atlasStatisticsQueryService.getExperimentCountsForGene(
                hematopoieticStemCellEfv,
                bioentityId, Collections.singleton(bioentityId), null);

        assertEquals(1, upExpCount);

        // Test restricting query with a different bioentityId
        upExpCount = atlasStatisticsQueryService.getExperimentCountsForGene(
                hematopoieticStemCellEfv,
                bioentityId, Collections.singleton(bioentityId - 1), null);
        // Gene index contains more genes, but experiment counts are stored only for bioentityId, hence the expected result of 0
        assertEquals(0, upExpCount);
    }

    @Test

    public void test_getStatisticsOrQuery() {

        Efo efo = EasyMock.createMock(Efo.class);
        atlasStatisticsQueryService.setEfo(efo);
        EasyMock.expect(efo.getTermAndAllChildrenIds(EasyMock.eq(hematopoieticCellEfo.getValue()), EasyMock.eq(Integer.MAX_VALUE))).andReturn(Collections.<String>singleton(hematopoieticStemCellEfo.getValue()));
        EasyMock.replay(efo);

        List<Attribute> orAttributes = new ArrayList<Attribute>();
        orAttributes.add(hematopoieticCellEfo);
        orAttributes.add(hematopoieticStemCellEfv);

        StatisticsQueryOrConditions<StatisticsQueryCondition> orConditions = atlasStatisticsQueryService.getStatisticsOrQuery(orAttributes, 1);
        Set<StatisticsQueryCondition> conditions = orConditions.getConditions();
        assertTrue(conditions.size() > 0);

        boolean foundMapping = false;
        for (StatisticsQueryCondition condition : conditions) {
            Set<EfvAttribute> attrs = condition.getAttributes();
            Set<ExperimentInfo> exps = condition.getExperiments();
            if (attrs.contains(hematopoieticStemCellEfv) && !exps.isEmpty() && exps.contains(E_MTAB_62))
                foundMapping = true;
        }
        assertTrue(foundMapping);
    }

    @Test
    public void test_scoreQuery() {

        Efo efo = EasyMock.createMock(Efo.class);
        atlasStatisticsQueryService.setEfo(efo);
        EasyMock.expect(efo.getTermAndAllChildrenIds(EasyMock.eq(hematopoieticCellEfo.getValue()), EasyMock.eq(Integer.MAX_VALUE))).andReturn(Collections.<String>singleton(hematopoieticStemCellEfo.getValue()));
        EasyMock.replay(efo);

        StatisticsQueryCondition statsQuery = new StatisticsQueryCondition(StatisticsType.UP_DOWN);
        statsQuery.and(atlasStatisticsQueryService.getStatisticsOrQuery(Collections.singletonList(hematopoieticCellEfo), 1));

        Multiset<Integer> experimentCounts = StatisticsQueryUtils.scoreQuery(statsQuery, statisticsStorage, null);
        assertTrue(experimentCounts.entrySet().size() > 0);

        ConciseSet geneRestrictionIdxs = statisticsStorage.getIndexesForGeneIds(Collections.singleton(bioentityId));
        statsQuery.setGeneRestrictionSet(geneRestrictionIdxs); //ENSG00000162924
        Set<ExperimentInfo> scoringExps = new HashSet<ExperimentInfo>();
        experimentCounts = StatisticsQueryUtils.scoreQuery(statsQuery, statisticsStorage, scoringExps);
        assertEquals(0, experimentCounts.size());
        assertTrue(scoringExps.size() > 0);
    }

    @Test
    public void test_getScoresAcrossAllEfos() {
        Multiset<Integer> experimentCounts = StatisticsQueryUtils.getScoresAcrossAllEfos(StatisticsType.UP_DOWN, statisticsStorage);
        assertTrue(experimentCounts.entrySet().size() > 0);
        Integer bioentityIdx = statisticsStorage.getIndexForGeneId(bioentityId); //ENSG00000162924
        assertTrue(experimentCounts.contains(bioentityIdx));
        assertTrue(experimentCounts.count(bioentityIdx) > 0);

    }

    @Test
    public void getExperimentsForGeneAndAttribute() {
        assertTrue(atlasStatisticsQueryService.getExperimentsForGeneAndAttribute(bioentityId, null, StatisticsType.UP_DOWN).size() > 0);
    }

    @Test
    public void test_getIndexForGene() {
        assertNotNull(atlasStatisticsQueryService.getIndexForGene(bioentityId));
    }

    @Test
    public void test_getAttributeForIndex() {
        assertEquals(hematopoieticStemCellEfv,
                atlasStatisticsQueryService.getAttributeForIndex(atlasStatisticsQueryService.getIndexForAttribute(hematopoieticStemCellEfv)));
        // Test that objects returned from the index are in fact cloned
        assertFalse(atlasStatisticsQueryService.getAttributeForIndex(atlasStatisticsQueryService.getIndexForAttribute(hematopoieticStemCellEfv)) ==
                atlasStatisticsQueryService.getAttributeForIndex(atlasStatisticsQueryService.getIndexForAttribute(hematopoieticStemCellEfv)));
    }

    @Test
    public void test_getSortedGenes() {
        List<Long> sortedGenesChunk = new ArrayList<Long>();

        Efo efo = EasyMock.createMock(Efo.class);
        atlasStatisticsQueryService.setEfo(efo);
        EasyMock.expect(efo.getTermAndAllChildrenIds(EasyMock.eq(hematopoieticCellEfo.getValue()), EasyMock.eq(Integer.MAX_VALUE))).andReturn(Collections.<String>singleton(hematopoieticStemCellEfo.getValue()));
        EasyMock.replay(efo);

        // Set up query
        StatisticsQueryCondition statsQuery = new StatisticsQueryCondition(StatisticsType.UP_DOWN);
        statsQuery.and(atlasStatisticsQueryService.getStatisticsOrQuery(Collections.singletonList(hematopoieticCellEfo), 1));
        atlasStatisticsQueryService.getSortedGenes(statsQuery, 0, 5, new HashSet<Long>(), sortedGenesChunk);
        assertEquals(1, sortedGenesChunk.size());
        assertTrue(sortedGenesChunk.contains(bioentityId));
    }


    @Test
    public void test_getScoringAttributesForGenes() {

        List<Multiset.Entry<Integer>> scoringAttrCounts = atlasStatisticsQueryService.getScoringAttributesForGenes(
                Collections.singleton(bioentityId),
                StatisticsType.UP_DOWN,
                Collections.singleton(hematopoieticStemCellEfv.getEf()));
        assertNotNull(scoringAttrCounts);
        assertTrue(scoringAttrCounts.size() > 0);
        for (Multiset.Entry<Integer> attrCount : scoringAttrCounts) {
            if (attrCount.getElement().equals(atlasStatisticsQueryService.getIndexForAttribute(hematopoieticStemCellEfv)))
                assertEquals(1, attrCount.getCount());
        }
    }

    @Test
    public void test_getScoringExperimentsForGeneAndAttribute() {
        hematopoieticStemCellEfv.setStatType(StatisticsType.UP);
        Set<ExperimentInfo> experiments = atlasStatisticsQueryService.getScoringExperimentsForGeneAndAttribute(bioentityId, hematopoieticStemCellEfv);
        assertTrue(experiments.size() > 0);
        EfvAttribute attr = new EfvAttribute("cell_type", StatisticsType.UP);
        experiments = atlasStatisticsQueryService.getScoringExperimentsForGeneAndAttribute(bioentityId, attr);
        assertTrue(experiments.size() > 0);
    }

    @Test
    public void test_getAttributesForEfo() {
        Set<EfvAttribute> attrs = atlasStatisticsQueryService.getAttributesForEfo(hematopoieticStemCellEfo.getValue());
        assertTrue(attrs.size() > 0);
        for (EfvAttribute attr : attrs) {
            assertNotNull(attr.getEf());
            assertNotNull(attr.getEfv());
        }
    }

    /**
     * @param list
     * @return true if list is sorted in ASC order by experiments' pVal/tStatRanks
     */
    private boolean isSortedByPValTStatRank(List<ExperimentInfo> list) {
        boolean sorted = true;
        ExperimentInfo earlierExperiment = null;
        for (ExperimentInfo experiment : list) {
            assertNotNull(experiment.getpValTStatRank());
            if (earlierExperiment != null) {
                if (earlierExperiment.getpValTStatRank().compareTo(experiment.getpValTStatRank()) > 0) {
                    sorted = false;
                }
            }

            earlierExperiment = experiment;
        }
        return sorted;
    }

    @Test
    public void test_getExperimentsSortedByPvalueTRank() {
        EfvAttribute attr = new EfvAttribute(null, null);
        attr.setStatType(StatisticsType.UP_DOWN);

        List<ExperimentInfo> list = atlasStatisticsQueryService.getExperimentsSortedByPvalueTRank(bioentityId, attr, -1, -1);
        assertNotNull(list);
        assertTrue(list.size() > 0);
        ExperimentInfo bestExperiment = list.get(0);
        assertNotNull(bestExperiment.getHighestRankAttribute());
        assertNotNull(bestExperiment.getHighestRankAttribute().getEf());
        assertTrue(isSortedByPValTStatRank(list));

        List<ExperimentInfo> list2 = atlasStatisticsQueryService.getExperimentsSortedByPvalueTRank(bioentityId, attr, 0, 5);
        assertNotNull(list2);
        assertEquals(5, list2.size());
        assertTrue(isSortedByPValTStatRank(list2));

        attr = new EfvAttribute("organism_part", "liver", StatisticsType.UP_DOWN);
        List<ExperimentInfo> list3 = atlasStatisticsQueryService.getExperimentsSortedByPvalueTRank(bioentityId, attr, -1, -1);
        assertNotNull(list3);
        assertTrue(list3.size() > 0);
        assertTrue(isSortedByPValTStatRank(list3));
    }

    @Test
    public void test_getScoringEfsForGene() {
        List<String> scoringEfs = atlasStatisticsQueryService.getScoringEfsForGene(bioentityId, StatisticsType.UP_DOWN, null);
        assertTrue(scoringEfs.size() > 1);
        assertTrue(scoringEfs.contains("cell_type"));
        scoringEfs = atlasStatisticsQueryService.getScoringEfsForGene(bioentityId, StatisticsType.UP_DOWN, "cell_type");
        assertEquals(1, scoringEfs.size());
        assertTrue(scoringEfs.contains("cell_type"));
    }

    @Test
    public void test_getScoringEfvsForGene() {
        List<EfvAttribute> scoringEfvs = atlasStatisticsQueryService.getScoringEfvsForGene(bioentityId, StatisticsType.UP_DOWN);
        assertTrue(scoringEfvs.size() > 1);
        assertTrue(scoringEfvs.contains(hematopoieticStemCellEfv));
    }

    @Test
    public void test_getExperimentsForGeneAndEf() {
        List<ExperimentInfo> experiments =
                atlasStatisticsQueryService.getExperimentsForGeneAndAttribute(bioentityId, new EfvAttribute("cell_type", StatisticsType.UP_DOWN), StatisticsType.UP_DOWN);
        assertTrue(experiments.size() > 0);
        experiments = atlasStatisticsQueryService.getExperimentsForGeneAndAttribute(bioentityId, null, StatisticsType.UP_DOWN);
        assertTrue(experiments.size() > 1);
    }
}
