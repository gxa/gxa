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

package ae3.service.structuredquery;

/**
 * @author Olga Melnichuk
 */
public class Rank implements Comparable<Rank>{

    private final double rank;

    public Rank(double ratio) {
        if (ratio < 0 || ratio > 1) {
            throw new IllegalArgumentException("Rank value is out of bounds [0.0 : 1.0]: " + ratio);
        }
        this.rank = ratio;
    }

    public boolean isMax() {
        return rank == 1.0;
    }

    public boolean isMin() {
        return rank == 0.0;
    }

    public Rank max(Rank o) {
        if (o == null) {
            return this;
        }
        return this.compareTo(o) > 0 ? this : o;
    }

    @Override
    public int compareTo(Rank o) {
        return Double.compare(rank, o.rank);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rank)) return false;

        Rank rank1 = (Rank) o;

        if (Double.compare(rank1.rank, rank) != 0) return false;

        return true;
    }

    @Override
    public int hashCode() {
        long temp = rank != +0.0d ? Double.doubleToLongBits(rank) : 0L;
        return (int) (temp ^ (temp >>> 32));
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
