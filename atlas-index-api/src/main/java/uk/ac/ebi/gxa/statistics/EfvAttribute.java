package uk.ac.ebi.gxa.statistics;

import com.google.common.base.Strings;

import javax.annotation.Nonnull;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * Serializable representation of ef-efv for the purpose of ConciseSet storage.
 * This class also represents ef-efvs at bit index query time.
 * <p/>
 */
public class EfvAttribute extends EfAttribute implements Serializable {
    private static final String EF_EFV_SEP = "_";
    private static final long serialVersionUID = -4674735573391531209L;

    private final String efv;
    private transient String value;

    /**
     * Constructor used for ef-efv tuple stored in bit index
     *
     * @param ef  an experiment factor name
     * @param efv an experiment factor value
     */
    public EfvAttribute(@Nonnull final String ef, @Nonnull final String efv) {
        super(ef);
        this.efv = efv;
        this.value = encodePair(ef, efv);
    }

    public String getEfv() {
        return efv;
    }

    @Override
    public String getValue() {
        return value;
    }

    private static String encodePair(@Nonnull final String ef, @Nonnull final String efv) {
        return !Strings.isNullOrEmpty(ef) && !Strings.isNullOrEmpty(efv) ? ef + EF_EFV_SEP + efv : null;
    }

    /**
     * Restores the transient fields on deserialization
     *
     * @return <code>this</code>
     * @throws ObjectStreamException in case of I/O errors during deserialization
     */
    private Object readResolve() throws ObjectStreamException {
        this.value = encodePair(getEf(), efv);
        return this;
    }
}
