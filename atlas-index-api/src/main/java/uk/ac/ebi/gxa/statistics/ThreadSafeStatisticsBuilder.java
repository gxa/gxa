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
    public void addStatistics(final EfvAttribute attribute, final ExperimentInfo experiment, final Collection<Integer> bioEntityIds) {
        get().addStatistics(attribute, experiment, bioEntityIds);
    }

    @Override
    public void addBioEntitiesForEfAttribute(final EfvAttribute attribute, final Collection<Integer> bioEntityIds) {
        get().addBioEntitiesForEfAttribute(attribute, bioEntityIds);
    }

    @Override
    public void addBioEntitiesForEfvAttribute(final EfvAttribute attribute, final Collection<Integer> bioEntityIds) {
        get().addBioEntitiesForEfvAttribute(attribute, bioEntityIds);
    }

    @Override
    public void setScoresAcrossAllEfos(final Multiset<Integer> scores) {
        get().setScoresAcrossAllEfos(scores);
    }

    @Override
    public void addPvalueTstatRank(final EfvAttribute attribute, final PTRank ptRank, final ExperimentInfo experiment, final Integer bioEntityId) {
        get().addPvalueTstatRank(attribute, ptRank, experiment, bioEntityId);
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
