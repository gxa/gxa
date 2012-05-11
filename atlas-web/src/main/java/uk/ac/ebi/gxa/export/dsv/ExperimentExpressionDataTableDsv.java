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

import uk.ac.ebi.gxa.data.ExpressionDataCursor;
import uk.ac.ebi.gxa.utils.dsv.DsvDocument;

import java.util.Iterator;

import static com.google.common.collect.ObjectArrays.concat;

/**
 * @author Olga Melnichuk
 */
public class ExperimentExpressionDataTableDsv {

    public static DsvDocument createDsvDocument(final ExpressionDataCursor cursor) {
        return new DsvDocument() {
            @Override
            public String[] getHeader() {
                return concat("DesignElementAccession", cursor.getAssayAccessions());
            }

            @Override
            public Iterator<String[]> getRowIterator() {
                return new Iterator<String[]>() {
                    @Override
                    public boolean hasNext() {
                        return cursor.hasNextDE();
                    }

                    @Override
                    public String[] next() {
                        cursor.nextDE();
                        float [] values = cursor.getValues();
                        String[] converted = new String[values.length + 1];
                        converted[0] = cursor.getDeAccession();
                        for(int i=0; i<values.length; i++) {
                            converted[i + 1] = Float.toString(values[i]);
                        }
                        return converted;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("Remove operation is not supported");
                    }
                };
            }

            @Override
            public int getTotalRowCount() {
                return cursor.getDeCount();
            }
        };
    }
}
