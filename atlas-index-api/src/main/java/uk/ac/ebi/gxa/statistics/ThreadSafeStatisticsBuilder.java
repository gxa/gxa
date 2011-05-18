package uk.ac.ebi.gxa.statistics;

import com.google.common.collect.Multiset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.concurrent.*;

import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

@ThreadSafe
public class ThreadSafeStatisticsBuilder implements StatisticsBuilder {
    private static final Logger log = LoggerFactory.getLogger(ThreadSafeStatisticsBuilder.class);

    private final Statistics statistics = new Statistics();
    private final ExecutorService executor = new ThreadPoolExecutor(1, 1, 1, TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(100, false), new BlockOnRejectedExecutionHandler());

    @Override
    public void addStatistics(final Integer attributeIndex, final Integer experimentIndex, final Collection<Integer> bioEntityIds) {
        enqueue(new Runnable() {
            @Override
            public void run() {
                statistics.addStatistics(attributeIndex, experimentIndex, bioEntityIds);
            }
        });
    }

    @Override
    public void addBioEntitiesForEfAttribute(final Integer attributeIndex, final Collection<Integer> bioEntityIds) {
        enqueue(new Runnable() {
            @Override
            public void run() {
                statistics.addBioEntitiesForEfAttribute(attributeIndex, bioEntityIds);
            }
        });
    }

    @Override
    public void addBioEntitiesForEfvAttribute(final Integer attributeIndex, final Collection<Integer> bioEntityIds) {
        enqueue(new Runnable() {
            @Override
            public void run() {
                statistics.addBioEntitiesForEfvAttribute(attributeIndex, bioEntityIds);
            }
        });
    }

    @Override
    public void setScoresAcrossAllEfos(final Multiset<Integer> scores) {
        enqueue(new Runnable() {
            @Override
            public void run() {
                statistics.setScoresAcrossAllEfos(scores);
            }
        });
    }

    @Override
    public void addPvalueTstatRank(final Integer attributeIndex, final Float pValue, final Short tStatRank, final Integer experimentIndex, final Integer bioEntityId) {
        enqueue(new Runnable() {
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

    private void enqueue(Runnable task) {
        executor.submit(task);
    }

    private static class BlockOnRejectedExecutionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            try {
                executor.getQueue().put(r);
            } catch (InterruptedException e) {
                throw createUnexpected("Interrupted: " + e.getMessage(), e);
            }
        }
    }
}
