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

package uk.ac.ebi.gxa.annotator.loader;

import org.junit.Test;
import uk.ac.ebi.gxa.annotator.AnnotationException;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityAnnotationData;
import uk.ac.ebi.gxa.annotator.loader.data.DesignElementMappingData;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * User: nsklyar
 * Date: 31/08/2011
 */
public class BioMartParserTest {

   /* @Test
    public void testParseBioMartPropertyValues() throws Exception {
        List<BioEntityType> bioEntityTypes = initTypes();
        AnnotationParser<BioEntityAnnotationData> parser = getBioMartParser();

        Organism organism = new Organism(null, "test_org");
        parser.parseBioEntities(BioMartParserTest.class.getResource("bioentities.txt"), organism);

        BioEntityProperty go = new BioEntityProperty(null, "go");
        parser.parsePropertyValues(go, BioMartParserTest.class.getResource("properties.txt"));

        BioEntityAnnotationData data = parser.getData();
        ArrayList<BioEntity> transcripts = new ArrayList<BioEntity>(data.getBioEntitiesOfType(bioEntityTypes.get(0)));
        ArrayList<BioEntity> genes = new ArrayList<BioEntity>(data.getBioEntitiesOfType(bioEntityTypes.get(1)));

        assertEquals(3, transcripts.size());
        assertEquals(2, genes.size());

        assertEquals(2, data.getPropertyValues().size());
        for (BEPropertyValue value : data.getPropertyValues()) {
            assertTrue(value.getValue().equals("extracellular region") || value.getValue().equals("hormone activity"));
        }

        for (BioEntityType type : bioEntityTypes) {
            Collection<Pair<String, BEPropertyValue>> propertyValues = data.getPropertyValuesForBioEntityType(type);
            assertEquals(2, propertyValues.size());
            if (type.equals(new BioEntityType(null, "ensgene", 1))) {
                assertTrue(propertyValues.contains(Pair.create("ENSBTAG00000025314", new BEPropertyValue(go, "extracellular region"))));
                assertTrue(propertyValues.contains(Pair.create("ENSBTAG00000025314", new BEPropertyValue(go, "hormone activity"))));
            } else {
                assertTrue(propertyValues.contains(Pair.create("ENSBTAT00000015116", new BEPropertyValue(go, "extracellular region"))));
                assertTrue(propertyValues.contains(Pair.create("ENSBTAT00000015116", new BEPropertyValue(go, "hormone activity"))));
            }
        }
    }

    @Test
    public void testParseBioMartPropertyValuesMultiple() throws Exception {
        List<BioEntityType> bioEntityTypes = initTypes();
        AnnotationParser<BioEntityAnnotationData> parser = getBioMartParser();

        BioEntityProperty go = new BioEntityProperty(null, "go");
        BioEntityProperty testProp = new BioEntityProperty(null, "testProp");

        final List<BioEntityProperty> properties = Arrays.asList(go, testProp);

        parser.parsePropertyValues(properties, BioMartParserTest.class.getResourceAsStream("multiple_properties.txt"), true);

        BioEntityAnnotationData data = parser.getData();


        assertEquals(5, data.getPropertyValues().size());

        for (BioEntityType type : bioEntityTypes) {
            Collection<Pair<String, BEPropertyValue>> propertyValues = data.getPropertyValuesForBioEntityType(type);
            assertEquals(5, propertyValues.size());
            if (type.equals(new BioEntityType(null, "ensgene", 1))) {
                assertTrue(propertyValues.contains(Pair.create("ENSBTAG00000025314", new BEPropertyValue(go, "extracellular region"))));
                assertTrue(propertyValues.contains(Pair.create("ENSBTAG00000025314", new BEPropertyValue(go, "hormone activity"))));
                assertTrue(propertyValues.contains(Pair.create("ENSBTAG00000025314", new BEPropertyValue(testProp, "pv1"))));
                assertTrue(propertyValues.contains(Pair.create("ENSBTAG00000039669", new BEPropertyValue(testProp, "pv2"))));
                assertTrue(propertyValues.contains(Pair.create("ENSBTAG00000025314", new BEPropertyValue(testProp, "pv3"))));
            } else {
                assertTrue(propertyValues.contains(Pair.create("ENSBTAT00000015116", new BEPropertyValue(go, "extracellular region"))));
                assertTrue(propertyValues.contains(Pair.create("ENSBTAT00000015116", new BEPropertyValue(go, "hormone activity"))));
            }
        }
    }


    @Test(expected = AnnotationException.class)
    public void testParseBioMartIncorrectBioEntitites1() throws Exception {
        Organism organism = new Organism(null, "test_org");
        getBioMartParser().parseBioEntities(BioMartParserTest.class.getResource("bioentities_incorrect1.txt"), organism);
    }

    @Test(expected = AnnotationException.class)
    public void testParseBioMartIncorrectBioEntitites2() throws Exception {
        Organism organism = new Organism(null, "test_org");
        getBioMartParser().parseBioEntities(BioMartParserTest.class.getResource("bioentities_incorrect2.txt"), organism);
    }

    @Test(expected = AnnotationException.class)
    public void testParseBioMartIncorrectPropertyValues1() throws Exception {
        BioEntityProperty property = new BioEntityProperty(null, "go");
        getBioMartParser().parsePropertyValues(property, BioMartParserTest.class.getResource("properties_incorrect1.txt"));
    }

    @Test(expected = AnnotationException.class)
    public void testParseBioMartIncorrectPropertyValues2() throws Exception {
        BioEntityProperty property = new BioEntityProperty(null, "go");
        getBioMartParser().parsePropertyValues(property, BioMartParserTest.class.getResource("properties_incorrect2.txt"));
    }

    @Test
    public void testParseDesignElementMappings() throws Exception {
        List<BioEntityType> bioEntityTypes = initTypes();
        AnnotationParser<DesignElementMappingData> parser = getBioMartParserForDesignElements();

        parser.parseDesignElementMappings(BioMartParserTest.class.getResource("designelements.txt"));
        DesignElementMappingData data = parser.getData();

        for (BioEntityType type : bioEntityTypes) {
            Collection<Pair<String, String>> designElementToBioEntity = data.getDesignElementToBioEntity(type);
            assertEquals(3, designElementToBioEntity.size());
            if (type.equals(new BioEntityType(null, "ensgene", 1))) {
                assertTrue(designElementToBioEntity.contains(Pair.create("232832_at", "ENSG00000236057")));
                assertTrue(designElementToBioEntity.contains(Pair.create("235934_at", "ENSG00000245602")));
                assertTrue(designElementToBioEntity.contains(Pair.create("232835_at", "ENSG00000236057")));
            } else {
                assertTrue(designElementToBioEntity.contains(Pair.create("232832_at", "ENST00000537266")));
                assertTrue(designElementToBioEntity.contains(Pair.create("235934_at", "ENST00000500835")));
                assertTrue(designElementToBioEntity.contains(Pair.create("232835_at", "ENST00000446137")));
            }
        }
    }

    @Test
    public void testParseDesignElementMappings1() throws Exception {
        AnnotationParser<DesignElementMappingData> parser = getBioMartParserForDesignElements();

        Organism organism = new Organism(null, "test_org");
        parser.parseBioEntities(BioMartParserTest.class.getResource("bioentities.txt"), organism);

        DesignElementMappingData data = parser.getData();
        assertEquals(initTypes().size(), data.getBioEntityTypes().size());

    }

    @Test(expected = AnnotationException.class)
    public void testParseIncorrectDesignElementMappings1() throws Exception {
        getBioMartParserForDesignElements().parseDesignElementMappings(BioMartParserTest.class.getResource("designelements_incorrect1.txt"));
    }

    @Test(expected = AnnotationException.class)
    public void testParseIncorrectDesignElementMappings2() throws Exception {
        getBioMartParserForDesignElements().parseDesignElementMappings(BioMartParserTest.class.getResource("designelements_incorrect2.txt"));
    }

    private List<BioEntityType> initTypes() {
        List<BioEntityType> types = new ArrayList<BioEntityType>(2);
        types.add(new BioEntityType(null, "enstranscript", 1));
        types.add(new BioEntityType(null, "ensgene", 1));
        return types;
    }

    private AnnotationParser<BioEntityAnnotationData> getBioMartParser() {
        List<BioEntityType> bioEntityTypes = initTypes();
        BioEntityAnnotationDataBuilder builder = new BioEntityAnnotationDataBuilder();
        return AnnotationParser.initParser(bioEntityTypes, builder);
    }

    private AnnotationParser<DesignElementMappingData> getBioMartParserForDesignElements() {
        List<BioEntityType> bioEntityTypes = initTypes();
        DesignElementDataBuilder builder = new DesignElementDataBuilder();
        return AnnotationParser.initParser(bioEntityTypes, builder);
    }*/


}
