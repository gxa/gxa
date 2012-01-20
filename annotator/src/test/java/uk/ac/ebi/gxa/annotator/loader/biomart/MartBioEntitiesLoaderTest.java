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
import java.util.*;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.ac.ebi.gxa.annotator.AnnotationSourceFactory.newBioMartAnnotationSource;
import static uk.ac.ebi.gxa.annotator.Tables.convert2map;
import static uk.ac.ebi.gxa.annotator.Tables.transpose;
import static uk.ac.ebi.gxa.annotator.loader.biomart.MartServiceClientFactory.newMartClient;

/**
 * ID_PROPERTY_1    ID_PROPERTY_2
 * ENSBTAT00000057520  ENSBTAG00000039669
 * ENSBTAT00000049990  ENSBTAG00000039669
 * ENSBTAT00000015116  ENSBTAG00000025314
 *
 * @author Olga Melnichuk
 */
public class MartBioEntitiesLoaderTest {

    private static List<String[]> TSV = new ArrayList<String[]>() {
        {
            add(new String[]{"type1", "type2"});
            add(new String[]{"ENSBTAT00000057520", "ENSBTAG00000039669"});
            add(new String[]{"ENSBTAT00000049990", "ENSBTAG00000039669"});
            add(new String[]{"ENSBTAT00000015116", "ENSBTAG00000025314"});
        }
    };

    private static Map<String, List<String>> TSV_TRANSPOSED = convert2map(transpose(TSV));

    @Test
    public void test() throws InvalidAnnotationDataException, BioMartException, IOException, InvalidCSVColumnException {
        BioMartAnnotationSource annotSource = newAnnotationSource();

        BioEntityData.Builder builder = new BioEntityData.Builder(annotSource.getOrganism());
        new MartBioEntitiesLoader(annotSource, newMartClient(TSV)).load(builder);

        BioEntityData data = builder.build(annotSource.getTypes());
        assertEquals(annotSource.getOrganism(), data.getOrganism());

        for (BioEntityType type : annotSource.getTypes()) {
            Collection<BioEntity> actual = data.getBioEntitiesOfType(type);
            Collection<String> expected = TSV_TRANSPOSED.get(type.getName());
            for (BioEntity be : actual) {
                assertTrue(expected.contains(be.getIdentifier()));
            }
        }
    }

    private static BioMartAnnotationSource newAnnotationSource() {
        return newBioMartAnnotationSource()
                .type("type1", "prop1", "prop2")
                .type("type2", "prop3", "prop4")
                .property("prop1", "extProp1")
                .property("prop2", "extProp2")
                .property("prop3", "extProp3")
                .property("prop4", "extProp4")
                .create();
    }
}
