package ae3.service;

import ae3.model.AtlasGene;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.gxa.index.StatisticsStorageFactory;
import uk.ac.ebi.gxa.statistics.Experiment;
import uk.ac.ebi.gxa.statistics.StatisticsStorage;
import uk.ac.ebi.gxa.statistics.StatisticsType;

import java.io.File;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AtlasStatisticsQueryServiceTest {

    private static AtlasStatisticsQueryService atlasStatisticsQueryService;
    private long geneId = 169968252l;

    static {
        try {
            String bitIndexResourceName = "bitstats";
            File bitIndexResourcePath = new File(AtlasGene.class.getClassLoader().getResource(bitIndexResourceName).toURI());
            StatisticsStorageFactory statisticsStorageFactory = new StatisticsStorageFactory(bitIndexResourceName);
            statisticsStorageFactory.setAtlasIndex(new File(bitIndexResourcePath.getParent()));
            StatisticsStorage<Long> statisticsStorage = statisticsStorageFactory.createStatisticsStorage();
            atlasStatisticsQueryService = new AtlasBitIndexQueryService(bitIndexResourceName);
            atlasStatisticsQueryService.setStatisticsStorage(statisticsStorage);
        } catch (Exception e) {
        }
    }

    @Before
    public void initGene() throws Exception {
        geneId = 169968252l;
    }

    @Test
    public void test_getExperimentsSortedByPvalueTRank() {

        List<Experiment> list = atlasStatisticsQueryService.getExperimentsSortedByPvalueTRank(
                geneId, StatisticsType.UP_DOWN, null, null, false, -1, -1);
        assertNotNull(list);
        assertTrue(list.size() > 0);
        Experiment bestExperiment = list.get(0);
        assertNotNull(bestExperiment.getHighestRankAttribute());
        assertNotNull(bestExperiment.getHighestRankAttribute().getEf());

        List<Experiment> list2 = atlasStatisticsQueryService.getExperimentsSortedByPvalueTRank(
                geneId, StatisticsType.UP_DOWN, null, null, false, 1, 5);
        assertNotNull(list2);
        assertEquals(5, list2.size());

        List<Experiment> list3 = atlasStatisticsQueryService.getExperimentsSortedByPvalueTRank(
                geneId, StatisticsType.UP_DOWN, "organism_part", "liver", false, -1, -1);
        assertNotNull(list3);
        assertTrue(list3.size() > 0);
    }

    @Test
    public void getExperimentsForGeneAndEf() {
        assertTrue(atlasStatisticsQueryService.getExperimentsForGeneAndEf(geneId, null, StatisticsType.UP_DOWN).size() > 0);
    }
}
