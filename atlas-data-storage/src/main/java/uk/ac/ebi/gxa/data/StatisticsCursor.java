/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *  
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.data;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.DesignElementStatistics;
import uk.ac.ebi.microarray.atlas.model.UpDownExpression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.BitSet;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

/**
 * A cursor used to navigate a NetCDF-stored statistics
 * <p/>
 * Hides parallel structures (DE, uEFV, P, T) from the programmer, hence making it harder to make an error.
 * <p/>
 * As a trade-off, it changes its own state as you go (hence the "Cursor"), and thus should not be stored anywhere
 * despite the <code>implements DesignElementStatistics</code> part.
 * Use it to read statistics sequentially, or make copies to store and pass further using a {@link #getSnapshot} method.
 *
 * @author alf
 */
public class StatisticsCursor implements DesignElementStatistics {
    public static final Predicate<Pair<String, String>> NON_EMPTY_EFV = new Predicate<Pair<String, String>>() {
        @Override
        public boolean apply(@Nullable Pair<String, String> input) {
            return input != null && !isNullOrEmpty(input.getValue()) && !"(empty)".equals(input.getValue());
        }
    };

    /**
     * the index of current design element's index. Refers to a position in {@link #des}
     * <p/>
     * TODO: replace me with an {@link java.util.Iterator}, please
     */
    private int dii = -1;
    /**
     * the index of current EFV index. Refers to a position in the original NetCDF and {@see #uEFVs}
     */
    private int efvi = -1;

    private final List<Pair<String, String>> uEFVs;
    private final long[] bioentities;
    private final FloatMatrixProxy tstat;
    private final FloatMatrixProxy pvals;
    private final String[] deAccessions;

    private final DataProxy dataProxy;
    private final Predicate<Long> bePredicate;
    private final Predicate<Pair<String, String>> efvPredicate;
    private final int[] des; // consideration: BitSet will do just fine and might even be cleaner
    private final String[] factors;
    private String[][] factorValues;

    StatisticsCursor(DataProxy dataProxy, Predicate<Long> bePredicate, Predicate<Pair<String, String>> efvPredicate)
            throws AtlasDataException, StatisticsNotFoundException {
        this(dataProxy, bePredicate, efvPredicate, null);
    }

    StatisticsCursor(DataProxy dataProxy, Predicate<Pair<String, String>> efvPredicate, int de)
            throws AtlasDataException, StatisticsNotFoundException {
        this(dataProxy, ANY_KNOWN_GENE, efvPredicate, new int[]{de});
    }

    StatisticsCursor(DataProxy dataProxy, Predicate<Long> bePredicate, Predicate<Pair<String, String>> efvPredicate, int[] des)
            throws AtlasDataException, StatisticsNotFoundException {
        this.dataProxy = dataProxy;
        this.bePredicate = bePredicate;
        this.efvPredicate = efvPredicate;
        this.des = des;

        uEFVs = dataProxy.getUniqueEFVs();
        tstat = dataProxy.getTStatistics(des);
        pvals = dataProxy.getPValues(des);
        factors = dataProxy.getFactors();
        factorValues = dataProxy.getFactorValues();
        deAccessions = dataProxy.getDesignElementAccessions();
        bioentities = dataProxy.getGenes();
    }

    @Nonnull
    @Override
    public UpDownExpression getExpression() {
        return UpDownExpression.valueOf(getP(), getT());
    }

    @Nonnull
    @Override
    public Pair<String, String> getEfv() {
        return uEFVs.get(efvi);
    }

    @Override
    public long getBioEntityId() {
        return bioentities[de()];
    }

    @Override
    public float getT() {
        return tstat.get(dii, efvi);
    }

    @Override
    public float getP() {
        return pvals.get(dii, efvi);
    }

    public boolean isEmpty() {
        return uEFVs.isEmpty();
    }

    @Override
    @Deprecated
    public int getDeIndex() {
        return de();
    }

    @Nonnull
    @Override
    public String getDeAccession() {
        return deAccessions[de()];
    }

    public int getEfvCount() {
        return uEFVs.size();
    }

    public int getDeCount() {
        return des == null ? deAccessions.length : des.length;
    }

    public boolean nextEFV() {
        for (efvi++; efvi < uEFVs.size() && !efvPredicate.apply(uEFVs.get(efvi)); efvi++) {
        }
        if (efvi < uEFVs.size()) {
            return true;
        } else {
            efvi = -1;
            return false;
        }
    }

    public boolean nextBioEntity() {
        for (dii++; dii < getDeCount() && !bePredicate.apply(bioentities[de()]); dii++) {
        }
        if (dii < getDeCount()) {
            return true;
        } else {
            dii = -1;
            return false;
        }
    }

    public String toString() {
        return "StatisticsCursor{" + dataProxy + "}";
    }

    public StatisticsSnapshot getSnapshot() {
        return new StatisticsSnapshot(this);
    }

    private int cachedFor = -1;
    private float[] cachedExpression = null;

    public float[] getRawExpression() {
        try {
            final int de = de();
            if (de != cachedFor) {
                cachedExpression = dataProxy.getExpressionDataForDesignElementAtIndex(de);
                cachedFor = de;
            }
            return copySelected(cachedExpression, getAssaysForEFV(getEfv()));
        } catch (AtlasDataException e) {
            throw createUnexpected("Failed to read expression data", e);
        }
    }

    private BitSet getAssaysForEFV(Pair<String, String> efv) throws AtlasDataException {
        final String name = efv.getFirst();
        final String value = efv.getSecond();

        BitSet assays = new BitSet(factorValues.length);
        for (int i = 0; i < factors.length; i++) {
            if (name.equals(factors[i])) {
                for (int j = 0; j < factorValues[i].length; j++) {
                    assays.set(j, value.equals(factorValues[i][j]));
                }
            }
        }
        return assays;
    }

    /**
     * @return current design element's index in the NetCDF
     */
    private int de() {
        return des == null ? dii : des[dii];
    }

    @VisibleForTesting
    static float[] copySelected(float[] src, BitSet mask) {
        float[] result = new float[mask.cardinality()];
        for (int i = 0, j = 0; i < src.length; i++) {
            if (mask.get(i)) {
                result[j++] = src[i];
            }
        }
        return result;
    }
}
