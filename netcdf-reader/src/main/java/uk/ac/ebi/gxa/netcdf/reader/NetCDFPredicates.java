package uk.ac.ebi.gxa.netcdf.reader;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;

/**
 * @author Alexey Filippov
 */
public class NetCDFPredicates {
    public static final Logger log = LoggerFactory.getLogger(NetCDFPredicates.class);

    public static Predicate<NetCDFProxy> hasArrayDesign(@Nonnull final String arrayDesignAcc) {
        return new Predicate<NetCDFProxy>() {
            public boolean apply(@Nonnull NetCDFProxy proxy) {
                try {
                    String adAcc = proxy.getArrayDesignAccession();
                    return arrayDesignAcc.equals(adAcc);
                } catch (IOException e) {
                    log.error("Failed to retrieve data from proxy: " + proxy, e);
                    return false;
                }
            }
        };
    }

    public static Predicate<NetCDFProxy> containsGenes(final Collection<Long> geneIds) {
        if (geneIds == null || geneIds.isEmpty())
            return Predicates.alwaysTrue();

        return new Predicate<NetCDFProxy>() {
            public boolean apply(@Nonnull NetCDFProxy proxy) {
                try {
                    return proxy.getGeneIds().containsAll(geneIds);
                } catch (IOException e) {
                    log.error("Failed to retrieve data from proxy: " + proxy, e);
                    return false;
                }
            }
        };
    }

    public static Predicate<NetCDFProxy> containsEfEfv(final String ef, final String efv) {
        return new Predicate<NetCDFProxy>() {
            public boolean apply(@Nonnull NetCDFProxy input) {
                try {
                    final String pattern = ef + "||" + efv;
                    for (String uefv : input.getUniqueFactorValues()) {
                        if (uefv.equals(pattern))
                            return true;
                    }
                    return false;
                } catch (IOException e) {
                    log.error("Cannot read NetCDF proxy " + input, e);
                    return false;
                }
            }
        };
    }
}
