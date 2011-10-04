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
 */
public class EfvAttribute extends EfAttribute implements Serializable {
    private static final String EF_EFV_SEP = "_";
    private static final long serialVersionUID = -4107638304283431910L;

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
        this.value = encodePair(getEf(), efv);
        return this;
    }
}
