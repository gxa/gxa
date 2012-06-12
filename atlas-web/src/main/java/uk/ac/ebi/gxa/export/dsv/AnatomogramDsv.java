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


package uk.ac.ebi.gxa.export.dsv;

import uk.ac.ebi.gxa.anatomogram.Anatomogram;
import uk.ac.ebi.gxa.utils.dsv.DsvColumn;
import uk.ac.ebi.gxa.utils.dsv.DsvRowIterator;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Robert Petryszak
 */
public class AnatomogramDsv {

    private static enum Column implements DsvColumn<Anatomogram.OrganismPart> {
        OrganismPartEfo("Organism Part", "Organism Part") {
            @Override
            public String convert(Anatomogram.OrganismPart row) {
                return row.getCaption();
            }
        },
        UpCounts("UpExperimentCounts", "Number of experiments in which this gene was up differentially expressed") {
            @Override
            public String convert(Anatomogram.OrganismPart row) {
                return String.valueOf(row.getUp());
            }
        },
        DownCounts("DownExperimentCounts", "Number of experiments in which this gene was down differentially expressed") {
            @Override
            public String convert(Anatomogram.OrganismPart row) {
                return String.valueOf(row.getDn());
            }
        };

        private final String name;
        private final String description;

        private Column(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public abstract String convert(Anatomogram.OrganismPart row);
    }

    public static DsvRowIterator<Anatomogram.OrganismPart> createDsvDocument(Anatomogram anatomogram) {
        Iterator<Anatomogram.OrganismPart> iterator = anatomogram.getOrganismParts().iterator();
        int size = anatomogram.getOrganismParts().size();

        return new DsvRowIterator<Anatomogram.OrganismPart>(iterator, size).addColumns(permanentColumns());
    }

    static List<DsvColumn<Anatomogram.OrganismPart>> permanentColumns() {
        return Arrays.<DsvColumn<Anatomogram.OrganismPart>>asList(Column.OrganismPartEfo,
                Column.UpCounts,
                Column.DownCounts);
    }
}
