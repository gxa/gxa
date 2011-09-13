package uk.ac.ebi.gxa.annotator.loader.biomart;

import org.junit.Test;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityAnnotationData;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityAnnotationDataBuilder;
import uk.ac.ebi.gxa.annotator.loader.data.DesignElementDataBuilder;
import uk.ac.ebi.gxa.annotator.loader.data.DesignElementMappingData;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * User: nsklyar
 * Date: 31/08/2011
 */
public class BioMartParserTest {

    @Test
    public void testParseBioMartPropertyValues() throws Exception {
        List<BioEntityType> bioEntityTypes = initTypes();

        BioEntityAnnotationDataBuilder builder = new BioEntityAnnotationDataBuilder();
        BioMartParser<BioEntityAnnotationData> parser = BioMartParser.initParser(bioEntityTypes, builder);

        Organism organism = new Organism(null, "test_org");
        parser.parseBioEntities(BioMartParserTest.class.getResource("bioentities.txt"), organism);

        BioEntityProperty go = new BioEntityProperty(null, "go");
        BioEntityProperty property = go;
        parser.parseBioMartPropertyValues(property, BioMartParserTest.class.getResource("properties.txt"));

        BioEntityAnnotationData data = parser.getData();
        for (BioEntityType type : bioEntityTypes) {
            Collection<BioEntity> bioEntitiesOfType = data.getBioEntitiesOfType(type);
            assertEquals(3, bioEntitiesOfType.size());
            for (BioEntity bioEntity : bioEntitiesOfType) {
                boolean passed = false;
                if (bioEntity.getIdentifier().equals("ENSBTAT00000057520")) {
                    assertEquals("Q6V947_BOVIN", bioEntity.getName());
                    passed = true;
                }
                if (bioEntity.getIdentifier().equals("ENSBTAG00000039669")) {
                    assertEquals("Q6V947_BOVIN", bioEntity.getName());
                    passed = true;
                }
                if (bioEntity.getIdentifier().equals("ENSBTAG00000035493")) {
                    assertEquals("ENSBTAG00000035493", bioEntity.getName());
                    passed = true;
                }
                if (bioEntity.getIdentifier().equals("ENSBTAT00000049990")) {
                    assertEquals("ENSBTAT00000049990", bioEntity.getName());
                    passed = true;
                }
                if (bioEntity.getIdentifier().equals("ENSBTAT00000015116")) {
                    assertEquals("Q2WGK0_BOVIN", bioEntity.getName());
                    passed = true;
                }
                if (bioEntity.getIdentifier().equals("ENSBTAG00000025314")) {
                    assertEquals("Q2WGK0_BOVIN", bioEntity.getName());
                    passed = true;
                }
                assertTrue(passed);
            }
        }

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
    public void testParseDesignElementMappings() throws Exception {
        List<BioEntityType> bioEntityTypes = initTypes();

        DesignElementDataBuilder builder = new DesignElementDataBuilder();
        BioMartParser<DesignElementMappingData> parser = BioMartParser.initParser(bioEntityTypes, builder);

        parser.parseDesignElementMappings(BioMartParserTest.class.getResource("designelements.txt"));
        DesignElementMappingData data = parser.getData();

        for (BioEntityType type : bioEntityTypes) {
            Collection<Pair<String, String>> designElementToBioEntity = data.getDesignElementToBioEntity(type);
            assertEquals(2, designElementToBioEntity.size());
            if (type.equals(new BioEntityType(null, "ensgene", 1))) {
                assertTrue(designElementToBioEntity.contains(Pair.create("232832_at", "ENSG00000236057")));
                assertTrue(designElementToBioEntity.contains(Pair.create("235934_at", "ENSG00000245602")));
            } else {
                assertTrue(designElementToBioEntity.contains(Pair.create("232832_at", "ENST00000537266")));
                assertTrue(designElementToBioEntity.contains(Pair.create("235934_at", "ENST00000500835")));
            }
        }

    }

    private List<BioEntityType> initTypes() {
        List<BioEntityType> types = new ArrayList<BioEntityType>(2);
        types.add(new BioEntityType(null, "enstranscript", 1));
        types.add(new BioEntityType(null, "ensgene", 1));
        return types;
    }


}
