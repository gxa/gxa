/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.dao;

import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.microarray.atlas.model.*;

import java.util.Collections;
import java.util.List;

/**
 * Actual tests for AtlasDAO, extends AtlasDAOTestCase which does all the handy instantiation of a basic, in memory DB.
 *
 * @author Tony Burdett
 */
@Transactional
public class TestAtlasDAO extends AtlasDAOTestCase {

    private static final String ABC_ABCXYZ_SOME_THING_1234_ABC123 = "abc:ABCxyz:SomeThing:1234.ABC123";
    private static final String ARRAY_DESIGN_ACCESSION = "A-AFFY-45";
    private static final String E_MEXP_420 = "E-MEXP-420";
    private static final String PROPERTY_NAME = "SEX";
    private static final String PROPERTY_VALUE = "MALE";
    private static final String PROPERTY_VALUE1 = "value007";
    private static final String PROPERTY_VALUE2 = "value005";
    private static final String ONTOLOGY_TERM = "EFO_0000107";
    private static final String ONTOLOGY_NAME = "EFO";
    private static final String ONTOLOGY_VERSION = "Thu Oct 02 2008";
    private static final String ONTOLOGY_DESCRIPTION = "ArrayExpress Experimental Factor Ontology";

    @Test
    public void testGetAllExperiments() throws Exception {
        // get row count of experiments in the dataset
        int expected = getDataSet().getTable("A2_EXPERIMENT").getRowCount();

        // get number of experiments from the DAO
        int actual = atlasDAO.getAllExperiments().size();

        // test data contains 2 experiments, check size of returned list
        assertEquals("Wrong number of experiments", expected, actual);
    }

    @Test
    public void testGetExperimentByAccession() throws Exception {
        // fetch the accession of the first experiment in our dataset
        String accession = someExperimentAccession();
        long id = someExperimentId();

        // fetch the experiment using the DAO
        Experiment exp = atlasDAO.getExperimentByAccession(accession);

        // check the returned data
        assertNotNull(exp);
        assertEquals("Accessions don't match", exp.getAccession(), accession);
        assertEquals("IDs don't match", exp.getId(), Long.valueOf(id));
    }

    private long someExperimentId() throws Exception {
        return Long.parseLong(getDataSet().getTable("A2_EXPERIMENT").getValue(0, "experimentid").toString());
    }

    private String someExperimentAccession() throws Exception {
        return getDataSet().getTable("A2_EXPERIMENT").getValue(0, "accession").toString();
    }

    @Test
    public void testGetAssaysByExperimentAccession() throws Exception {
        // fetch the accession of the first experiment in our dataset
        String accession =
                someExperimentAccession();

        List<Assay> assays = atlasDAO.getExperimentByAccession(accession).getAssays();

        for (Assay assay : assays) {
            // check the returned data
            assertNotNull(assay);
            assertEquals("Accessions don't match", assay.getExperiment().getAccession(),
                    accession);
        }
    }

    @Test
    public void testGetExperimentsByArrayDesignAccession() throws Exception {
        final List<Experiment> experiments = experimentDAO.getExperimentsByArrayDesignAccession(ARRAY_DESIGN_ACCESSION);
        assertTrue("No experiment containing assays with array design accession: " + ARRAY_DESIGN_ACCESSION + " was found", experiments.size() > 0);
    }

    @Test
    public void testGetAssayPropertiesByPropertyValue() throws Exception {
        final List<Assay> assays = assayDAO.getAssaysByPropertyValue(PROPERTY_VALUE1);
        for (Assay assay : assays) {
            boolean found = false;
            for (AssayProperty prop : assay.getProperties()) {
                if (PROPERTY_VALUE1.equals(prop.getValue()))
                    found = true;
            }
            assertTrue("Assay property did not contain property value: " + PROPERTY_VALUE, found);
        }
    }

    @Test
    public void testGetSamplePropertiesByPropertyValue() throws Exception {
        final List<Sample> samples = sampleDAO.getSamplesByPropertyValue(PROPERTY_VALUE2);
        for (Sample sample : samples) {
            boolean found = false;
            for (SampleProperty prop : sample.getProperties()) {
                if (PROPERTY_VALUE2.equals(prop.getValue()))
                    found = true;
            }
            assertTrue("Sample did not contain property value: " + PROPERTY_VALUE2, found);
        }
    }

    @Test
    public void testGetAllArrayDesigns() throws Exception {
        int expected = getDataSet().getTable("A2_ARRAYDESIGN").getRowCount();

        int actual = arrayDesignDAO.getAllArrayDesigns().size();

        assertEquals("Wrong number of array designs", expected, actual);
    }

    @Test
    public void testGetArrayDesignByAccession() throws Exception {
        String accession =
                getDataSet().getTable("A2_ARRAYDESIGN").getValue(0, "accession")
                        .toString();
        Long id =
                Long.valueOf(getDataSet().getTable("A2_ARRAYDESIGN")
                        .getValue(0, "arraydesignid")
                        .toString());

        ArrayDesign arrayDesign =
                arrayDesignDAO.getArrayDesignByAccession(accession);

        // check the returned data
        assertNotNull(arrayDesign);
        assertEquals("Accessions don't match", arrayDesign.getAccession(),
                accession);
        assertEquals("IDs don't match", arrayDesign.getArrayDesignID(), id);
    }


    @Test
    public void testGetOntologyMappingsForOntology() {
        String ontologyName = "EFO";

        List<OntologyMapping> ontologyMappings =
                atlasDAO.getOntologyMappingsByOntology(ontologyName);

        assertNotSame("Got zero ontology mappings", ontologyMappings.size(), 0);

        // todo: do some other checks once this code is implemented
    }

    @Test
    public void testDeleteAssayProperties() throws Exception {
        final long termsCount = countOntologyTerms();

        removeAssayProperties();

        final Experiment experiment = experimentDAO.getByName(E_MEXP_420);
        final Assay assay = experiment.getAssay(ABC_ABCXYZ_SOME_THING_1234_ABC123);

        assertEquals("Properties are not deleted!", 0, assay.getProperties().size());
        assertEquals("Deleted the OntologyTerm - invalid cascading", termsCount,
                countOntologyTerms());
    }

    @Test
    public void testAddDeleteAssayProperty() throws Exception {
        final PropertyValue propertyValue = propertyValueDAO.getOrCreatePropertyValue(PROPERTY_NAME, PROPERTY_VALUE);

        addAssayProperty();

        Experiment experiment = experimentDAO.getByName(E_MEXP_420);
        Assay assay = experiment.getAssay(ABC_ABCXYZ_SOME_THING_1234_ABC123);
        assertTrue("Property not added", assay.hasProperty(propertyValue));

        removeAssayProperty();

        checkRemovalResults(propertyValue);
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void checkRemovalResults(PropertyValue propertyValue) throws RecordNotFoundException {
        Experiment experiment = experimentDAO.getByName(E_MEXP_420);
        Assay assay = experiment.getAssay(ABC_ABCXYZ_SOME_THING_1234_ABC123);
        assertFalse("Property not removed", assay.hasProperty(propertyValue));
    }

    private long countOntologyTerms() {
        JdbcTemplate template = new JdbcTemplate(atlasDataSource);

        return template.queryForLong("select count(*) from a2_ontologyterm");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void removeAssayProperties() throws RecordNotFoundException {
        final Experiment experiment = experimentDAO.getByName(E_MEXP_420);
        final Assay assay = experiment.getAssay(ABC_ABCXYZ_SOME_THING_1234_ABC123);
        assay.getProperties().clear();
        experimentDAO.save(experiment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void addAssayProperty() throws RecordNotFoundException {
        final Experiment experiment = experimentDAO.getByName(E_MEXP_420);
        final Assay assay = experiment.getAssay(ABC_ABCXYZ_SOME_THING_1234_ABC123);
        final PropertyValue propertyValue = propertyValueDAO.getOrCreatePropertyValue(PROPERTY_NAME, PROPERTY_VALUE);
        final Ontology ontology = ontologyDAO.getOrCreateOntology(ONTOLOGY_NAME, ONTOLOGY_DESCRIPTION, null, ONTOLOGY_VERSION);
        List<OntologyTerm> terms = Collections.singletonList(ontologyTermDAO.getOrCreateOntologyTerm(
                ONTOLOGY_TERM,
                null,
                null,
                ontology));
        assay.addOrUpdateProperty(propertyValue, terms);
        experimentDAO.save(experiment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void removeAssayProperty() throws RecordNotFoundException {
        final Experiment experiment = experimentDAO.getByName(E_MEXP_420);
        final Assay assay = experiment.getAssay(ABC_ABCXYZ_SOME_THING_1234_ABC123);
        final PropertyValue propertyValue = propertyValueDAO.getOrCreatePropertyValue(PROPERTY_NAME, PROPERTY_VALUE);
        assay.deleteProperty(propertyValue);
        experimentDAO.save(experiment);
    }
}
