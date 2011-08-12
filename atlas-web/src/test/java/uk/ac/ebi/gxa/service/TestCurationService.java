package uk.ac.ebi.gxa.service;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.microarray.atlas.api.*;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * @author Robert Petryszak
 */
public class TestCurationService extends AtlasDAOTestCase {

    private static final String PROPERTY_NAME1 = "cell_type";
    private static final String PROPERTY_NAME2 = "PROP3";
    private static final String PROPERTY_VALUE1 = "value007";
    private static final String PROPERTY_VALUE2 = "value005";
    private static final String PROPERTY_VALUE3 = "value004";
    private static final String E_MEXP_420 = "E-MEXP-420";
    private static final String ASSAY_ACC = "abc:ABCxyz:SomeThing:1234.ABC123";
    private static final String SAMPLE_ACC = "abc:some/Sample:ABC_DEFG_123a";
    private static final String EFO = "EFO";
    private static final String EFO_0000827 = "EFO_0000827";

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
        assertTrue(propertyNames.size() > 0);
        assertTrue(Collections2.transform(propertyNames, PROPERTY_NAME_FUNC).contains(PROPERTY_NAME1));
    }

    @Test
    public void testGetPropertiesValues() throws Exception {
        Collection<ApiPropertyValue> propertyValues = curationService.getPropertyValues(PROPERTY_NAME1);
        assertTrue(propertyValues.size() > 0);
        assertTrue(Collections2.transform(propertyValues, PROPERTY_VALUE_FUNC).contains(PROPERTY_VALUE1));
    }

    @Test
    public void testGetExperiment() throws Exception {
        ApiExperiment exp = curationService.getExperiment(E_MEXP_420);
        assertNotNull("Experiment: " + E_MEXP_420 + " not found", exp);
    }

    @Test
    public void testGetAssay() throws Exception {
        ApiAssay exp = curationService.getAssay(E_MEXP_420, ASSAY_ACC);
        assertNotNull("Assay: " + ASSAY_ACC + " in experiment: " + E_MEXP_420 + " not found", exp);
    }

    @Test
    public void testGetSample() throws Exception {
        ApiSample exp = curationService.getSample(E_MEXP_420, SAMPLE_ACC);
        assertNotNull("Sample: " + SAMPLE_ACC + " in experiment: " + E_MEXP_420 + " not found", exp);
    }

    @Test
    public void testGetAssayProperties() throws Exception {
        Collection<ApiProperty> properties = curationService.getAssayProperties(E_MEXP_420, ASSAY_ACC);
        assertTrue("No properties found in assay: " + ASSAY_ACC + " in experiment: " + E_MEXP_420, properties.size() > 0);
        assertTrue(Collections2.transform(properties, PROPERTY_NAME_FUNC1).contains(PROPERTY_NAME1));
        assertTrue(Collections2.transform(properties, PROPERTY_VALUE_FUNC1).contains(PROPERTY_VALUE1));
    }

    @Test
    public void testGetSampleProperties() throws Exception {
        Collection<ApiProperty> properties = curationService.getSampleProperties(E_MEXP_420, SAMPLE_ACC);
        assertTrue("No properties found in sample: " + SAMPLE_ACC + " in experiment: " + E_MEXP_420, properties.size() > 0);
        assertTrue(Collections2.transform(properties, PROPERTY_NAME_FUNC1).contains(PROPERTY_NAME2));
        assertTrue(Collections2.transform(properties, PROPERTY_VALUE_FUNC1).contains(PROPERTY_VALUE3));
    }

    @Test
    public void testGetOntology() throws Exception {
        ApiOntology ontology = curationService.getOntology(EFO);
        assertNotNull("Ontology: " + EFO + " not found ", ontology);
    }

    @Test
    public void testGetOntologyTerm() throws Exception {
        ApiOntologyTerm ontologyTerm = curationService.getOntologyTerm(EFO_0000827);
        assertNotNull("Ontology term: " + EFO_0000827 + " not found ", ontologyTerm);
    }


}
