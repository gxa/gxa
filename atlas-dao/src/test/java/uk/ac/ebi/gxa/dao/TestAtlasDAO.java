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

import uk.ac.ebi.microarray.atlas.model.*;

import java.util.List;
import java.util.Map;

/**
 * Actual tests for AtlasDAO, extends AtlasDAOTestCase which does all the handy instantiation of a basic, in memory DB.
 *
 * @author Tony Burdett
 */
public class TestAtlasDAO extends AtlasDAOTestCase {
    public void testGetLoadDetailsForExperiments() throws Exception {
        // expected number of details
        int expected = getDataSet().getTable("LOAD_MONITOR").getRowCount();

        // get number of details from the DAO
        int actual = getAtlasDAO().getLoadDetailsForExperiments().size();

        assertEquals("Wrong number of load details", expected, actual);

        // now check pages
        // todo - this uses oracle paging grammar, bad syntax for hsql
//            List<LoadDetails> details = getAtlasDAO().getLoadDetailsForExperimentsByPage(2, 1);
//
//            // should have one result, accession = "E-ABCD-456"
//            assertEquals("Wrong number of results! expected 1, got " + details.size(), details.size(), 1);
//            assertEquals("Wrong accession! expected E-ABCD-456, got " + details.get(0).getAccession(),
//                         details.get(0).getAccession(), "E-ABCD-456");
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

    public void testGetAllExperimentsPendingIndexing() {
        // test index pending
        List<Experiment> experiments =
                getAtlasDAO().getAllExperimentsPendingIndexing();
        for (Experiment exp : experiments) {
//      assertTrue(exp.isPendingIndexing()); // todo - how to test this?
        }
    }

    public void testGetAllExperimentsPendingNetCDFs() {
        // test netcdf pending
        List<Experiment> experiments =
                getAtlasDAO().getAllExperimentsPendingNetCDFs();
        for (Experiment exp : experiments) {
//      assertTrue(exp.isPendingNetCDF()); // todo - how to test this?
        }
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

    public void testGetAllGenes() throws Exception {
        int expected = 1;

        // get number of experiments from the DAO
        int actual = getBioEntityDAO().getAllGenesFast().size();

        // test data contains 2 experiments, check size of returned list
        assertEquals("Wrong number of genes", expected, actual);

        System.out.println(
                "Expected number of genes: " + expected + ", actual: " +
                        actual);
    }

    public void testGetPropertiesForGenes() throws Exception {
        List<Gene> genes = getBioEntityDAO().getAllGenesFast();

        // use dao to get properties
        getBioEntityDAO().getPropertiesForGenes(genes);

        // now check properties on each gene, compared with dataset
        for (Gene gene : genes) {
            List<Property> props = gene.getProperties();

            for (Property prop : props) {
                //loop over properties in the dataset to make sure we can find a matching one
                boolean found = false;
                int rows = getDataSet().getTable("A2_BIOENTITYPROPERTY").getRowCount();

                assertTrue(rows > 0);

                for (int i = 0; i < rows; i++) {
                    String propName =
                            getDataSet().getTable("A2_BIOENTITYPROPERTY").getValue(i, "name")
                                    .toString();

                    if (propName.equals(prop.getName())) {
                        System.out.println(
                                "Expected property: " + propName + ", " +
                                        "actual property: " + prop.getName());
                        found = true;
                        break;
                    }
                }

                assertTrue("Couldn't find Gene property named " + prop.getName(),
                        found);
            }
        }
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

    public void testPropertyValueCount() throws Exception {
        int expected = getDataSet().getTable("A2_PROPERTYVALUE").getRowCount();
        int size = getAtlasDAO().getPropertyValueCount();
        assertEquals("Different number of property values, expected: " + expected + ", actual: " + size, expected,
                size);
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

    public void testGetDesignElementsByArrayAccession() throws Exception {
        // fetch the accession of the first array design in our dataset
        String accession =
                getDataSet().getTable("A2_ARRAYDESIGN").getValue(0, "accession")
                        .toString();

        Map<Long, String> designElements =
                getAtlasDAO().getDesignElementsByArrayAccession(accession);

        // check the returned data
        for (Long deID : designElements.keySet()) {
            assertNotNull(deID);
            assertNotSame("Empty int for design element ID", deID, "");
            System.out.println("Got design element: " + deID);
        }
    }

    public void testGetDesignElementsByGeneID() throws Exception {
        // fetch the accession of the first gene in our dataset
        long id = 169968252;


        List<DesignElement> designElements = getBioEntityDAO().getDesignElementsByGeneID(id);

        // check the returned data
        assertNotNull(designElements);

        assertTrue("No design elements found", designElements.size() > 0 );
        for (DesignElement designElement : designElements) {
            assertNotNull(designElement);
        }

    }

    public void testGetOntologyMappingsForOntology() {
        String ontologyName = "EFO";

        List<OntologyMapping> ontologyMappings =
                getAtlasDAO().getOntologyMappingsByOntology(ontologyName);

        assertNotSame("Got zero ontology mappings", ontologyMappings.size(), 0);

        // todo: do some other checks once this code is implemented
    }
}
