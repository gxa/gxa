package uk.ac.ebi.gxa.statistics;

import uk.ac.ebi.gxa.efo.Efo;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;
import static uk.ac.ebi.gxa.utils.EscapeUtil.encode;

/**
 * Serializable representation of an experiment factor for the purpose of ConciseSet storage.
 * This class also represents an ef at bit index query time.
 * <p/>
 */
public class EfAttribute extends Attribute implements Serializable {

    private static final long serialVersionUID = 2725738600257154348L;

    // Flag used in getEfvExperimentMappings() to indicate that this EfAttribute trivially maps to itself across all
    // experiments (c.f. same method in EfoAttribute)
    public static final ExperimentInfo ALL_EXPERIMENTS = null;
    private final String ef;

    /**
     * Constructor used for ef object stored in bit index
     *
     * @param ef an experiment factor name
     */
    public EfAttribute(@Nonnull final String ef) {
        this.ef = ef;
    }

    public String getEf() {
        return ef;
    }

    @Override
    public String getValue() {
        return ef;
    }

    /**
     * @param efo Efo
     * @return Set containing just this Attribute
     */
    @Override
    public Set<Attribute> getAttributeAndChildren(Efo efo) {
        return Collections.<Attribute>singleton(this);
    }

    /**
     * @param statisticsStorage - used to obtain indexes of attributes and experiments, needed finding experiment counts in bit index
     * @param allExpsToAttrs    Map: ExperimentInfo -> Set<Attribute> to which mappings this Attribute are to be added
     *                          Unlike for EfoAttribute, the only experiment key used is ALL_EXPERIMENTS_PLACEHOLDER
     */
    @Override
    public void getAttributeToExperimentMappings(
            final StatisticsStorage statisticsStorage,
            Map<ExperimentInfo, Set<EfAttribute>> allExpsToAttrs
    ) {
        if (!allExpsToAttrs.containsKey(ALL_EXPERIMENTS)) {
            allExpsToAttrs.put(ALL_EXPERIMENTS, new HashSet<EfAttribute>());
        }
        allExpsToAttrs.get(ALL_EXPERIMENTS).add(this);
    }
}
