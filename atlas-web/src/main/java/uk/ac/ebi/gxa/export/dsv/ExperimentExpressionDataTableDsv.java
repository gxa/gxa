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

import com.google.common.base.Function;
import org.hibernate.annotations.Columns;
import uk.ac.ebi.gxa.data.ExpressionDataCursor;
import uk.ac.ebi.gxa.utils.dsv.DsvColumn;
import uk.ac.ebi.gxa.utils.dsv.DsvRowIterator;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import static java.util.Arrays.asList;

/**
 * @author Olga Melnichuk
 */
public class ExperimentExpressionDataTableDsv {

    public static DsvRowIterator<ExpressionDataCursor> createDsvDocument(final ExpressionDataCursor cursor) {
        DsvRowIterator<ExpressionDataCursor> dsvIterator = new DsvRowIterator<ExpressionDataCursor>(new Iterator<ExpressionDataCursor>() {
            @Override
            public boolean hasNext() {
                return cursor.hasNextDE();
            }

            @Override
            public ExpressionDataCursor next() {
                cursor.nextDE();
                return cursor;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Remove operation not supported");
            }
        }, cursor.getDeCount());

        dsvIterator.addColumns(permanentColumns());

        int i = 0;
        for (String assayAcc : cursor.getAssayAccessions()) {
            dsvIterator.addColumn(assayAcc, "", assayValueConverter(i++));
        }
        return dsvIterator;
    }

    private static Function<ExpressionDataCursor, String> assayValueConverter(final int index) {
        return new Function<ExpressionDataCursor, String>() {
            @Override
            public String apply(@Nullable ExpressionDataCursor cursor) {
                return Float.toString(cursor.getValues()[index]);
            }
        };
    }

    static Collection<DsvColumn<ExpressionDataCursor>> permanentColumns() {
        return new ArrayList<DsvColumn<ExpressionDataCursor>>() {{
            add(new DsvColumn<ExpressionDataCursor>() {

                @Override
                public String convert(ExpressionDataCursor cursor) {
                    return cursor.getDeAccession();
                }

                @Override
                public String getName() {
                    return "DesignElementAccession";
                }

                @Override
                public String getDescription() {
                    return "";
                }
            });
        }};
    }
}
