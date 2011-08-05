package uk.ac.ebi.gxa.data;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static com.google.common.base.Predicates.or;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Collections2.transform;

/**
 * @author Alexey Filippov
 */
public class NetCDFPredicates {
    private final Logger log = LoggerFactory.getLogger(NetCDFPredicates.class);

    public Predicate<NetCDFProxy> hasArrayDesign(@Nonnull final String arrayDesign) {
        return new Predicate<NetCDFProxy>() {
            public boolean apply(@Nonnull NetCDFProxy proxy) {
                String adAcc = proxy.getArrayDesignAccession();
                return arrayDesign.equals(adAcc);
            }

            @Override
            public String toString() {
                return "HasArrayDesign(" + arrayDesign + ")";
            }
        };
    }

    public Predicate<NetCDFProxy> containsGenes(final Collection<Long> geneIds) {
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

            @Override
            public String toString() {
                return "ContainsGenes(" + geneIds + ")";
            }
        };
    }

    public Predicate<NetCDFProxy> containsAtLeastOneGene(final Collection<Long> geneIds) {
        return or(transform(geneIds, new Function<Long, Predicate<? super NetCDFProxy>>() {
            public Predicate<? super NetCDFProxy> apply(@Nonnull Long input) {
                return containsGenes(Arrays.asList(input));
            }
        }));
    }

    public Predicate<NetCDFProxy> containsEfEfv(final String ef, final String efv) {
        return isNullOrEmpty(efv) ?
                new Predicate<NetCDFProxy>() {
                    public boolean apply(@Nonnull NetCDFProxy input) {
                        try {
                            for (KeyValuePair uefv : input.getUniqueFactorValues()) {
                                if (uefv.key.equals(ef))
                                    return true;
                            }
                            return false;
                        } catch (IOException e) {
                            log.error("Cannot read NetCDF proxy " + input, e);
                            return false;
                        }
                    }

                    @Override
                    public String toString() {
                        return "HasEF(" + ef + ")";
                    }
                } :
                new Predicate<NetCDFProxy>() {
                    public boolean apply(@Nonnull NetCDFProxy input) {
                        try {
                            for (KeyValuePair uefv : input.getUniqueFactorValues()) {
                                if (uefv.key.equals(ef) && uefv.value.equals(efv)) {
                                    return true;
                                }
                            }
                            return false;
                        } catch (IOException e) {
                            log.error("Cannot read NetCDF proxy " + input, e);
                            return false;
                        }
                    }

                    @Override
                    public String toString() {
                        return "HasEFV(" + ef + "||" + efv + ")";
                    }
                };
    }
}
