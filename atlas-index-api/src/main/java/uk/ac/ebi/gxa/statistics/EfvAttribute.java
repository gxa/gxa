package uk.ac.ebi.gxa.statistics;

import uk.ac.ebi.gxa.efo.Efo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;

import static com.google.common.base.Strings.isNullOrEmpty;
import static uk.ac.ebi.gxa.utils.EscapeUtil.encode;

/**
 * Serializable representation of ef-efv for the purpose of ConciseSet storage.
 * This class also represents ef-efvs at bit index query time.
 */
public class EfvAttribute extends Attribute implements Serializable {

    private static final long serialVersionUID = 6693676616189269260L;

    // Flag used in getEfvExperimentMappings() to indicate that this EfvAttribute trivially maps to itself across all
    // experiments (c.f. same mathod in EfoAttribute)
    public final static Experiment ALL_EXPERIMENTS_PLACEHOLDER = null;
    private static final String EF_EFV_SEP = "_";

    private String ef;
    private String efv;

    /**
     * Constructor used for ef object stored in bit index
     *
     * @param ef
     */
    public EfvAttribute(@Nonnull final String ef, StatisticsType statType) {
        this(ef, null, statType);
    }

    /**
     * Constructor used for ef-efv tuple stored in bit index
     *
     * @param ef
     * @param efv
     */
    public EfvAttribute(@Nonnull final String ef, @Nullable final String efv, @Nullable StatisticsType statType) {
        this.ef = ef;
        this.efv = efv;
        this.value = encodePair(ef, efv);
        if (statType != null)
            this.statType = statType;
    }

    public String getEf() {
        return ef;
    }

    public String getEfv() {
        return efv;
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
     * @param allExpsToAttrs    Map: Experiment -> Set<Attribute> to which mappings for efo term represented by this Attribute are to be added
     *                          Unlike for EfoAttribute, the only experiment key used is ALL_EXPERIMENTS_PLACEHOLDER
     */
    @Override
    public void getEfvExperimentMappings(
            final StatisticsStorage<Long> statisticsStorage,
            Map<Experiment, Set<EfvAttribute>> allExpsToAttrs
    ) {
        if (!allExpsToAttrs.containsKey(ALL_EXPERIMENTS_PLACEHOLDER)) {
            allExpsToAttrs.put(ALL_EXPERIMENTS_PLACEHOLDER, new HashSet<EfvAttribute>());
        }
        allExpsToAttrs.get(ALL_EXPERIMENTS_PLACEHOLDER).add(this);
    }


    public static String encodePair(String ef, String efv) {
        if (isNullOrEmpty(ef) && isNullOrEmpty(efv))
            return null;
        final String pair = isNullOrEmpty(efv) ? ef : ef + EF_EFV_SEP + efv;
        return encode(pair).intern();
    }


}
