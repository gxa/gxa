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

package uk.ac.ebi.gxa.utils.dsv;

import com.google.common.base.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.transform;

/**
 * @author Olga Melnichuk
 */
public class DsvRowIterator<T> implements Iterator<List<String>> {

    private final int rowCount;
    private final Iterator<T> rowIterator;
    private final List<DsvColumn<T>> columns = new ArrayList<DsvColumn<T>>();

    public DsvRowIterator(Iterator<T> rowIterator, int rowCount) {
        this.rowCount = rowCount;
        this.rowIterator = rowIterator;
    }

    public DsvRowIterator<T> addColumn(@Nonnull String name, @Nonnull String description, @Nonnull Function<T, String> converter) {
        return addColumn(new DsvColumnDefault<T>(name, description, converter));
    }

    public DsvRowIterator<T> addColumn(@Nonnull DsvColumn<T> column) {
        this.columns.add(column);
        return this;
    }

    public DsvRowIterator<T> addColumns(Collection<? extends DsvColumn<T>> columns) {
        this.columns.addAll(columns);
        return this;
    }


    public List<String> getColumnNames() {
        return transform(columns, new Function<DsvColumn<T>, String>() {
            @Override
            public String apply(@Nullable DsvColumn<T> column) {
                return checkNotNull(column.getName(), "Null column name in CSV/TSV");
            }
        });
    }

    public List<String> getColumnDescriptions() {
        return transform(columns, new Function<DsvColumn<T>, String>() {
            @Override
            public String apply(@Nullable DsvColumn<T> column) {
                String desc = column.getDescription();
                return isNullOrEmpty(desc) ? "" : "# " + desc;
            }
        });
    }

    public int getTotalRowCount() {
        return rowCount;
    }

    @Override
    public boolean hasNext() {
        return rowIterator.hasNext();
    }

    @Override
    public List<String> next() {
        final T value = rowIterator.next();
        return transform(columns, new Function<DsvColumn<T>, String>() {
            @Override
            public String apply(@Nullable DsvColumn<T> column) {
                return column.convert(value);
            }
        });
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove operation is not supported");
    }

    private static class DsvColumnDefault<T> implements DsvColumn<T> {
        private final String name;
        private final String description;
        private final Function<T, String> converter;

        private DsvColumnDefault(String name, String description, Function<T, String> converter) {
            this.name = name;
            this.description = description;
            this.converter = converter;
        }

        public String convert(T value) {
            return converter.apply(value);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return description;
        }
    }
}


