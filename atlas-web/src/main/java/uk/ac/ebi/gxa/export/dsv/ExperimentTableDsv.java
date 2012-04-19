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

import uk.ac.ebi.gxa.service.experiment.ExperimentAnalytics;
import uk.ac.ebi.gxa.spring.view.dsv.DsvDocument;
import uk.ac.ebi.gxa.web.controller.ExperimentViewController;

import java.util.Iterator;
import java.util.Map;

/**
 * @author Olga Melnichuk
 */
public class ExperimentTableDsv {

    @SuppressWarnings("unchecked")
    public static DsvDocument createDsvDocument(Map<String, Object> model) {
        return createDsvDocument((Iterable<ExperimentAnalytics.TableRow>) model.get("items"));
    }

    public static DsvDocument createDsvDocument(Iterable<ExperimentAnalytics.TableRow> rows) {
        final Iterator<ExperimentAnalytics.TableRow> iterator = rows.iterator();

        return new DsvDocument() {

            @Override
            public String[] getHeader() {
                return new String[]{
                        "GeneName",
                        "GeneIdentifier",
                        "DesignElementAccession",
                        "ExperimentalFactor",
                        "ExperimentalFactorValue",
                        "UpDownExpression",
                        "TStatistic",
                        "PValue"
                };
            }

            @Override
            public String[] getColumnsDescription() {
                return new String[]{
                        "Name of gene",
                        "Gene identifier",
                        "Probe set name for which expression was measured on the array",
                        "Assay or Sample attribute that is an experimental factor",
                        "Value for the experimental factor",
                        "Type of differential expression reported",
                        "Statistical measure of confidence in the differential expression call",
                        "Statistical measure of confidence in the differential expression call"
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
                        ExperimentAnalytics.TableRow row = iterator.next();
                        return new String[] {
                                row.getGeneName(),
                                row.getGeneIdentifier(),
                                row.getDeAccession(),
                                row.getFactor(),
                                row.getFactorValue(),
                                row.getUpDown(),
                                Float.toString(row.getFloatTValue()),
                                Float.toString(row.getFloatPValue())
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
