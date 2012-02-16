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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.annotator.loader.util.CSVBasedReader;
import uk.ac.ebi.gxa.annotator.loader.util.InvalidCSVColumnException;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityData;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import static com.google.common.io.Closeables.closeQuietly;

/**
 * @author Olga Melnichuk
 * @version 1/14/12 7:29 PM
 */
class MartBioEntitiesLoader {

    final private Logger log = LoggerFactory.getLogger(this.getClass());

    private final BioMartAnnotationSource annotSource;
    private final MartServiceClient martClient;
    private final Map<String, BioEntityType> name2Type;

    public MartBioEntitiesLoader(BioMartAnnotationSource annotSource, MartServiceClient martClient) {
        this.annotSource = annotSource;
        this.martClient = martClient;
        this.name2Type = annotSource.getExternalName2TypeMap();
    }

    public void load(BioEntityData.Builder builder) throws BioMartException, IOException, InvalidCSVColumnException {
        Set<String> columns = name2Type.keySet();
        int actualRowCount = parse(martClient.runQuery(columns), builder);
        log.debug("loaded rows from BioMart: " + actualRowCount);

    }

    private int parse(InputStream in, BioEntityData.Builder dataBuilder) throws IOException, InvalidCSVColumnException {
        CSVBasedReader reader = null;

        try {
            reader = CSVBasedReader.tsvReader(in);

            int rc = 0;
            CSVBasedReader.Row row;
            while ((row = reader.readNext()) != null) {
                rc ++;
                int col = 0;
                for (BioEntityType beType : name2Type.values()) {
                    dataBuilder.addBioEntity(new BioEntity(row.get(col++), beType, annotSource.getOrganism()));
                }
            }
            return rc;
        } finally {
            closeQuietly(reader);
        }
    }

}
