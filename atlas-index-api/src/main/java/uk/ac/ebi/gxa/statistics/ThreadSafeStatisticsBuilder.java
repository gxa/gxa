package uk.ac.ebi.gxa.statistics;

import com.google.common.collect.Multiset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

@ThreadSafe
public class ThreadSafeStatisticsBuilder implements StatisticsBuilder {
    private static final Logger log = LoggerFactory.getLogger(ThreadSafeStatisticsBuilder.class);

    private final Statistics statistics = new Statistics();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void addStatistics(final Integer attributeIndex, final Integer experimentIndex, final Collection<Integer> bioEntityIds) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                statistics.addStatistics(attributeIndex, experimentIndex, bioEntityIds);
            }
        });
    }

    @Override
    public void addBioEntitiesForEfAttribute(final Integer attributeIndex, final Collection<Integer> bioEntityIds) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                statistics.addBioEntitiesForEfAttribute(attributeIndex, bioEntityIds);
            }
        });
    }

    @Override
    public void addBioEntitiesForEfvAttribute(final Integer attributeIndex, final Collection<Integer> bioEntityIds) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                statistics.addBioEntitiesForEfvAttribute(attributeIndex, bioEntityIds);
            }
        });
    }

    @Override
    public void setScoresAcrossAllEfos(final Multiset<Integer> scores) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                statistics.setScoresAcrossAllEfos(scores);
            }
        });
    }

    @Override
    public void addPvalueTstatRank(final Integer attributeIndex, final Float pValue, final Short tStatRank, final Integer experimentIndex, final Integer bioEntityId) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                statistics.addPvalueTstatRank(attributeIndex, pValue, tStatRank, experimentIndex, bioEntityId);
            }
        });
    }

    @Override
    public Statistics getStatistics() {
        try {
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            return statistics;
        } catch (InterruptedException e) {
            throw createUnexpected("Interrupted shutdown", e);
        }
    }
}
