package uk.ac.ebi.gxa.service;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.gxa.exceptions.ResourceNotFoundException;
import uk.ac.ebi.microarray.atlas.api.*;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * @author Robert Petryszak
 */
public class TestCurationService extends AtlasDAOTestCase {

    private static final String CELL_TYPE = "cell_type";
    private static final String PROP3 = "prop3";
    private static final String VALUE007 = "value007";
    private static final String VALUE004 = "value004";
    private static final String VALUE010 = "value010";
    private static final String E_MEXP_420 = "E-MEXP-420";
    private static final String ASSAY_ACC = "abc:ABCxyz:SomeThing:1234.ABC123";
    private static final String SAMPLE_ACC = "abc:some/Sample:ABC_DEFG_123a";
    private static final String EFO = "EFO";
    private static final String VBO = "VBO";
    private static final String EFO_0000827 = "EFO_0000827";
    private static final String VBO_0000001 = "VBO_0000001";

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

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

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
    public void testReplacePropertyValueInAssays() throws Exception {
        assertTrue("Property : " + CELL_TYPE + ":" + VALUE007 + " not found in assay properties",
                propertyPresent(curationService.getAssayProperties(E_MEXP_420, ASSAY_ACC), CELL_TYPE, VALUE007));
        assertFalse("Property : " + CELL_TYPE + ":" + VALUE010 + " found in assay properties",
                propertyPresent(curationService.getAssayProperties(E_MEXP_420, ASSAY_ACC), CELL_TYPE, VALUE010));

        curationService.replacePropertyValueInAssays(CELL_TYPE, VALUE007, VALUE010);

        assertFalse("Property : " + CELL_TYPE + ":" + VALUE007 + " found in assay properties",
                propertyPresent(curationService.getAssayProperties(E_MEXP_420, ASSAY_ACC), CELL_TYPE, VALUE007));
        assertTrue("Property : " + CELL_TYPE + ":" + VALUE010 + " not found in assay properties",
                propertyPresent(curationService.getAssayProperties(E_MEXP_420, ASSAY_ACC), CELL_TYPE, VALUE010));
    }

    @Test
    public void testReplacePropertyValueInSamples() throws Exception {
        assertTrue("Property : " + PROP3 + ":" + VALUE004 + " not found in sample properties",
                propertyPresent(curationService.getSampleProperties(E_MEXP_420, SAMPLE_ACC), PROP3, VALUE004));
        assertFalse("Property : " + PROP3 + ":" + VALUE010 + " found in sample properties",
                propertyPresent(curationService.getSampleProperties(E_MEXP_420, SAMPLE_ACC), PROP3, VALUE010));

        curationService.replacePropertyValueInSamples(PROP3, VALUE004, VALUE010);

        assertFalse("Property : " + PROP3 + ":" + VALUE004 + " found in sample properties",
                propertyPresent(curationService.getSampleProperties(E_MEXP_420, SAMPLE_ACC), PROP3, VALUE004));
        assertTrue("Property : " + PROP3 + ":" + VALUE010 + " not found in sample properties",
                propertyPresent(curationService.getSampleProperties(E_MEXP_420, SAMPLE_ACC), PROP3, VALUE010));
    }


    @Test
    public void testRemovePropertyValue() throws Exception {
        assertTrue("Property : " + CELL_TYPE + ":" + VALUE007 + " not found in assay properties",
                propertyPresent(curationService.getAssayProperties(E_MEXP_420, ASSAY_ACC), CELL_TYPE, VALUE007));
        assertTrue("Property : " + PROP3 + ":" + VALUE004 + " not found in sample properties",
                propertyPresent(curationService.getSampleProperties(E_MEXP_420, SAMPLE_ACC), PROP3, VALUE004));

        curationService.removePropertyValue(CELL_TYPE, VALUE007);
        curationService.removePropertyValue(PROP3, VALUE004);

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
    public void testGetAssay() throws Exception {
        try {
            curationService.getAssay(E_MEXP_420, ASSAY_ACC);
        } catch (ResourceNotFoundException e) {
            fail("Assay: " + ASSAY_ACC + " in experiment: " + E_MEXP_420 + " not found");
        }
    }

    @Test
    public void testGetSample() throws Exception {
        try {
            curationService.getSample(E_MEXP_420, SAMPLE_ACC);
        } catch (ResourceNotFoundException e) {
            fail("Sample: " + SAMPLE_ACC + " in experiment: " + E_MEXP_420 + " not found");
        }
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
    public void testGetOntology() throws Exception {
        ApiOntology ontology = curationService.getOntology(EFO);
        assertNotNull("Ontology: " + EFO + " not found ", ontology);
    }

    @Test
    public void testPutOntology() throws Exception {
        ApiOntology ontology = curationService.getOntology(EFO);
        ontology.setName(VBO);
        try {
            curationService.getOntology(VBO);
            fail("Ontology: " + VBO + " already exists");
        } catch (ResourceNotFoundException e) {

        }

        curationService.putOntology(ontology);
        try {
            curationService.getOntology(VBO);
        } catch (ResourceNotFoundException e) {
            fail("Failed to create ontology: " + VBO);
        }
    }

    @Test
    public void testGetOntologyTerm() throws Exception {
        ApiOntologyTerm ontologyTerm = curationService.getOntologyTerm(EFO_0000827);
        assertNotNull("Ontology term: " + EFO_0000827 + " not found ", ontologyTerm);
    }

    @Test
    public void testPutOntologyTerms() throws Exception {
        ApiOntologyTerm ontologyTerm = curationService.getOntologyTerm(EFO_0000827);

        try {
            curationService.getOntologyTerm(VBO_0000001);
            fail("Ontology term: " + VBO_0000001 + " already exists");
        } catch (ResourceNotFoundException e) {

        }

        ontologyTerm.setAccession(VBO_0000001);

        ApiOntologyTerm[] ontologyTerms = new ApiOntologyTerm[1];
        ontologyTerms[0] = ontologyTerm;
        curationService.putOntologyTerms(ontologyTerms);

        try {
            curationService.getOntologyTerm(VBO_0000001);
        } catch (ResourceNotFoundException e) {
            fail("Failed to create ontology term: " + VBO_0000001);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private boolean propertyPresent(Collection<ApiProperty> properties, String propertyName, String propertyValue) {
        boolean found = false;
        for (ApiProperty property : properties) {
            if (propertyName.equals(property.getPropertyValue().getProperty().getName()) && propertyValue.equals(property.getPropertyValue().getValue()))
                found = true;
        }
        return found;
    }
}
