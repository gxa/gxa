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

package ae3.service.structuredquery;

import ae3.model.AtlasGene;

import java.util.List;

/**
 * Structured query result row representing one gene and it's up/down counters
 *
 * @author pashky
 */
public class StructuredResultRow implements Comparable<StructuredResultRow> {
    private AtlasGene gene;

    private List<UpdownCounter> updownCounters; // all UpdownCounters
    private boolean qualifies = false; // if true means this row will be displayed as it has at least one cell with experiment counts
    // The following variables are non-primitive to prevent getTotalUpDnStudies()
    // and getTotalNoneDEStudies() being re-evaluated every time an instance of this
    // class is inserted into a SortedSet (heatmap construction speed up)
    private Integer totalUpDnStudies;
    private Integer totalNonDEStudies;
    // This variable forces getTotalUpDnStudies() and getTotalNoneDEStudies() to re-calculate the aggregate counts
    // if the aggregate counts cached before were derived from updownCounters list of a different size from the current size
    private int cachedCountersSize = 0;

    public StructuredResultRow(AtlasGene gene, List<UpdownCounter> updownCounters, boolean qualifies) {
        this.gene = gene;
        this.updownCounters = updownCounters;
        this.qualifies = qualifies;
    }

    public AtlasGene getGene() {
        return gene;
    }

    public List<UpdownCounter> getCounters() {
        return updownCounters;
    }

    public void addCounter(UpdownCounter counter) {
        updownCounters.add(counter);
    }


    /**
     * @return sum total of studies from updownCounters
     */
    public int getTotalUpDnStudies() {
        int numCounters = updownCounters.size();
        if (totalUpDnStudies == null || cachedCountersSize != updownCounters.size()) {
            totalUpDnStudies = 0;
            for (UpdownCounter counter : updownCounters) {
                totalUpDnStudies += counter.getNoStudies();
            }
            cachedCountersSize = numCounters;
        }
        return totalUpDnStudies;
    }

    /**
     * @return sum total of studies from updownCounters
     */
    public int getTotalNoneDEStudies() {
        int numCounters = updownCounters.size();
        if (totalNonDEStudies == null || cachedCountersSize != numCounters) {
            totalNonDEStudies = 0;
            for (UpdownCounter counter : updownCounters) {
                totalNonDEStudies += counter.getNones();
            }
            cachedCountersSize = numCounters;
        }

        return totalNonDEStudies;
    }

    public boolean qualifies() {
        return qualifies;
    }

    /**
     * StructuredResultRows are compared
     *
     * @param o
     * @return
     */
    public int compareTo(StructuredResultRow o) {
        if (getTotalUpDnStudies() != o.getTotalUpDnStudies()) {
            return -(getTotalUpDnStudies() - o.getTotalUpDnStudies());
        }

        if (getTotalNoneDEStudies() != o.getTotalNoneDEStudies()) {
            return -(getTotalNoneDEStudies() - o.getTotalNoneDEStudies());
        }

        if (getGene().getGeneName() == null) {
            return 1;
        } else if (o.getGene().getGeneName() == null) {
            return -1;
        } else
            return getGene().getGeneName().compareTo(o.getGene().getGeneName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StructuredResultRow that = (StructuredResultRow) o;

        if (gene != null ? !gene.equals(that.gene) : that.gene != null) return false;
        if (qualifyingCounters != null ? !qualifyingCounters.equals(that.qualifyingCounters) : that.qualifyingCounters != null)
            return false;
        if (totalNonDEStudies != null ? !totalNonDEStudies.equals(that.totalNonDEStudies) : that.totalNonDEStudies != null)
            return false;
        if (totalUpDnStudies != null ? !totalUpDnStudies.equals(that.totalUpDnStudies) : that.totalUpDnStudies != null)
            return false;
        if (updownCounters != null ? !updownCounters.equals(that.updownCounters) : that.updownCounters != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = gene != null ? gene.hashCode() : 0;
        result = 31 * result + (updownCounters != null ? updownCounters.hashCode() : 0);
        result = 31 * result + (qualifyingCounters != null ? qualifyingCounters.hashCode() : 0);
        result = 31 * result + (totalUpDnStudies != null ? totalUpDnStudies.hashCode() : 0);
        result = 31 * result + (totalNonDEStudies != null ? totalNonDEStudies.hashCode() : 0);
        return result;
    }
}
