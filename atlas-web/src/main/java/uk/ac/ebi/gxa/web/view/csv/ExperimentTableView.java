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

package uk.ac.ebi.gxa.web.view.csv;

import uk.ac.ebi.gxa.spring.view.csv.AbstractCsvView;
import uk.ac.ebi.gxa.web.controller.ExperimentViewController;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Olga Melnichuk
 */
public class ExperimentTableView extends AbstractCsvView {

    @Override
    protected CsvDocument buildDocument(Map<String, Object> model) {
        Iterable<ExperimentViewController.ExperimentTableRow> rows =
                (Iterable<ExperimentViewController.ExperimentTableRow>) model.get("items");
        final Iterator<ExperimentViewController.ExperimentTableRow> iterator = rows.iterator();
        
        return new CsvDocument() {
            @Override
            public String[] getComments() {
                return new String[]{
                        "# Blah blah blah..."
                };
            }

            @Override
            public String[] getHeader() {
                return new String[]{
                        "GeneName",
                        "GeneIdentifier",
                        "DesignElementAccession",
                        "ExperimentalFactor",
                        "ExperimentalFactorValue",
                        "UpDownExpression",
                        "PValue",
                        "TStatistic"
                };
            }

            @Override
            public Iterator<String[]> getRowIterator() {
                 return new Iterator<String[]>() {
                     @Override
                     public boolean hasNext() {
                         return iterator.hasNext();
                     }

                     @Override
                     public String[] next() {
                         ExperimentViewController.ExperimentTableRow row = iterator.next();
                         return new String[] {
                                 row.getGeneName(),
                                 row.getGeneIdentifier(),
                                 row.getDeAccession(),
                                 row.getFactor(),
                                 row.getFactorValue(),
                                 row.getUpDown(),
                                 Float.toString(row.getFloatPValue()),
                                 Float.toString(row.getFloatTValue())
                         };
                     }

                     @Override
                     public void remove() {
                         iterator.remove();
                     }
                 };
            }
        };
    }
}
