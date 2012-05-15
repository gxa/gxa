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
import uk.ac.ebi.gxa.utils.dsv.DsvRowIterator;
import uk.ac.ebi.gxa.web.controller.ExperimentDesignUI;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Olga Melnichuk
 */
public class ExperimentDesignTableDsv {

    @SuppressWarnings("unchecked")
    public static DsvRowIterator<ExperimentDesignUI.Row> createDsvDocument(Map<String, Object> model) {
        return createDsvDocument((ExperimentDesignUI) model.get("experimentDesign"));
    }

    public static DsvRowIterator<ExperimentDesignUI.Row> createDsvDocument(final ExperimentDesignUI expDesign) {
        Collection<ExperimentDesignUI.Row> rows = expDesign.getPropertyValues();
        Iterator<ExperimentDesignUI.Row> iterator = rows.iterator();
        int size = rows.size();

        DsvRowIterator<ExperimentDesignUI.Row> dsvIterator = new DsvRowIterator<ExperimentDesignUI.Row>(iterator, size);
        int i = 0;
        for (String propName : expDesign.getPropertyNames()) {
            dsvIterator.addColumn(propName, "", propertyValueConverter(i++));
        }

        dsvIterator
                .addColumn("assay", "", new Function<ExperimentDesignUI.Row, String>() {
                    @Override
                    public String apply(@Nullable ExperimentDesignUI.Row row) {
                        return row.getAssayAcc();
                    }
                })
                .addColumn("array", "", new Function<ExperimentDesignUI.Row, String>() {
                    @Override
                    public String apply(@Nullable ExperimentDesignUI.Row row) {
                        return row.getArrayDesignAcc();
                    }
                });
        return dsvIterator;
    }

    private static Function<ExperimentDesignUI.Row, String> propertyValueConverter(final int index) {
        return new Function<ExperimentDesignUI.Row, String>() {
            @Override
            public String apply(@Nullable ExperimentDesignUI.Row row) {
                return row.getPropertyValues().get(index);
            }
        };
    }
}
