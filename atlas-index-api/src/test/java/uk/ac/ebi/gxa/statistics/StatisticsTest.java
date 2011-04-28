package uk.ac.ebi.gxa.statistics;

import com.google.common.collect.HashMultiset;
import it.uniroma3.mat.extendedset.ConciseSet;
import org.junit.Test;
import uk.ac.ebi.gxa.utils.CollectionUtil;

import java.io.*;
import java.util.Collections;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class StatisticsTest {
    @Test
    public void testAddStatistics() throws InterruptedException {
        Statistics statistics = new Statistics();
        statistics.addStatistics(1, 22, asList(1, 5, 6, 9));
        waitForBackgroundProcesses();
        assertEquals(newHashSet(22), statistics.getScoringExperiments());
        assertEquals(newHashSet(22), statistics.getExperimentsForBioEntityAndAttribute(1, 9));
        assertEquals(CollectionUtil.<Integer, ConciseSet>makeMap(22, new ConciseSet(asList(1, 5, 6, 9))),
                statistics.getStatisticsForAttribute(1));
        assertEquals(newHashSet(1), statistics.getAttributes());
    }

    @Test
    public void testAddStatisticsInSteps() throws InterruptedException {
        Statistics statistics = new Statistics();
        statistics.addStatistics(1, 22, asList(1, 9));
        statistics.addStatistics(1, 22, asList(5, 6));
        waitForBackgroundProcesses();
        assertEquals(newHashSet(22), statistics.getScoringExperiments());
        assertEquals(newHashSet(22), statistics.getExperimentsForBioEntityAndAttribute(1, 9));
        assertEquals(CollectionUtil.<Integer, ConciseSet>makeMap(22, new ConciseSet(asList(1, 5, 6, 9))),
                statistics.getStatisticsForAttribute(1));
        assertEquals(newHashSet(1), statistics.getAttributes());
    }

    @Test
    public void testAddBioEntitiesForEfv() throws InterruptedException {
        Statistics statistics = new Statistics();
        statistics.addBioEntitiesForEfvAttribute(1, asList(1, 2, 3, 999));
        waitForBackgroundProcesses();
        assertEquals(4, statistics.getBioEntityCountForAttribute(1));
    }

    @Test
    public void testScoringEfvs() throws InterruptedException {
        Statistics statistics = new Statistics();
        statistics.addBioEntitiesForEfvAttribute(1, asList(1, 3, 999));
        statistics.addBioEntitiesForEfvAttribute(2, asList(1, 2, 999));
        waitForBackgroundProcesses();
        assertEquals(3, statistics.getBioEntityCountForAttribute(1));
        assertEquals(3, statistics.getBioEntityCountForAttribute(2));
        assertEquals(newHashSet(1, 2), statistics.getScoringEfvAttributesForBioEntity(1));
        assertEquals(newHashSet(2), statistics.getScoringEfvAttributesForBioEntity(2));
        assertEquals(newHashSet(1), statistics.getScoringEfvAttributesForBioEntity(3));
        assertEquals(newHashSet(1, 2), statistics.getScoringEfvAttributesForBioEntity(999));
        assertEquals(Collections.<Integer>emptySet(), statistics.getScoringEfvAttributesForBioEntity(1000));
    }

    @Test
    public void testScoringEfs() throws InterruptedException {
        Statistics statistics = new Statistics();
        statistics.addBioEntitiesForEfAttribute(1, asList(1, 3, 999));
        statistics.addBioEntitiesForEfAttribute(2, asList(1, 2));
        statistics.addBioEntitiesForEfAttribute(2, asList(1, 999));
        waitForBackgroundProcesses();
        assertEquals(newHashSet(1, 2), statistics.getScoringEfAttributesForBioEntity(1));
        assertEquals(newHashSet(2), statistics.getScoringEfAttributesForBioEntity(2));
        assertEquals(newHashSet(1), statistics.getScoringEfAttributesForBioEntity(3));
        assertEquals(newHashSet(1, 2), statistics.getScoringEfAttributesForBioEntity(999));
        assertEquals(Collections.<Integer>emptySet(), statistics.getScoringEfAttributesForBioEntity(1000));
    }

    @Test
    public void testPValTStat() throws InterruptedException {
        Statistics statistics = new Statistics();
        statistics.addPvalueTstatRank(1, 0.25f, (short) 2, 22, 6);
        statistics.addPvalueTstatRank(1, 0.25f, (short) 2, 22, 7);
        waitForBackgroundProcesses();

        assertEquals(CollectionUtil.<Integer, ConciseSet>makeMap(22, new ConciseSet(asList(6, 7))),
                statistics.getPvalsTStatRanksForAttribute(1).get(new PvalTstatRank(0.25f, (short) 2)));
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException, InterruptedException {
        Statistics statistics = new Statistics();
        statistics.addBioEntitiesForEfvAttribute(1, asList(1, 2, 3, 999));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new ObjectOutputStream(baos).writeObject(statistics);
        Statistics s1 = (Statistics) new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject();

        assertEquals(4, s1.getBioEntityCountForAttribute(1));
        s1.addBioEntitiesForEfvAttribute(1, asList(452));
        waitForBackgroundProcesses();
        assertEquals(5, s1.getBioEntityCountForAttribute(1));
    }

    @Test
    public void testScoreAccessors() {
        Statistics statistics = new Statistics();
        HashMultiset<Integer> scores = HashMultiset.create();
        statistics.setScoresAcrossAllEfos(scores);
        assertSame(scores, statistics.getScoresAcrossAllEfos());

        HashMultiset<Integer> scores2 = HashMultiset.create();
        statistics.setScoresAcrossAllEfos(scores2);
        assertSame(scores2, statistics.getScoresAcrossAllEfos());
    }

    private static void waitForBackgroundProcesses() throws InterruptedException {
        Thread.sleep(10);
    }
}
