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

import org.junit.Test;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityData;
import uk.ac.ebi.gxa.annotator.loader.data.InvalidAnnotationDataException;
import uk.ac.ebi.gxa.annotator.loader.util.InvalidCSVColumnException;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static uk.ac.ebi.gxa.annotator.AnnotationSourceFactory.newBioMartAnnotationSource;

/**
 * ID_PROPERTY_1    ID_PROPERTY_2
 * ENSBTAT00000057520  ENSBTAG00000039669
 * ENSBTAT00000049990  ENSBTAG00000039669
 * ENSBTAT00000015116  ENSBTAG00000025314
 *
 * @author Olga Melnichuk
 */
public class MartBioEntitiesLoaderTest {

    private static Map<String, List<String>> TSV = new HashMap<String, List<String>>() {
        {
            put("type1", asList("ENSBTAT00000057520", "ENSBTAT00000049990", "ENSBTAT00000015116"));
            put("type2", asList("ENSBTAG00000039669", "ENSBTAG00000039669", "ENSBTAG00000025314"));
        }
    };

    @Test
    public void test() throws InvalidAnnotationDataException, BioMartException, IOException, InvalidCSVColumnException {
        BioMartAnnotationSource annotSource = newAnnotationSource();

        BioEntityData.Builder builder = new BioEntityData.Builder(annotSource.getOrganism());
        new MartBioEntitiesLoader(annotSource, newMartClient()).load(builder);

        BioEntityData data = builder.build(annotSource.getTypes());
        assertEquals(annotSource.getOrganism(), data.getOrganism());

        for (BioEntityType type : annotSource.getTypes()) {
            Collection<BioEntity> items = data.getBioEntitiesOfType(type);
            assertEquals(3, items.size());
            //TODO
        }
    }

    private BioMartAnnotationSource newAnnotationSource() {
        return newBioMartAnnotationSource()
                .type("type1", "prop1", "prop2")
                .type("type2", "prop3", "prop4")
                .property("prop1", "extProp1")
                .property("prop2", "extProp2")
                .property("prop3", "extProp3")
                .property("prop4", "extProp4")
                .create();
    }

    private MartServiceClient newMartClient() {

        StringBuilder sb = new StringBuilder();
        int ncol = TSV.size();
        for (int i = 0; i < ncol; i++) {
            if (i > 0) {
                sb.append("\t");
            }
            sb.append("COLUMN_").append(i);
        }
        sb.append("\n");
        //TODO
        final String columns = sb.toString();
        final int size = TSV.entrySet().iterator().next().getValue().size();


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
            public InputStream runAttributesQuery() throws BioMartException, IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public InputStream runDatasetListQuery() throws BioMartException, IOException {
                throw new UnsupportedOperationException();
            }
        };
    }
}
