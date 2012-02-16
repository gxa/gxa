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

import uk.ac.ebi.gxa.annotator.loader.util.CSVBasedReader;
import uk.ac.ebi.gxa.annotator.loader.util.InvalidCSVColumnException;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityAnnotationData;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.model.ExternalBioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * @author Olga Melnichuk
 * @version 1/15/12 1:10 PM
 */
class MartPropertyValuesLoader {

    private final MartServiceClient martClient;
    private final Map<String, BioEntityType> name2Type;

    public MartPropertyValuesLoader(BioMartAnnotationSource annotSource, MartServiceClient martClient) {
        this.martClient = martClient;
        this.name2Type = annotSource.getExternalName2TypeMap();
    }

    public void load(ExternalBioEntityProperty property, BioEntityAnnotationData.Builder builder)
            throws BioMartException, IOException, InvalidCSVColumnException {
        List<String> columns = new ArrayList<String>();
        columns.addAll(name2Type.keySet());
        columns.add(property.getName());
        parse(martClient.runQuery(columns), property, builder);

    }

    private int parse(InputStream in, ExternalBioEntityProperty property, BioEntityAnnotationData.Builder builder)
            throws IOException, InvalidCSVColumnException {
        CSVBasedReader reader = null;
        try {
            reader = CSVBasedReader.tsvReader(in);

            int rc = 0;
            CSVBasedReader.Row row;
            while ((row = reader.readNext()) != null) {
                rc++;
                int col = 0;
                BEPropertyValue propertyValue = new BEPropertyValue(property.getBioEntityProperty(), row.getLast());
                for (BioEntityType type : name2Type.values()) {
                    builder.addPropertyValue(row.get(col++), type, propertyValue);
                }
            }
            return rc;
        } finally {
            closeQuietly(reader);
        }
    }
}
