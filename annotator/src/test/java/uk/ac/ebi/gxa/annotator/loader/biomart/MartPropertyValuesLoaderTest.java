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
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityAnnotationData;
import uk.ac.ebi.gxa.annotator.loader.data.InvalidAnnotationDataException;
import uk.ac.ebi.gxa.annotator.loader.util.InvalidCSVColumnException;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.model.ExternalBioEntityProperty;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.ac.ebi.gxa.annotator.AnnotationSourceFactory.newBioMartAnnotationSource;
import static uk.ac.ebi.gxa.annotator.Tables.convert2map;
import static uk.ac.ebi.gxa.annotator.Tables.transpose;
import static uk.ac.ebi.gxa.annotator.loader.biomart.MartServiceClientFactory.newMartClient;

/**
 * ID_PROPERTY_1        ID_PROPERTY_2       PROPERTY
 * ENSBTAT00000015116	ENSBTAG00000025314	extracellular region
 * ENSBTAT00000057520	ENSBTAG00000039669
 * ENSBTAT00000015116	ENSBTAG00000025314	hormone activity
 *
 * @author Olga Melnichuk
 * @version 1/19/12 11:31 PM
 */
public class MartPropertyValuesLoaderTest {

    private static List<String[]> TSV = new ArrayList<String[]>() {
        {
            add(new String[]{"type1", "type2", "prop"});
            add(new String[]{"ENSBTAT00000015116", "ENSBTAG00000025314", "extracellular region"});
            add(new String[]{"ENSBTAT00000057520", "ENSBTAG00000039669", ""});
            add(new String[]{"ENSBTAT00000015116", "ENSBTAG00000025314", "hormone activity"});
        }
    };

    private static Map<String, List<String>> TSV_TRANSPOSED = convert2map(transpose(TSV));

    @Test
    public void test() throws BioMartException, IOException, InvalidCSVColumnException, InvalidAnnotationDataException {
        BioMartAnnotationSource annotSource = newAnotationSource();
        ExternalBioEntityProperty extProp = annotSource.getExternalBioEntityProperties().iterator().next();
        BioEntityProperty prop = extProp.getBioEntityProperty();
        BioEntityAnnotationData.Builder builder = new BioEntityAnnotationData.Builder();
        new MartPropertyValuesLoader(annotSource, newMartClient(TSV)).load(extProp, builder);

        BioEntityAnnotationData data = builder.build(annotSource.getTypes());
        Collection<BEPropertyValue> propValues = data.getPropertyValues();
        List<String> expectedPropValues = TSV_TRANSPOSED.get("prop");
        for(BEPropertyValue propValue : propValues) {
            assertEquals(prop, propValue.getProperty());
            String v = propValue.getValue();
            assertTrue(!isNullOrEmpty(v));
            assertTrue(expectedPropValues.contains(v));
        }

        for(BioEntityType type : annotSource.getTypes()) {
            List<String> identifiers = TSV_TRANSPOSED.get(type.getName());
            Collection<Pair<String, BEPropertyValue>> values = data.getPropertyValuesForBioEntityType(type);
            for(Pair<String, BEPropertyValue> v : values) {
                assertTrue(identifiers.contains(v.getFirst()));
                int rowNum = expectedPropValues.indexOf(v.getSecond().getValue());
                assertEquals(
                        identifiers.get(rowNum),
                        v.getFirst());
            }
        }
    }

    private static BioMartAnnotationSource newAnotationSource() {
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
