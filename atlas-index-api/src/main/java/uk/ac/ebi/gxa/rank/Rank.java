/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.rank;

/**
 * @author Olga Melnichuk
 */
public class Rank implements Comparable<Rank>{

    public static final int MAX = 100;
    public static final int MIN = 1;

    private final int rank;

    public Rank(double ratio) {
        if (ratio < 0 || ratio > 1) {
            throw new IllegalArgumentException("Argument value is out of bounds [0..1]: " + ratio);
        }
        this.rank = MIN + (int) Math.round((MAX - MIN) * ratio);
    }

    public boolean isMax() {
        return rank == MAX;
    }

    public boolean isMin() {
        return rank == MIN;
    }

    public Rank max(Rank o) {
        if (o == null) {
            return this;
        }
        return this.compareTo(o) > 0 ? this : o;
    }

    @Override
    public int compareTo(Rank o) {
        return rank - o.rank;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rank)) return false;

        Rank rank1 = (Rank) o;

        if (rank != rank1.rank) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return rank;
    }

    @Override
    public String toString() {
        return "Rank{" +
                "rank=" + rank +
                '}';
    }

    public static Rank max(Rank r1, Rank r2) {
        return r1.equals(r2) ? r1 : (r1.compareTo(r2) > 0 ? r1 : r2);
    }

    public static Rank maxRank() {
        return new Rank(1.0);
    }

    public static Rank minRank() {
        return new Rank(0.0);
    }
}
