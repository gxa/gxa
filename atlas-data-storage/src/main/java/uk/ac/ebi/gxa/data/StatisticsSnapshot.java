/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.UpDownExpression;

import javax.annotation.concurrent.Immutable;

/**
 * A snapshot of {@link DesignElementStatistics} to be stored and shared
 * <p/>
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
@Immutable
public final class StatisticsSnapshot implements DesignElementStatistics, Comparable<StatisticsSnapshot> {
    private final float p;
    private final float t;
    private final int deIndex;
    private final long bioEntityId;
    private final String deAccession;
    private final Pair<String, String> efv;

    public StatisticsSnapshot(DesignElementStatistics s) {
        p = s.getP();
        t = s.getT();
        bioEntityId = s.getBioEntityId();
        deAccession = s.getDeAccession();
        deIndex = s.getDeIndex();
        efv = s.getEfv();
    }

    @Override
    public UpDownExpression getExpression() {
        return UpDownExpression.valueOf(p, t);
    }

    @Override
    public Pair<String, String> getEfv() {
        return efv;
    }

    @Override
    public long getBioEntityId() {
        return bioEntityId;
    }

    @Override
    public float getT() {
        return t;
    }

    @Override
    public float getP() {
        return p;
    }

    @Override
    public int getDeIndex() {
        return deIndex;
    }

    @Override
    public String getDeAccession() {
        return deAccession;
    }

    @Override
    public String toString() {
        return "StatisticsSnapshot{" +
                "pValue=" + p +
                ", tStat=" + t +
                ", deIndex=" + deIndex +
                ", efv=" + efv +
                '}';
    }

    @Override
    public int compareTo(StatisticsSnapshot o) {
        return ORDER.compare(this, o);
    }
}
