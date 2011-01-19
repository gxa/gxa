package uk.ac.ebi.gxa.statistics;

import uk.ac.ebi.gxa.utils.EscapeUtil;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: Oct 26, 2010
 * Time: 5:31:40 PM
 * Serializable representation of ef-efv for the purpose of ConciseSet storage
 */
public class Attribute implements Serializable {

    private static final String EF_EFV_SEP = "_";

    private String value;
    private String ef;
    private String efv;
    private transient boolean isEfo;
    private transient StatisticsType statType;

    /**
     * Constructor used when for object stored in bit index
     * @param ef
     * @param efv
     */
    public Attribute(final String ef, final String efv) {
        this.ef = ef;
        this.efv = efv;
        this.value = EscapeUtil.encode(ef + (!efv.isEmpty() ? EF_EFV_SEP + efv : "")).intern();
    }

    /**
     * Constructor used for efo terms at bit index query time
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

    public void setEfo(boolean efo) {
        isEfo = efo;
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
}
