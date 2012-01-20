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

package uk.ac.ebi.gxa.annotator.loader.biomart;

import com.google.common.base.Joiner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

/**
 * @author Olga Melnichuk
 * @version 1/19/12 11:37 PM
 */
public class MartServiceClientFactory {

    public static MartServiceClient newMartClient(List<String[]> table) {
        StringBuilder sb = new StringBuilder();
        for (String[] row : table) {
            sb.append(Joiner.on("\t").join(row)).append("\n");
        }
        final String columns = sb.toString();
        final int size = table.size() - 1;

        return new MartServiceClient() {
            @Override
            public InputStream runQuery(Collection<String> attributes) throws BioMartException, IOException {
                return new ByteArrayInputStream(columns.getBytes("UTF-8"));
            }

            @Override
            public int runCountQuery(Collection<String> attributes) throws BioMartException, IOException {
                return size;
            }

            @Override
            public Collection<String> runAttributesQuery() throws BioMartException, IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public Collection<String> runDatasetListQuery() throws BioMartException, IOException {
                throw new UnsupportedOperationException();
            }
        };
    }
}
