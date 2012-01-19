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

import com.google.common.base.Predicate;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.UpDownExpression;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * A cursor used to navigate a NetCDF-stored statistics
 * <p/>
 * Hides the parallel structures (DE, uEFV, P, T) from the programmer, hence making it harder to make an error.
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

    private int i = -1, j = -1;

    private final int deCount;
    private final int efvCount;

    private final List<Pair<String, String>> uEFVs;
    private final long[] bioentities;
    private final TwoDFloatArray tstat;
    private final TwoDFloatArray pvals;
    private final String[] deAccessions;

    private final DataProxy dataProxy;
    private final Predicate<Long> bePredicate;
    private final Predicate<Pair<String, String>> efvPredicate;

    StatisticsCursor(DataProxy dataProxy, Predicate<Long> bePredicate, Predicate<Pair<String, String>> efvPredicate)
            throws AtlasDataException, StatisticsNotFoundException {
        this.dataProxy = dataProxy;
        this.bePredicate = bePredicate;
        this.efvPredicate = efvPredicate;

        uEFVs = dataProxy.getUniqueEFVs();
        tstat = dataProxy.getTStatistics();
        pvals = dataProxy.getPValues();
        deAccessions = dataProxy.getDesignElementAccessions();
        bioentities = dataProxy.getGenes();

        deCount = tstat.getRowCount();
        efvCount = uEFVs.size();
    }

    @Override
    public UpDownExpression getExpression() {
        return UpDownExpression.valueOf(getP(), getT());
    }

    @Override
    public Pair<String, String> getEfv() {
        return uEFVs.get(j);
    }

    @Override
    public long getBioEntityId() {
        return bioentities[i];
    }

    @Override
    public float getT() {
        return tstat.get(i, j);
    }

    @Override
    public float getP() {
        return pvals.get(i, j);
    }

    public boolean isEmpty() {
        return uEFVs.size() == 0;
    }

    @Override
    @Deprecated
    public int getDeIndex() {
        return i;
    }

    @Override
    public String getDeAccession() {
        return deAccessions[i];
    }

    public int getEfvCount() {
        return efvCount;
    }

    public int getDeCount() {
        return deCount;
    }

    public boolean nextEFV() {
        for (j++; j < efvCount && !efvPredicate.apply(uEFVs.get(j)); j++) {
        }
        return j < efvCount;
    }

    public boolean nextBioEntity() {
        for (i++; i < deCount && !bePredicate.apply(bioentities[i]); i++) {
        }
        return i < deCount;
    }

    public String toString() {
        return "cursor over" + dataProxy;
    }

    public StatisticsSnapshot getSnapshot() {
        return new StatisticsSnapshot(this);
    }

    /**
     * NEVER USE ME
     * <p/>
     * It is only here to support a <em>temporary</em> solution for a legacy code.
     *
     * @param de  design element index to jump to
     * @param efv experiment factor value index to jump to
     */
    @Deprecated
    void jump(int de, int efv) {
        i = de;
        j = efv;
    }
}
