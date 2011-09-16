package uk.ac.ebi.gxa.statistics;

import uk.ac.ebi.gxa.efo.Efo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;
import static uk.ac.ebi.gxa.utils.EscapeUtil.encode;

/**
 * Serializable representation of ef-efv for the purpose of ConciseSet storage.
 * This class also represents ef-efvs at bit index query time.
 * <p/>
 * TODO: Ticket #3109 to split this class into EfAttribute and EfvAttribute - to separate its currently dual usage for storing ef-efv as well as ef only
 */
public class EfvAttribute extends Attribute implements Serializable {
    private static final long serialVersionUID = -8117173650721973734L;

    // Flag used in getEfvExperimentMappings() to indicate that this EfvAttribute trivially maps to itself across all
    // experiments (c.f. same method in EfoAttribute)
    public static final ExperimentInfo ALL_EXPERIMENTS_PLACEHOLDER = null;
    private static final String EF_EFV_SEP = "_";

    private final String ef;
    private final String efv;
    private transient String value;

    /**
     * Constructor used for ef object stored in bit index
     *
     * @param ef  an experiment factor name
     */
    public EfvAttribute(@Nonnull final String ef) {
        this(ef, null);
    }

    /**
     * Constructor used for ef-efv tuple stored in bit index
     *
     * @param ef an experiment factor name
     * @param efv an experiment factor value
     */
    public EfvAttribute(@Nonnull final String ef, @Nullable final String efv) {
        this.ef = ef;
        this.efv = efv;
        this.value = encodePair(ef, efv);
    }

    public String getEf() {
        return ef;
    }

    public String getEfv() {
        return efv;
    }

    @Override
    public String getValue() {
        return value;
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
     * @param allExpsToAttrs    Map: ExperimentInfo -> Set<Attribute> to which mappings for efo term represented by this Attribute are to be added
     *                          Unlike for EfoAttribute, the only experiment key used is ALL_EXPERIMENTS_PLACEHOLDER
     */
    @Override
    public void getEfvExperimentMappings(
            final StatisticsStorage statisticsStorage,
            Map<ExperimentInfo, Set<EfvAttribute>> allExpsToAttrs
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

    /**
     * Restores the transient fields on deserialization
     *
     * @return <code>this</code>
     * @throws ObjectStreamException in case of I/O errors during deserialization
     */
    private Object readResolve() throws ObjectStreamException {
        this.value = encodePair(ef, efv);
        return this;
    }
}
