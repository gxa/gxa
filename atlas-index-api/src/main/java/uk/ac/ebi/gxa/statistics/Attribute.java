package uk.ac.ebi.gxa.statistics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;

import static com.google.common.base.Strings.isNullOrEmpty;
import static uk.ac.ebi.gxa.utils.EscapeUtil.encode;

/**
 * Serializable representation of ef-efv for the purpose of ConciseSet storage
 */
public class Attribute implements Serializable {

    private static final long serialVersionUID = -328785464649820761L;

    private static final String EF_EFV_SEP = "_";

    private String value;
    private String ef;
    private String efv;
    private transient boolean isEfo;
    private transient StatisticsType statType;

    /**
     * Constructor used for ef-efv tuple stored in bit index
     *
     * @param ef
     * @param efv
     */
    public Attribute(@Nonnull final String ef, @Nullable final String efv) {
        this.ef = ef;
        this.efv = efv;
        this.value = encodePair(ef, efv);
    }

    /**
     * Constructor used for ef object stored in bit index
     *
     * @param ef
     */
    public Attribute(@Nonnull final String ef) {
        this(ef, null);
    }

    /**
     * Constructor used for efo terms at bit index query time
     *
     * TODO: having one class representing two contracts is going to bite us hard sooner or later.
     *
     * @param value
     * @param isEfo
     * @param statType
     */
    public Attribute(final String value, final boolean isEfo, final StatisticsType statType) {
        this.value = value.intern();
        this.isEfo = isEfo;
        this.statType = statType;
    }

    public String getValue() {
        return value;
    }

    public boolean isEfo() {
        return isEfo;
    }

    public String getEf() {
        return ef;
    }

    public String getEfv() {
        return efv;
    }

    public StatisticsType getStatType() {
        return statType;
    }

    public void setStatType(StatisticsType statType) {
        this.statType = statType;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Attribute attribute = (Attribute) o;

        if (value != null ? !value.equals(attribute.value) : attribute.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }

    private static String encodePair(String ef, String efv) {
        final String pair = isNullOrEmpty(efv) ? ef : ef + EF_EFV_SEP + efv;
        return encode(pair).intern();
    }
}
