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
public class StructuredResultRow {
    private AtlasGene gene;

    private List<UpdownCounter> updownCounters;

    public StructuredResultRow(AtlasGene gene, List<UpdownCounter> updownCounters) {
        this.gene = gene;
        this.updownCounters = updownCounters;
    }

    public AtlasGene getGene() {
        return gene;
    }

    public List<UpdownCounter> getCounters() {
        return updownCounters;
    }
}
