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
import uk.ac.ebi.gxa.utils.dsv.DsvColumn;
import uk.ac.ebi.gxa.utils.dsv.DsvRowIterator;

import java.util.Iterator;
import java.util.Map;

import static java.util.Arrays.asList;

/**
 * @author Olga Melnichuk
 */
public class ExperimentTableDsv {

    private static enum Column implements DsvColumn<ExperimentAnalytics.TableRow> {
        GeneName("GeneName", "Name of gene") {
            @Override
            public String convert(ExperimentAnalytics.TableRow row) {
                return row.getGeneName();
            }
        },
        GeneIdentifier("GeneIdentifier", "Gene identifier") {
            @Override
            public String convert(ExperimentAnalytics.TableRow row) {
                return row.getGeneIdentifier();
            }
        },
        DesignElementAcc("DesignElementAccession", "Probe set name for which expression was measured on the array") {
            @Override
            public String convert(ExperimentAnalytics.TableRow row) {
                return row.getDeAccession();
            }
        },
        ExperimentalFactor("ExperimentalFactor", "Assay or Sample attribute that is an experimental factor") {
            @Override
            public String convert(ExperimentAnalytics.TableRow row) {
                return row.getFactor();
            }
        },
        ExperimentalFactorValue("ExperimentalFactorValue", "Value for the experimental factor") {
            @Override
            public String convert(ExperimentAnalytics.TableRow row) {
                return row.getFactorValue();
            }
        },
        UpDownExpression("UpDownExpression", "Type of differential expression reported") {
            @Override
            public String convert(ExperimentAnalytics.TableRow row) {
                return row.getUpDown();
            }
        },
        TSatistic("TStatistic", "Statistical measure of confidence in the differential expression call") {
            @Override
            public String convert(ExperimentAnalytics.TableRow row) {
                return Float.toString(row.getFloatTValue());
            }
        },
        PValue("PValue", "Statistical measure of confidence in the differential expression call") {
            @Override
            public String convert(ExperimentAnalytics.TableRow row) {
                return Float.toString(row.getFloatPValue());
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

        public abstract String convert(ExperimentAnalytics.TableRow row);
    }

    @SuppressWarnings("unchecked")
    public static DsvRowIterator<ExperimentAnalytics.TableRow> createDsvDocument(Map<String, Object> model) {
        return createDsvDocument((ExperimentAnalytics) model.get("analytics"));
    }

    public static DsvRowIterator<ExperimentAnalytics.TableRow> createDsvDocument(ExperimentAnalytics analytics) {
        Iterator<ExperimentAnalytics.TableRow> iterator = analytics.getRows().iterator();
        int size = analytics.size();

        return new DsvRowIterator<ExperimentAnalytics.TableRow>(iterator, size)
                .addColumns(asList(Column.GeneName,
                        Column.GeneIdentifier,
                        Column.DesignElementAcc,
                        Column.ExperimentalFactor,
                        Column.ExperimentalFactorValue,
                        Column.UpDownExpression,
                        Column.TSatistic,
                        Column.PValue)
                );

    }
}