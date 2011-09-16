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
import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

public class AtlasStatisticsQueryServiceTest {

    private static AtlasStatisticsQueryService atlasStatisticsQueryService;
    private static StatisticsStorage statisticsStorage;
    private int bioEntityId;
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
            throw createUnexpected("Cannot init tests", e);
        }
    }

    @Before
    public void initGene() throws Exception {
        bioEntityId = 838592;  // identifier: ENSG00000162924; name: REL)
        hematopoieticStemCellEfo = new EfoAttribute("EFO_0000527");
        hematopoieticCellEfo = new EfoAttribute("EFO_0002436");
        hematopoieticStemCellEfv = new EfvAttribute("369_groups", "hematopoietic stem cell");
        E_MTAB_62 = new ExperimentInfo("E-MTAB-62", 1036809468l);
    }


    @Test
    public void test_getExperimentCountsForGene() {

        Efo efo = EasyMock.createMock(Efo.class);
        atlasStatisticsQueryService.setEfo(efo);
        EasyMock.expect(efo.getTermAndAllChildrenIds(EasyMock.eq(hematopoieticCellEfo.getValue()), EasyMock.eq(Integer.MAX_VALUE))).andReturn(Collections.<String>singleton(hematopoieticStemCellEfo.getValue()));
        EasyMock.replay(efo);
        int upExpCount = atlasStatisticsQueryService.getExperimentCountsForBioEntity(hematopoieticCellEfo, bioEntityId, StatisticsType.UP);

        efo = EasyMock.createMock(Efo.class);
        atlasStatisticsQueryService.setEfo(efo);
        EasyMock.expect(efo.getTermAndAllChildrenIds(EasyMock.eq(hematopoieticCellEfo.getValue()), EasyMock.eq(Integer.MAX_VALUE))).andReturn(Collections.<String>singleton(hematopoieticStemCellEfo.getValue()));
        EasyMock.replay(efo);
        int downExpCount = atlasStatisticsQueryService.getExperimentCountsForBioEntity(hematopoieticCellEfo, bioEntityId, StatisticsType.DOWN);

        efo = EasyMock.createMock(Efo.class);
        atlasStatisticsQueryService.setEfo(efo);
        EasyMock.expect(efo.getTermAndAllChildrenIds(EasyMock.eq(hematopoieticCellEfo.getValue()), EasyMock.eq(Integer.MAX_VALUE))).andReturn(Collections.<String>singleton(hematopoieticStemCellEfo.getValue()));
        EasyMock.replay(efo);
        int nonDEExpCount = atlasStatisticsQueryService.getExperimentCountsForBioEntity(hematopoieticCellEfo, bioEntityId, StatisticsType.NON_D_E);


        assertTrue(upExpCount > 0);
        assertEquals(0, downExpCount);
        assertTrue(nonDEExpCount > 0);

        upExpCount = atlasStatisticsQueryService.getExperimentCountsForBioEntity(
                hematopoieticStemCellEfv,
                bioEntityId,
                StatisticsType.UP);

        assertEquals(1, upExpCount);

        // Test restricting query with geneId
        upExpCount = atlasStatisticsQueryService.getExperimentCountsForBioEntity(
                hematopoieticStemCellEfv,
                bioEntityId, StatisticsType.UP, Collections.singleton(bioEntityId), null);

        assertEquals(1, upExpCount);

        // Test restricting query with a different geneId
        upExpCount = atlasStatisticsQueryService.getExperimentCountsForBioEntity(
                hematopoieticStemCellEfv,
                bioEntityId, StatisticsType.UP, Collections.singleton(bioEntityId - 1), null);
        // Gene index contains more genes, but experiment counts are stored only for geneId, hence the expected result of 0
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

        StatisticsQueryOrConditions<StatisticsQueryCondition> orConditions = atlasStatisticsQueryService.getStatisticsOrQuery(orAttributes, StatisticsType.UP_DOWN, 1);
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
        statsQuery.and(atlasStatisticsQueryService.getStatisticsOrQuery(Collections.singletonList(hematopoieticCellEfo), StatisticsType.UP_DOWN, 1));

        Multiset<Integer> experimentCounts = StatisticsQueryUtils.scoreQuery(statsQuery, statisticsStorage, null);
        assertTrue(experimentCounts.entrySet().size() > 0);

        statsQuery.setBioEntityIdRestrictionSet(Collections.singleton(bioEntityId));
        Set<ExperimentInfo> scoringExps = new HashSet<ExperimentInfo>();
        experimentCounts = StatisticsQueryUtils.scoreQuery(statsQuery, statisticsStorage, scoringExps);
        assertEquals(0, experimentCounts.size());
        assertTrue(scoringExps.size() > 0);
    }

    @Test
    public void test_getScoresAcrossAllEfos() {
        Multiset<Integer> experimentCounts = StatisticsQueryUtils.getScoresAcrossAllEfos(StatisticsType.UP_DOWN, statisticsStorage);
        assertTrue(experimentCounts.entrySet().size() > 0);
        assertTrue(experimentCounts.contains(bioEntityId));
        assertTrue(experimentCounts.count(bioEntityId) > 0);

    }

    @Test
    public void getExperimentsForGeneAndAttribute() {
        assertTrue(atlasStatisticsQueryService.getExperimentsForBioEntityAndAttribute(bioEntityId, null, StatisticsType.UP_DOWN).size() > 0);
    }


    @Test
    public void test_getSortedGenes() {
        List<Integer> sortedGenesChunk = new ArrayList<Integer>();

        Efo efo = EasyMock.createMock(Efo.class);
        atlasStatisticsQueryService.setEfo(efo);
        EasyMock.expect(efo.getTermAndAllChildrenIds(EasyMock.eq(hematopoieticCellEfo.getValue()), EasyMock.eq(Integer.MAX_VALUE))).andReturn(Collections.<String>singleton(hematopoieticStemCellEfo.getValue()));
        EasyMock.replay(efo);

        // Set up query
        StatisticsQueryCondition statsQuery = new StatisticsQueryCondition(StatisticsType.UP_DOWN);
        statsQuery.and(atlasStatisticsQueryService.getStatisticsOrQuery(Collections.singletonList(hematopoieticCellEfo), StatisticsType.UP_DOWN, 1));
        atlasStatisticsQueryService.getSortedBioEntities(statsQuery, 0, 5, new HashSet<Integer>(), sortedGenesChunk);
        assertTrue(sortedGenesChunk.size() > 0);
        assertTrue(sortedGenesChunk.contains(bioEntityId));
    }


    @Test
    public void test_getScoringAttributesForGenes() {

        List<Multiset.Entry<EfvAttribute>> scoringAttrCounts = atlasStatisticsQueryService.getScoringAttributesForBioEntities(
                Collections.singleton(bioEntityId),
                StatisticsType.UP_DOWN,
                Collections.singleton(hematopoieticStemCellEfv.getEf()));
        assertNotNull(scoringAttrCounts);
        assertTrue(scoringAttrCounts.size() > 0);
        for (Multiset.Entry<EfvAttribute> attrCount : scoringAttrCounts) {
            if (attrCount.getElement().equals(hematopoieticStemCellEfv))
                assertEquals(1, attrCount.getCount());
        }
    }

    @Test
    public void test_getScoringExperimentsForGeneAndAttribute() {
        Set<ExperimentInfo> experiments = atlasStatisticsQueryService.getScoringExperimentsForBioEntityAndAttribute(bioEntityId, hematopoieticStemCellEfv, StatisticsType.UP);
        assertTrue(experiments.size() > 0);
        EfvAttribute attr = new EfvAttribute("cell_type");
        experiments = atlasStatisticsQueryService.getScoringExperimentsForBioEntityAndAttribute(bioEntityId, attr, StatisticsType.UP);
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
    private boolean isSortedByPValTStatRank(List<ExperimentResult> list) {
        boolean sorted = true;
        ExperimentResult earlierExperiment = null;
        for (ExperimentResult experiment : list) {
            assertNotNull(experiment.getPValTStatRank());
            if (earlierExperiment != null) {
                if (earlierExperiment.getPValTStatRank().compareTo(experiment.getPValTStatRank()) > 0) {
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

        List<ExperimentResult> list = atlasStatisticsQueryService.getExperimentsSortedByPvalueTRank(bioEntityId, attr, -1, -1, StatisticsType.UP_DOWN);
        assertNotNull(list);
        assertTrue(list.size() > 0);
        ExperimentResult bestExperiment = list.get(0);
        assertNotNull(bestExperiment.getHighestRankAttribute());
        assertNotNull(bestExperiment.getHighestRankAttribute().getEf());
        assertTrue(isSortedByPValTStatRank(list));

        List<ExperimentResult> list2 = atlasStatisticsQueryService.getExperimentsSortedByPvalueTRank(bioEntityId, attr, 0, 5, StatisticsType.UP_DOWN);
        assertNotNull(list2);
        assertEquals(5, list2.size());
        assertTrue(isSortedByPValTStatRank(list2));

        attr = new EfvAttribute("organism_part", "liver");
        List<ExperimentResult> list3 = atlasStatisticsQueryService.getExperimentsSortedByPvalueTRank(bioEntityId, attr, -1, -1, StatisticsType.UP_DOWN);
        assertNotNull(list3);
        assertTrue(list3.size() > 0);
        assertTrue(isSortedByPValTStatRank(list3));
    }

    @Test
    public void test_getScoringEfsForGene() {
        List<String> scoringEfs = atlasStatisticsQueryService.getScoringEfsForBioEntity(bioEntityId, StatisticsType.UP_DOWN, null);
        assertTrue(scoringEfs.size() > 1);
        assertTrue(scoringEfs.contains("cell_type"));
        scoringEfs = atlasStatisticsQueryService.getScoringEfsForBioEntity(bioEntityId, StatisticsType.UP_DOWN, "cell_type");
        assertEquals(1, scoringEfs.size());
        assertTrue(scoringEfs.contains("cell_type"));
    }

    @Test
    public void test_getScoringEfvsForGene() {
        List<EfvAttribute> scoringEfvs = atlasStatisticsQueryService.getScoringEfvsForBioEntity(bioEntityId, StatisticsType.UP_DOWN);
        assertTrue(scoringEfvs.size() > 1);
        assertTrue(scoringEfvs.contains(hematopoieticStemCellEfv));
    }

    @Test
    public void test_getExperimentsForGeneAndEf() {
        List<ExperimentInfo> experiments =
                atlasStatisticsQueryService.getExperimentsForBioEntityAndAttribute(bioEntityId, new EfvAttribute("cell_type"), StatisticsType.UP_DOWN);
        assertTrue(experiments.size() > 0);
        experiments = atlasStatisticsQueryService.getExperimentsForBioEntityAndAttribute(bioEntityId, null, StatisticsType.UP_DOWN);
        assertTrue(experiments.size() > 1);
    }
}
