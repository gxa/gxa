package uk.ac.ebi.gxa.statistics;

import com.google.common.collect.Multiset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.utils.GCFriendlyThreadLocal;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.List;

@ThreadSafe
public class ThreadSafeStatisticsBuilder implements StatisticsBuilder {
    private static final Logger log = LoggerFactory.getLogger(ThreadSafeStatisticsBuilder.class);

    /**
     * Special kind of {@link ThreadLocal}, it allows GC not to wait until the thread is gone.
     */
    private final GCFriendlyThreadLocal<Statistics> statistics = new GCFriendlyThreadLocal<Statistics>(Statistics.class);

    @Override
    public void addStatistics(final EfAttribute attribute, final ExperimentInfo experiment, final Collection<Integer> bioEntityIds) {
        get().addStatistics(attribute, experiment, bioEntityIds);
    }

    @Override
    public void addBioEntitiesForEfAttribute(final EfAttribute attribute, final Collection<Integer> bioEntityIds) {
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
    public void addPvalueTstatRank(final EfAttribute attribute, final PTRank ptRank, final ExperimentInfo experiment, final Integer bioEntityId) {
        get().addPvalueTstatRank(attribute, ptRank, experiment, bioEntityId);
    }

    private Statistics get() {
        return statistics.get();
    }

    @Override
    public Statistics getStatistics() {
        final List<Statistics> partialResults = statistics.getAll();
        if (partialResults.isEmpty())
            return new Statistics();

        // We don't want to create empty Statistics and copy it over, so we reuse the first one
        Statistics result = partialResults.get(0);
        for (int i = 1; i < partialResults.size(); i++) {
            final Statistics s = partialResults.get(i);
            result.addAll(s);
        }
        return result;
    }

    public void destroy() {
        statistics.destroyAndAllowGC();
    }
}
