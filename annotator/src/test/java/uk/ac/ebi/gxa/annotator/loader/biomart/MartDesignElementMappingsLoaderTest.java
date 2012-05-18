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
import uk.ac.ebi.gxa.annotator.loader.data.DesignElementMappingData;
import uk.ac.ebi.gxa.annotator.loader.data.InvalidAnnotationDataException;
import uk.ac.ebi.gxa.annotator.loader.util.InvalidCSVColumnException;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.model.ExternalArrayDesign;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.DesignElement;
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
 * It checks that the data loaded from a .tsv file correctly parsed by the parser.
 * The content of .tsv file are generated from the predefined values to easy verify
 * data afterwards.
 * <p/>
 * ID_PROPERTY_1    ID_PROPERTY_2   DESIGN_ELEMENT
 * ENST00000537266	ENSG00000236057	232832_at
 * ENST00000446137	ENSG00000236057	232835_at
 * ENST00000500835	ENSG00000245602	235934_at
 *
 * @author Olga Melnichuk
 */
public class MartDesignElementMappingsLoaderTest {

    private static List<String[]> TSV = new ArrayList<String[]>() {
        {
            add(new String[]{"type1", "type2", "arrayDesign"});
            add(new String[]{"ENST00000537266", "ENSG00000236057", "232832_at"});
            add(new String[]{"ENST00000446137", "ENSG00000236057", "232835_at"});
            add(new String[]{"ENST00000500835", "ENSG00000245602", "235934_at"});
        }
    };

    private static Map<String, List<String>> TSV_TRANSPOSED = convert2map(transpose(TSV));

    @Test
    public void test() throws BioMartException, IOException, InvalidCSVColumnException, InvalidAnnotationDataException {
        BioMartAnnotationSource annotSource = newAnnotationSource();
        ExternalArrayDesign extArrayDesign = annotSource.getExternalArrayDesigns().iterator().next();
        DesignElementMappingData.Builder builder = new DesignElementMappingData.Builder();
        (new MartDesignElementMappingsLoader(annotSource, newMartClient(TSV))).load(extArrayDesign, builder);

        DesignElementMappingData data = builder.build(annotSource.getTypes());
        Collection<DesignElement> designElements = data.getDesignElements();
        List<String> expectedDesignElements = TSV_TRANSPOSED.get("arrayDesign");
        for (DesignElement de : designElements) {
            String deAcc = de.getAccession();
            assertTrue(!isNullOrEmpty(deAcc));
            assertTrue(expectedDesignElements.contains(deAcc));
        }

        for(BioEntityType type : annotSource.getTypes()) {
            List<String> identifiers = TSV_TRANSPOSED.get(type.getName());
            Collection<Pair<String, String>> pairs = data.getDesignElementToBioEntity(type);
            for (Pair<String, String> p : pairs) {
                assertTrue(identifiers.contains(p.getSecond()));
                int rowNum = expectedDesignElements.indexOf(p.getFirst());
                assertEquals(
                        identifiers.get(rowNum),
                        p.getSecond());
            }
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
                .arrayDesign("ACCESSION-123", "ExternalArrayDesignName")
                .create();

    }
}
