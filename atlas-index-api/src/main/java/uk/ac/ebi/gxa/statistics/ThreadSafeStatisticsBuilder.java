package uk.ac.ebi.gxa.statistics;

import com.google.common.collect.Multiset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ThreadSafe
public class ThreadSafeStatisticsBuilder implements StatisticsBuilder {
    private static final Logger log = LoggerFactory.getLogger(ThreadSafeStatisticsBuilder.class);

    @GuardedBy("threadLocals")
    private final List<Statistics[]> threadLocals = new ArrayList<Statistics[]>();

    private final ThreadLocal<Statistics[]> statistics = new ThreadLocal<Statistics[]>() {
        @Override
        protected synchronized Statistics[] initialValue() {
            final Statistics[] result = new Statistics[]{new Statistics()};
            synchronized (threadLocals) {
                threadLocals.add(result);
            }
            return result;
        }
    };

    @Override
    public void addStatistics(final EfvAttribute attributeIndex, final ExperimentInfo experimentIndex, final Collection<Integer> bioEntityIds) {
        get().addStatistics(attributeIndex, experimentIndex, bioEntityIds);
    }

    @Override
    public void addBioEntitiesForEfAttribute(final EfvAttribute attributeIndex, final Collection<Integer> bioEntityIds) {
        get().addBioEntitiesForEfAttribute(attributeIndex, bioEntityIds);
    }

    @Override
    public void addBioEntitiesForEfvAttribute(final EfvAttribute attributeIndex, final Collection<Integer> bioEntityIds) {
        get().addBioEntitiesForEfvAttribute(attributeIndex, bioEntityIds);
    }

    @Override
    public void setScoresAcrossAllEfos(final Multiset<Integer> scores) {
        get().setScoresAcrossAllEfos(scores);
    }

    @Override
    public void addPvalueTstatRank(final EfvAttribute attributeIndex, final PTRank ptRank, final ExperimentInfo experimentIndex, final Integer bioEntityId) {
        get().addPvalueTstatRank(attributeIndex, ptRank, experimentIndex, bioEntityId);
    }

    private Statistics get() {
        return statistics.get()[0];
    }

    @Override
    public Statistics getStatistics() {
        synchronized (threadLocals) {
            if (threadLocals.isEmpty())
                return new Statistics();

            Statistics result = threadLocals.get(0)[0];
            for (int i = 1; i < threadLocals.size(); i++) {
                final Statistics[] s = threadLocals.get(i);
                result.addAll(s[0]);
                s[0] = null;
            }
            return result;
        }
    }
}
