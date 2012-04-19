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

import uk.ac.ebi.gxa.spring.view.dsv.DsvDocument;
import uk.ac.ebi.gxa.web.controller.ExperimentDesignUI;

import java.util.*;

import static java.util.Arrays.asList;

/**
 * @author Olga Melnichuk
 */
public class ExperimentDesignTableDsv {

    public static DsvDocument createDsvDocument(Map<String, Object> model) {
        final ExperimentDesignUI expDesign =
                (ExperimentDesignUI) model.get("experimentDesign");

        final Iterator<ExperimentDesignUI.Row> iterator = expDesign.getPropertyValues().iterator();

        return new DsvDocument() {
            private String[] asArray(Collection<String> values, String... other) {
                List<String> headers = new ArrayList<String>(asList(other));
                headers.addAll(values);
                return headers.toArray(new String[headers.size()]);
            }

            @Override
            public String[] getHeader() {
                return asArray(expDesign.getPropertyNames(), "assay", "array");
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
                        ExperimentDesignUI.Row row = iterator.next();
                        return asArray(row.getPropertyValues(), row.getAssayAcc(), row.getArrayDesignAcc());
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
