package uk.ac.ebi.gxa.statistics;

import com.google.common.collect.Multiset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.*;

import static java.util.concurrent.Executors.callable;
import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

@ThreadSafe
public class ThreadSafeStatisticsBuilder implements StatisticsBuilder {
    private static final Logger log = LoggerFactory.getLogger(ThreadSafeStatisticsBuilder.class);

    private final Statistics statistics = new Statistics();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Queue<Future<Object>> pending = new ConcurrentLinkedQueue<Future<Object>>();

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
            for (Future<Object> future : pending) {
                future.get();
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted, returning incomplete result", e);
            return statistics;
        } catch (ExecutionException e) {
            throw createUnexpected("Exception in statistics update", e.getCause());
        }
        return statistics;
    }

    private void enqueue(Runnable task) {
        pending.offer(executor.submit(callable(task)));

        // now we clean up the pending queue off the finished tasks
        Future<Object> future = pending.peek();
        while (future != null && future.isDone()) {
            future = pending.poll();
            if (future == null)
                return;
            if (!future.isDone()) {
                pending.offer(future);
                return;
            }
        }
    }
}
