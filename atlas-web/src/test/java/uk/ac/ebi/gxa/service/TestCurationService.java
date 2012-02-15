package uk.ac.ebi.gxa.service;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.gxa.exceptions.ResourceNotFoundException;
import uk.ac.ebi.microarray.atlas.api.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Robert Petryszak
 */
public class TestCurationService extends AtlasDAOTestCase {

    private static final String CELL_TYPE = "cell_type";
    private static final String PROP3 = "prop3";
    private static final String VALUE = "value";
    private static final String VALUE007 = "value007";
    private static final String VALUE004 = "value004";
    private static final String VALUE010 = "value010";
    private static final String MICROGLIAL_CELL = "microglial cell";
    private static final String E_MEXP_420 = "E-MEXP-420";
    private static final String ASSAY_ACC = "abc:ABCxyz:SomeThing:1234.ABC123";
    private static final String SAMPLE_ACC = "abc:some/Sample:ABC_DEFG_123a";
    private static final String EFO_0000827 = "EFO_0000827";
    private static final String EFO_0000828 = "EFO_0000828";


    private static final Function<ApiPropertyName, String> PROPERTY_NAME_FUNC =
            new Function<ApiPropertyName, String>() {
                @Override
                public String apply(@Nonnull ApiPropertyName input) {
                    return input.getName();
                }
            };

    private static final Function<ApiPropertyValue, String> PROPERTY_VALUE_FUNC =
            new Function<ApiPropertyValue, String>() {
                @Override
                public String apply(@Nonnull ApiPropertyValue input) {
                    return input.getValue();
                }
            };

    private static final Function<ApiProperty, String> PROPERTY_NAME_FUNC1 =
            new Function<ApiProperty, String>() {
                @Override
                public String apply(@Nonnull ApiProperty input) {
                    return input.getPropertyValue().getProperty().getName();
                }
            };

    private static final Function<ApiProperty, String> PROPERTY_VALUE_FUNC1 =
            new Function<ApiProperty, String>() {
                @Override
                public String apply(@Nonnull ApiProperty input) {
                    return input.getPropertyValue().getValue();
                }
            };

    @Autowired
    private CurationService curationService;

    @Test
    public void testGetProperties() throws Exception {
        Collection<ApiPropertyName> propertyNames = curationService.getPropertyNames();
        assertTrue("No property names found", propertyNames.size() > 0);
        assertTrue("Property name: " + CELL_TYPE + " not found", Collections2.transform(propertyNames, PROPERTY_NAME_FUNC).contains(CELL_TYPE));
    }

    @Test
    public void testGetPropertyValues() throws Exception {
        assertTrue("Property name: " + CELL_TYPE + " does not exist", Collections2.transform(curationService.getPropertyNames(), PROPERTY_NAME_FUNC).contains(CELL_TYPE));
        Collection<ApiPropertyValue> propertyValues = curationService.getPropertyValues(CELL_TYPE);
        assertTrue("No property values found", propertyValues.size() > 0);
        assertTrue("Property value: " + VALUE007 + " not found", Collections2.transform(propertyValues, PROPERTY_VALUE_FUNC).contains(VALUE007));
    }

    @Test
    public void testGetExperiment() throws Exception {
        try {
            curationService.getExperiment(E_MEXP_420);
        } catch (ResourceNotFoundException e) {
            fail("Experiment: " + E_MEXP_420 + " not found");
        }
    }

    @Test
    public void testGetExperimentsByPropertyOntologyTerm() throws Exception {
        assertTrue("Some assays or samples should contain property values mapped to ontology term: " + EFO_0000827,
                curationService.getExperimentsByOntologyTerm(EFO_0000827).size() > 0);
    }

    @Test
    public void testGetExperimentsByAssayPropertyValue() throws Exception {
        assertTrue("Some assays should contain property value: " + PROP3 + ":" + VALUE004,
                curationService.getExperimentsByPropertyValue(PROP3, VALUE004).size() > 0);
    }

    @Test
    public void testGetPropertyValueOntologyMappingsByPropertyValueExactMatch() throws Exception {
        assertTrue("Some assays or samples should contain property value: " + PROP3.toUpperCase() + ":" + VALUE004.toUpperCase(),
                curationService.getOntologyMappingsByPropertyValue(PROP3.toUpperCase(), VALUE004.toUpperCase(), true).size() > 0);
        assertTrue("Some assays or samples should contain property value: " + VALUE004.toUpperCase(),
                curationService.getOntologyMappingsByPropertyValue(null, VALUE004.toUpperCase(), true).size() > 0);
    }

    @Test
    public void testGetPropertyValueOntologyMappingsByPropertyValuePartialMatch() throws Exception {
        assertTrue("Some assays or samples should contain property value as a substring: " + PROP3.toUpperCase() + ":" + VALUE.toUpperCase(),
                curationService.getOntologyMappingsByPropertyValue(PROP3.toUpperCase(), VALUE.toUpperCase(), false).size() > 0);
        assertTrue("Some assays or samples should contain property value as a substring: " + VALUE.toUpperCase(),
                curationService.getOntologyMappingsByPropertyValue(null, VALUE.toUpperCase(), false).size() > 0);
    }

    @Test
    public void testReplacePropertyInAssays() throws Exception {
        // Test replace VALUE007 (already a property of ASSAY_ACC) with VALUE010 (not a property of ASSAY_ACC)
        Collection<ApiProperty> assayProperties = curationService.getAssayProperties(E_MEXP_420, ASSAY_ACC);

        assertTrue("Property : " + CELL_TYPE + ":" + VALUE007 + " not found in assay properties",
                propertyPresent(assayProperties, CELL_TYPE, VALUE007));

        curationService.replacePropertyInAssays(CELL_TYPE, PROP3);

        assertFalse("Property : " + CELL_TYPE + ":" + VALUE007 + " found in assay properties",
                propertyPresent(assayProperties, CELL_TYPE, VALUE007));
        assertTrue("Property : " + PROP3 + ":" + VALUE007 + " not found in assay properties",
                propertyPresent(assayProperties, PROP3, VALUE007));
    }

    @Test
    public void testReplacePropertyInAssays1() throws Exception {
        // Test replace MICROGLIAL_CELL with VALUE004 (both properties of ASSAY_ACC)
        Collection<ApiProperty> assayProperties = curationService.getAssayProperties(E_MEXP_420, ASSAY_ACC);

        assertTrue("Property : " + CELL_TYPE + ":" + MICROGLIAL_CELL + " not found in assay properties",
                propertyPresent(assayProperties, CELL_TYPE, MICROGLIAL_CELL));

        // First add VALUE004 to ASSAY_ACC properties
        Set<ApiOntologyTerm> terms = Sets.newHashSet();
        terms.add(curationService.getOntologyTerm(EFO_0000828));
        ApiProperty apiProperty = new ApiProperty(new ApiPropertyValue(new ApiPropertyName(PROP3), MICROGLIAL_CELL), terms);
        ApiProperty[] newProps = new ApiProperty[1];
        newProps[0] = apiProperty;
        curationService.putAssayProperties(E_MEXP_420, ASSAY_ACC, newProps);

        // Now that both CELL_TYPE:MICROGLIAL_CELL and PROP3:MICROGLIAL_CELL are both present in ASSAY_ACC, replace CELL_TYPE with PROP3
        curationService.replacePropertyInAssays(CELL_TYPE, PROP3);

        assertFalse("Property : " + CELL_TYPE + ":" + MICROGLIAL_CELL + " found in assay properties",
                propertyPresent(assayProperties, CELL_TYPE, MICROGLIAL_CELL));

        assertTrue("Property : " + PROP3 + ":" + MICROGLIAL_CELL + " not found in assay properties",
                propertyPresent(assayProperties, PROP3, MICROGLIAL_CELL));

        for (ApiProperty property : assayProperties) {
            if (PROP3.equals(property.getPropertyValue().getProperty().getName()) &&
                    MICROGLIAL_CELL.equals(property.getPropertyValue().getValue())) {
                Set<ApiOntologyTerm> newTerms = property.getTerms();
                assertEquals(2, newTerms.size());
                // Set of terms in the retained VALUE004 property should be a superset of terms assigned
                // to the replaced VALUE010 and to the replacing VALUE004
                assertTrue(newTerms + " doesn't contain " + curationService.getOntologyTerm(EFO_0000827),
                        newTerms.contains(curationService.getOntologyTerm(EFO_0000827))); // from property VALUE010
                assertTrue(newTerms + " doesn't contain " + curationService.getOntologyTerm(EFO_0000828),
                        newTerms.contains(curationService.getOntologyTerm(EFO_0000828))); // from property VALUE004
            }
        }
    }

    @Test
    public void testReplacePropertyInSamples() throws Exception {
        // Test replace PROP3 with CELL_TYPE
        Collection<ApiProperty> sampleProperties = curationService.getSampleProperties(E_MEXP_420, SAMPLE_ACC);

        assertTrue("Property : " + PROP3 + ":" + VALUE004 + " not found in sample properties",
                propertyPresent(sampleProperties, PROP3, VALUE004));

        curationService.replacePropertyInSamples(PROP3, CELL_TYPE);

        assertFalse("Property : " + PROP3 + ":" + VALUE004 + " found in sample properties",
                propertyPresent(sampleProperties, PROP3, VALUE004));
        assertTrue("Property : " + CELL_TYPE + ":" + VALUE004 + " not found in sample properties",
                propertyPresent(sampleProperties, CELL_TYPE, VALUE004));
    }

    @Test
    public void testReplacePropertyInSamples1() throws Exception {
         // Test replace PROP3 with CELL_TYPE, WHERE values of CELL_TYPE already exist in ASSAY
        Collection<ApiProperty> sampleProperties = curationService.getSampleProperties(E_MEXP_420, SAMPLE_ACC);

        assertTrue("Property : " + PROP3 + ":" + VALUE004 + " not found in sample properties",
                propertyPresent(sampleProperties, PROP3, VALUE004));

        // First add VALUE004 to ASSAY_ACC properties
        Set<ApiOntologyTerm> terms = Sets.newHashSet();
        terms.add(curationService.getOntologyTerm(EFO_0000828));
        ApiProperty apiProperty = new ApiProperty(new ApiPropertyValue(new ApiPropertyName(CELL_TYPE), VALUE004), terms);
        ApiProperty[] newProps = new ApiProperty[1];
        newProps[0] = apiProperty;
        curationService.putSampleProperties(E_MEXP_420, SAMPLE_ACC, newProps);

        // Now that both CELL_TYPE:VALUE004 and PROP3:VALUE004 are both present in SAMPLE_ACC, replace PROP3 with CELL_TYPE
        curationService.replacePropertyInSamples(PROP3, CELL_TYPE);

        assertFalse("Property : " + PROP3 + ":" + VALUE004 + " found in sample properties",
                propertyPresent(sampleProperties, PROP3, VALUE004));

        assertTrue("Property : " + CELL_TYPE + ":" + VALUE004 + " not found in sample properties",
                propertyPresent(sampleProperties, CELL_TYPE, VALUE004));

        for (ApiProperty property : sampleProperties) {
            if (CELL_TYPE.equals(property.getPropertyValue().getProperty().getName()) &&
                    VALUE004.equals(property.getPropertyValue().getValue())) {
                Set<ApiOntologyTerm> newTerms = property.getTerms();
                assertEquals(1, newTerms.size());
                assertTrue(newTerms + " doesn't contain " + curationService.getOntologyTerm(EFO_0000828),
                        newTerms.contains(curationService.getOntologyTerm(EFO_0000828))); 
            }
        }
    }



       @Test
    public void testReplacePropertyValueInAssays() throws Exception {
        // Test replace VALUE007 (already a property of ASSAY_ACC) with VALUE010 (not a property of ASSAY_ACC)
        Collection<ApiProperty> assayProperties = curationService.getAssayProperties(E_MEXP_420, ASSAY_ACC);

        assertTrue("Property : " + CELL_TYPE + ":" + VALUE007 + " not found in assay properties",
                propertyPresent(assayProperties, CELL_TYPE, VALUE007));
        assertFalse("Property : " + CELL_TYPE + ":" + VALUE010 + " found in assay properties",
                propertyPresent(assayProperties, CELL_TYPE, VALUE010));

        curationService.replacePropertyValueInAssays(CELL_TYPE, VALUE007, VALUE010);

        assertFalse("Property : " + CELL_TYPE + ":" + VALUE007 + " found in assay properties",
                propertyPresent(assayProperties, CELL_TYPE, VALUE007));
        assertTrue("Property : " + CELL_TYPE + ":" + VALUE010 + " not found in assay properties",
                propertyPresent(assayProperties, CELL_TYPE, VALUE010));
    }

    @Test
    public void testReplacePropertyValueInAssays1() throws Exception {
        // Test replace MICROGLIAL_CELL with VALUE004 (both properties of ASSAY_ACC)
        Collection<ApiProperty> assayProperties = curationService.getAssayProperties(E_MEXP_420, ASSAY_ACC);

        assertTrue("Property : " + CELL_TYPE + ":" + MICROGLIAL_CELL + " not found in assay properties",
                propertyPresent(assayProperties, CELL_TYPE, MICROGLIAL_CELL));

        // First add VALUE004 to ASSAY_ACC properties
        Set<ApiOntologyTerm> terms = Sets.newHashSet();
        terms.add(curationService.getOntologyTerm(EFO_0000828));
        ApiProperty apiProperty = new ApiProperty(new ApiPropertyValue(new ApiPropertyName(CELL_TYPE), VALUE004), terms);
        ApiProperty[] newProps = new ApiProperty[1];
        newProps[0] = apiProperty;
        curationService.putAssayProperties(E_MEXP_420, ASSAY_ACC, newProps);

        // Now that both MICROGLIAL_CELL and VALUE004 are both present in ASSAY_ACC, replace MICROGLIAL_CELL with VALUE004
        curationService.replacePropertyValueInAssays(CELL_TYPE, MICROGLIAL_CELL, VALUE004);

        assertFalse("Property : " + CELL_TYPE + ":" + MICROGLIAL_CELL + " found in assay properties",
                propertyPresent(assayProperties, CELL_TYPE, MICROGLIAL_CELL));
        assertTrue("Property : " + CELL_TYPE + ":" + VALUE004 + " not found in assay properties",
                propertyPresent(assayProperties, CELL_TYPE, VALUE004));

        for (ApiProperty property : assayProperties) {
            if (CELL_TYPE.equals(property.getPropertyValue().getProperty().getName()) &&
                    VALUE004.equals(property.getPropertyValue().getValue())) {
                Set<ApiOntologyTerm> newTerms = property.getTerms();
                assertEquals(2, newTerms.size());
                // Set of terms in the retained VALUE004 property should be a superset of terms assigned
                // to the replaced VALUE010 and to the replacing VALUE004
                assertTrue(newTerms + " doesn't contain " + curationService.getOntologyTerm(EFO_0000827),
                        newTerms.contains(curationService.getOntologyTerm(EFO_0000827))); // from property VALUE010
                assertTrue(newTerms + " doesn't contain " + curationService.getOntologyTerm(EFO_0000828),
                        newTerms.contains(curationService.getOntologyTerm(EFO_0000828))); // from property VALUE004
            }
        }
    }

    @Test
    public void testReplacePropertyValueInSamples() throws Exception {
        // Test replace VALUE004 (already a property of SAMPLE_ACC) with VALUE010 (not a property of SAMPLE_ACC)
        Collection<ApiProperty> sampleProperties = curationService.getSampleProperties(E_MEXP_420, SAMPLE_ACC);

        assertTrue("Property : " + PROP3 + ":" + VALUE004 + " not found in sample properties",
                propertyPresent(sampleProperties, PROP3, VALUE004));
        assertFalse("Property : " + PROP3 + ":" + VALUE010 + " found in sample properties",
                propertyPresent(sampleProperties, PROP3, VALUE010));

        curationService.replacePropertyValueInSamples(PROP3, VALUE004, VALUE010);

        assertFalse("Property : " + PROP3 + ":" + VALUE004 + " found in sample properties",
                propertyPresent(sampleProperties, PROP3, VALUE004));
        assertTrue("Property : " + PROP3 + ":" + VALUE010 + " not found in sample properties",
                propertyPresent(sampleProperties, PROP3, VALUE010));
    }

    @Test
    public void testReplacePropertyValueInSamples1() throws Exception {
        // Test replace VALUE004 with VALUE010 (both properties of SAMPLE_ACC)
        Collection<ApiProperty> sampleProperties = curationService.getSampleProperties(E_MEXP_420, SAMPLE_ACC);

        assertTrue("Property : " + PROP3 + ":" + VALUE004 + " not found in sample properties",
                propertyPresent(sampleProperties, PROP3, VALUE004));

        // First add VALUE010 to SAMPLE_ACC properties
        Set<ApiOntologyTerm> terms = Sets.newHashSet();
        terms.add(curationService.getOntologyTerm(EFO_0000828));
        ApiProperty apiProperty = new ApiProperty(new ApiPropertyValue(new ApiPropertyName(PROP3), VALUE010), terms);
        ApiProperty[] newProps = new ApiProperty[1];
        newProps[0] = apiProperty;
        curationService.putSampleProperties(E_MEXP_420, SAMPLE_ACC, newProps);

        // Now that both VALUE010 and VALUE004 are both present in SAMPLE_ACC, replace VALUE004 with VALUE010
        curationService.replacePropertyValueInSamples(PROP3, VALUE004, VALUE010);

        assertFalse("Property : " + PROP3 + ":" + VALUE004 + " found in sample properties",
                propertyPresent(sampleProperties, PROP3, VALUE004));
        assertTrue("Property : " + PROP3 + ":" + VALUE010 + " not found in sample properties",
                propertyPresent(sampleProperties, PROP3, VALUE010));

        for (ApiProperty property : sampleProperties) {
            if (PROP3.equals(property.getPropertyValue().getProperty().getName()) &&
                    VALUE010.equals(property.getPropertyValue().getValue())) {
                Set<ApiOntologyTerm> newTerms = property.getTerms();
                assertEquals(1, newTerms.size());
                // Set of terms in the retained VALUE004 property should be a superset of terms assigned
                // to the replaced VALUE010 and to the replacing VALUE004
                assertTrue(newTerms + " doesn't contain " + curationService.getOntologyTerm(EFO_0000828),
                        newTerms.contains(curationService.getOntologyTerm(EFO_0000828))); // from property VALUE010
            }
        }
    }


    @Test
    public void testDeletePropertyValue() throws Exception {
        assertTrue("Property : " + CELL_TYPE + ":" + VALUE007 + " not found in assay properties",
                propertyPresent(curationService.getAssayProperties(E_MEXP_420, ASSAY_ACC), CELL_TYPE, VALUE007));
        assertTrue("Property : " + PROP3 + ":" + VALUE004 + " not found in sample properties",
                propertyPresent(curationService.getSampleProperties(E_MEXP_420, SAMPLE_ACC), PROP3, VALUE004));

        curationService.deletePropertyValue(CELL_TYPE, VALUE007);
        curationService.deletePropertyValue(PROP3, VALUE004);

        assertFalse("Property : " + CELL_TYPE + ":" + VALUE007 + " not removed from assay properties",
                propertyPresent(curationService.getAssayProperties(E_MEXP_420, ASSAY_ACC), CELL_TYPE, VALUE007));
        assertFalse("Property : " + PROP3 + ":" + VALUE004 + " not removed from sample properties",
                propertyPresent(curationService.getSampleProperties(E_MEXP_420, SAMPLE_ACC), PROP3, VALUE004));

        Collection<ApiPropertyValue> propertyValues = curationService.getPropertyValues(CELL_TYPE);
        assertFalse("Property value: " + VALUE007 + " found", Collections2.transform(propertyValues, PROPERTY_VALUE_FUNC).contains(VALUE007));

        propertyValues = curationService.getPropertyValues(PROP3);
        assertFalse("Property value: " + VALUE004 + " found", Collections2.transform(propertyValues, PROPERTY_VALUE_FUNC).contains(VALUE004));
    }

    @Test
    public void testDeleteProperty() throws Exception {
        Collection<ApiPropertyName> propertyNames = curationService.getPropertyNames();
        assertTrue("Property: " + CELL_TYPE + " not found", Collections2.transform(propertyNames, PROPERTY_NAME_FUNC).contains(CELL_TYPE));
        curationService.deleteProperty(CELL_TYPE);
        propertyNames = curationService.getPropertyNames();
        assertFalse("Property: " + CELL_TYPE + " not removed", Collections2.transform(propertyNames, PROPERTY_NAME_FUNC).contains(CELL_TYPE));

    }

    @Test
    public void testGetAssayProperties() throws Exception {
        Collection<ApiProperty> properties = curationService.getAssayProperties(E_MEXP_420, ASSAY_ACC);
        assertTrue("No properties found in assay: " + ASSAY_ACC + " in experiment: " + E_MEXP_420, properties.size() > 0);
        assertTrue("Assay property name: " + CELL_TYPE + " not found", Collections2.transform(properties, PROPERTY_NAME_FUNC1).contains(CELL_TYPE));
        assertTrue("Assay property value: " + VALUE007 + " not found", Collections2.transform(properties, PROPERTY_VALUE_FUNC1).contains(VALUE007));
    }

    @Test
    public void testAddDeleteAssayProperties() throws Exception {
        Collection<ApiProperty> properties = curationService.getAssayProperties(E_MEXP_420, ASSAY_ACC);
        assertTrue("No properties found in sample: " + SAMPLE_ACC + " in experiment: " + E_MEXP_420, properties.size() > 0);

        ApiProperty apiProperty = properties.iterator().next();
        apiProperty.setPropertyValue(new ApiPropertyValue(new ApiPropertyName(PROP3), VALUE004));
        ApiProperty[] newProps = new ApiProperty[1];
        newProps[0] = apiProperty;

        curationService.deleteAssayProperties(E_MEXP_420, ASSAY_ACC, newProps);
        assertFalse("Property : " + PROP3 + ":" + VALUE004 + " not deleted in assay properties", propertyPresent(curationService.getAssayProperties(E_MEXP_420, ASSAY_ACC), PROP3, VALUE004));

        curationService.putAssayProperties(E_MEXP_420, ASSAY_ACC, newProps);
        assertTrue("Property : " + PROP3 + ":" + VALUE004 + " not added to assay properties", propertyPresent(curationService.getAssayProperties(E_MEXP_420, ASSAY_ACC), PROP3, VALUE004));

    }

    @Test
    public void testGetSampleProperties() throws Exception {
        Collection<ApiProperty> properties = curationService.getSampleProperties(E_MEXP_420, SAMPLE_ACC);
        assertTrue("No properties found in sample: " + SAMPLE_ACC + " in experiment: " + E_MEXP_420, properties.size() > 0);
        assertTrue("Sample property name: " + PROP3 + " not found", Collections2.transform(properties, PROPERTY_NAME_FUNC1).contains(PROP3));
        assertTrue("Sample property value: " + VALUE004 + " not found", Collections2.transform(properties, PROPERTY_VALUE_FUNC1).contains(VALUE004));
    }

    @Test
    public void testAddDeleteSampleProperties() throws Exception {
        Collection<ApiProperty> properties = curationService.getSampleProperties(E_MEXP_420, SAMPLE_ACC);
        assertTrue("No properties found in sample: " + SAMPLE_ACC + " in experiment: " + E_MEXP_420, properties.size() > 0);

        ApiProperty apiProperty = properties.iterator().next();
        apiProperty.setPropertyValue(new ApiPropertyValue(new ApiPropertyName(PROP3), VALUE004));
        ApiProperty[] newProps = new ApiProperty[1];
        newProps[0] = apiProperty;

        curationService.deleteSampleProperties(E_MEXP_420, SAMPLE_ACC, newProps);
        properties = curationService.getSampleProperties(E_MEXP_420, SAMPLE_ACC);
        assertFalse("Property : " + PROP3 + ":" + VALUE004 + " not deleted in sample properties", propertyPresent(properties, PROP3, VALUE004));

        curationService.putSampleProperties(E_MEXP_420, SAMPLE_ACC, newProps);
        properties = curationService.getSampleProperties(E_MEXP_420, SAMPLE_ACC);
        assertTrue("Property : " + PROP3 + ":" + VALUE004 + " not added to sample properties", propertyPresent(properties, PROP3, VALUE004));
    }

    @Test
    public void testGetUnusedProperties() throws Exception {
        assertTrue(curationService.getUnusedPropertyNames().size() > 0);
    }

    @Test
    public void testGetUnusedPropertyValues() throws Exception {
        assertTrue(curationService.getUnusedPropertyValues().size() > 0);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private boolean propertyPresent(Collection<ApiProperty> properties, String propertyName, @Nullable String propertyValue) {
        boolean found = false;
        for (ApiProperty property : properties) {
            if (propertyName.equals(property.getPropertyValue().getProperty().getName()) && (Strings.isNullOrEmpty(propertyValue) || propertyValue.equals(property.getPropertyValue().getValue())))
                found = true;
        }
        return found;
    }
}
