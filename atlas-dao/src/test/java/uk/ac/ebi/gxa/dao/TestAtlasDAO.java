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

import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import uk.ac.ebi.microarray.atlas.model.*;

import java.io.InputStream;
import java.util.List;

/**
 * Actual tests for AtlasDAO, extends AtlasDAOTestCase which does all the handy instantiation of a basic, in memory DB.
 *
 * @author Tony Burdett
 */
public class TestAtlasDAO extends AtlasDAOTestCase {

    private static final String ATLAS_GENE_DATA_RESOURCE = "atlas-db.xml";

    protected IDataSet getDataSet() throws Exception {
         InputStream in = this.getClass().getClassLoader().getResourceAsStream(ATLAS_GENE_DATA_RESOURCE);

         return new FlatXmlDataSetBuilder().build(in);
     }

    protected void setUp() throws Exception {

        // do dbunit setup
        super.setUp();

        // do our setup

        atlasDataSource = new SingleConnectionDataSource(
                getConnection().getConnection(), false);
        atlasDAO = new AtlasDAO();
        JdbcTemplate template = new JdbcTemplate(atlasDataSource);
        atlasDAO.setJdbcTemplate(template);
        bioEntityDAO = new BioEntityDAO();
        bioEntityDAO.setJdbcTemplate(template);

        ArrayDesignDAOInterface arrayDesignDAO = new OldArrayDesignDAO();
        arrayDesignDAO.setJdbcTemplate(template);

        atlasDAO.setBioEntityDAO(bioEntityDAO);
        atlasDAO.setArrayDesignDAO(arrayDesignDAO);
    }

    public void testGetAllExperiments() throws Exception {
        // get row count of experiments in the dataset
        int expected = getDataSet().getTable("A2_EXPERIMENT").getRowCount();

        // get number of experiments from the DAO
        int actual = getAtlasDAO().getAllExperiments().size();

        // test data contains 2 experiments, check size of returned list
        assertEquals("Wrong number of experiments", expected, actual);

        System.out.println(
                "Expected number of experiments: " + expected + ", actual: " +
                        actual);
    }

    public void testGetExperimentByAccession() throws Exception {
        // fetch the accession of the first experiment in our dataset
        String accession = getDataSet().getTable("A2_EXPERIMENT")
                .getValue(0, "accession").toString();
        long id = Long.parseLong(getDataSet().getTable("A2_EXPERIMENT")
                .getValue(0, "experimentid").toString());

        // fetch the experiment using the DAO
        Experiment exp = getAtlasDAO().getExperimentByAccession(accession);

        // check the returned data
        assertNotNull(exp);
        assertEquals("Accessions don't match", exp.getAccession(), accession);
        assertEquals("IDs don't match", exp.getExperimentID(), id);

        System.out.println(
                "Fetched expected experiment id: " + id + " by accession: " +
                        accession + " successfully");
    }

    public void testGetAssaysByExperimentAccession() throws Exception {
        // fetch the accession of the first experiment in our dataset
        String accession =
                getDataSet().getTable("A2_EXPERIMENT").getValue(0, "accession")
                        .toString();

        List<Assay> assays =
                getAtlasDAO().getAssaysByExperimentAccession(accession);

        for (Assay assay : assays) {
            // check the returned data
            assertNotNull(assay);
            assertEquals("Accessions don't match", assay.getExperimentAccession(),
                    accession);

            System.out.println(
                    "Fetched expected assay id: " + assay.getAssayID() +
                            " by accession: " +
                            accession + " successfully");
        }
    }

    public void testGetSamplesByAssayAccession() throws Exception {
        String accession =
                getDataSet().getTable("A2_ASSAY").getValue(0, "accession")
                        .toString();

        //TODO:
        List<Sample> samples =
                getAtlasDAO().getSamplesByAssayAccession("experimentAccession", accession);

        for (Sample sample : samples) {
            // check the returned data
            assertNotNull(sample);
            assertNotNull(sample.getAssayAccessions());
            assertNotSame("Sample has zero assay accessions",
                    sample.getAssayAccessions().size(), 0);
            for (String acc : sample.getAssayAccessions()) {
                assertEquals("Accessions don't match", acc, accession);
            }

            System.out.println(
                    "Fetched expected sample id: " + sample.getSampleID() +
                            " by accession: " +
                            accession + " successfully");
        }
    }

    public void testOneSampleToManyAssays() {
        // use the accession of the assay that tests one to many
        String accession = "one:ToMany:TestAssay1";

        //TODO:
        List<Sample> samples = getAtlasDAO().getSamplesByAssayAccession("experimentAccession", accession);

        for (Sample sample : samples) {
            if (sample.getAccession().equals("one:ToMany:TestSample1")) {
                // check the returned data
                assertNotNull(sample);
                assertNotNull(sample.getAssayAccessions());
                assertNotSame("Sample has zero assay accessions",
                        sample.getAssayAccessions().size(), 0);
                assertTrue("Not enough assays - sample " + sample.getAccession() +
                        " should be related to more than 1 assay",
                        sample.getAssayAccessions().size() > 1);
            }
        }
    }

    public void testGetAllArrayDesigns() throws Exception {
        int expected = getDataSet().getTable("A2_ARRAYDESIGN").getRowCount();

        int actual = getAtlasDAO().getAllArrayDesigns().size();

        assertEquals("Wrong number of array designs", expected, actual);

        System.out.println(
                "Expected number of array designs: " + expected + ", actual: " +
                        actual);
    }

    public void testGetArrayDesignByAccession() throws Exception {
        String accession =
                getDataSet().getTable("A2_ARRAYDESIGN").getValue(0, "accession")
                        .toString();
        long id =
                Long.parseLong(getDataSet().getTable("A2_ARRAYDESIGN")
                        .getValue(0, "arraydesignid")
                        .toString());

        ArrayDesign arrayDesign =
                getAtlasDAO().getArrayDesignByAccession(accession);

        // check the returned data
        assertNotNull(arrayDesign);
        assertEquals("Accessions don't match", arrayDesign.getAccession(),
                accession);
        assertEquals("IDs don't match", arrayDesign.getArrayDesignID(), id);

        System.out.println(
                "Fetched expected array design id: " + id + " by accession: " +
                        accession + " successfully");
    }


    public void testGetOntologyMappingsForOntology() {
        String ontologyName = "EFO";

        List<OntologyMapping> ontologyMappings =
                getAtlasDAO().getOntologyMappingsByOntology(ontologyName);

        assertNotSame("Got zero ontology mappings", ontologyMappings.size(), 0);

        // todo: do some other checks once this code is implemented
    }
}
