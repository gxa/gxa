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
 * @author pashky
*/
public class StructuredResultRow implements Comparable<StructuredResultRow>{
    private AtlasGene gene;

    private List<UpdownCounter> updownCounters; // all UpdownCounters
    private List<UpdownCounter> qualifyingCounters; // UpdownCounters with counts greater than min experiments
    // The following variables are non-primitive to prevent getTotalUpDnStudies()
    // and getTotalNoneDEStudies() being re-evaluated every time an instance of this
    // class is inserted into a SortedSet (heatmap construction speed up)
    private Integer totalUpDnStudies;
    private Integer totalNonDEStudies;

    public StructuredResultRow(AtlasGene gene, List<UpdownCounter> updownCounters, List<UpdownCounter> qualifyingCounters) {
        this.gene = gene;
        this.updownCounters = updownCounters;
        this.qualifyingCounters = qualifyingCounters;
    }

    public AtlasGene getGene() {
        return gene;
    }

    public List<UpdownCounter> getCounters() {
        return updownCounters;
    }

    /**
     * @return sum total of studies from updownCounters
     */
    public int getTotalUpDnStudies() {
        if (totalUpDnStudies == null) {
            totalUpDnStudies = 0;
            for (UpdownCounter counter : updownCounters) {
                totalUpDnStudies += counter.getNoStudies();
            }
        }
        return totalUpDnStudies;
    }

     /**
     * @return sum total of studies from updownCounters
     */
    public int getTotalNoneDEStudies() {
         if (totalNonDEStudies == null) {
             totalNonDEStudies = 0;
             for (UpdownCounter counter : updownCounters) {
                 totalNonDEStudies += counter.getNones();
             }
         }
         return totalNonDEStudies;
    }

    public boolean qualifies() {
        return qualifyingCounters.size() > 0;
    }

    /**
     * StructuredResultRows are compared
     * @param o
     * @return
     */
    public int compareTo(StructuredResultRow o) {
        if (getTotalUpDnStudies() != o.getTotalUpDnStudies())
            return getTotalUpDnStudies() > o.getTotalUpDnStudies() ? -1 : 1;
        else if (getTotalNoneDEStudies() != o.getTotalNoneDEStudies()) {
            return getTotalNoneDEStudies() > o.getTotalNoneDEStudies() ? -1 : 1;
        }

        if (getGene().getGeneName() == null) {
            return 1;
        } else if (o.getGene().getGeneName() == null) {
            return -1;
        } else
            return getGene().getGeneName().compareTo(o.getGene().getGeneName());
    }
}
