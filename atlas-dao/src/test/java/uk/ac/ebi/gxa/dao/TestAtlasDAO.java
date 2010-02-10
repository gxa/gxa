package uk.ac.ebi.gxa.dao;

import uk.ac.ebi.microarray.atlas.model.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * Actual tests for AtlasDAO, extends AtlasDAOTestCase which does all the handy instantiation of a basic, in memory DB.
 *
 * @author Tony Burdett
 * @date 05-Oct-2009
 */
public class TestAtlasDAO extends AtlasDAOTestCase {
    public void testGetLoadDetailsForExperiments() {
        try {
            // expected number of details
            int expected = getDataSet().getTable("LOAD_MONITOR").getRowCount();

            // get number of details from the DAO
            int actual = getAtlasDAO().getLoadDetailsForExperiments().size();

            assertEquals("Wrong number of load details", expected, actual);

//            // now check pages
//            List<LoadDetails> details = getAtlasDAO().getLoadDetailsForExperimentsByPage(2, 1);
//
//            // should have one result, accession = "E-ABCD-456"
//            assertEquals("Wrong number of results! expected 1, got " + details.size(), details.size(), 1);
//            assertEquals("Wrong accession! expected E-ABCD-456, got " + details.get(0).getAccession(),
//                         details.get(0).getAccession(), "E-ABCD-456");
        }
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testGetAllExperiments() {
        try {

            getAtlasDAO().writeTest();

            if(1==1)
            return;

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
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
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

    public void testGetExperimentByAccession() {
        try {
            // fetch the accession of the first experiment in our dataset
            String accession = getDataSet().getTable("A2_EXPERIMENT")
                    .getValue(0, "accession").toString();
            int id = Integer.parseInt(getDataSet().getTable("A2_EXPERIMENT")
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
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testGetAllGenes() {
        try {
            // get row count of experiments in the dataset
            int expected = getDataSet().getTable("A2_GENE").getRowCount();

            // get number of experiments from the DAO
            int actual = getAtlasDAO().getAllGenes().size();

            // test data contains 2 experiments, check size of returned list
            assertEquals("Wrong number of genes", expected, actual);

            System.out.println(
                    "Expected number of genes: " + expected + ", actual: " +
                            actual);
        }
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testGetAllPendingGenes() {
        // test index pending
        List<Gene> genes = getAtlasDAO().getAllPendingGenes();
        for (Gene gene : genes) {
//      assertTrue(gene.isPending()); // todo - how to test this?
        }
    }

    public void testGetPropertiesForGenes() {
        List<Gene> genes = getAtlasDAO().getAllGenes();

        // use dao to get properties
        getAtlasDAO().getPropertiesForGenes(genes);

        // now check properties on each gene, compared with dataset
        try {
            for (Gene gene : genes) {
                List<Property> props = gene.getProperties();

                for (Property prop : props) {
                    //loop over properties in the dataset to make sure we can find a matching one
                    boolean found = false;
                    int rows = getDataSet().getTable("A2_GENEPROPERTY").getRowCount();
                    for (int i = 0; i < rows; i++) {
                        String propName =
                                getDataSet().getTable("A2_GENEPROPERTY").getValue(i, "name")
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
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testGetAssaysByExperimentAccession() {
        try {
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
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testGetExpressionValuesForAssays() {
        try {
            // fetch the accession of the first experiment in our dataset
            String accession =
                    getDataSet().getTable("A2_EXPERIMENT").getValue(0, "accession")
                            .toString();

            // get some assays for this experiment
            List<Assay> assays =
                    getAtlasDAO().getAssaysByExperimentAccession(accession);

            // populate their expression values
            getAtlasDAO().getExpressionValuesForAssays(assays);

            // now check EVS
            for (Assay assay : assays) {
                assertNotNull("Null collection of expression values",
                              assay.getAllExpressionValues());
                System.out.println("Assay " + assay.getAccession() + " has " +
                        assay.getAllExpressionValues().length + " expression values");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testGetSamplesByAssayAccession() {
        try {
            String accession =
                    getDataSet().getTable("A2_ASSAY").getValue(0, "accession")
                            .toString();

            List<Sample> samples =
                    getAtlasDAO().getSamplesByAssayAccession(accession);

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
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testOneSampleToManyAssays() {
        try {
            // use the accession of the assay that tests one to many
            String accession = "one:ToMany:TestAssay1";

            List<Sample> samples = getAtlasDAO().getSamplesByAssayAccession(accession);

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
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testPropertyValueCount() {
        try {
            int expected = getDataSet().getTable("A2_PROPERTYVALUE").getRowCount();

            int size = getAtlasDAO().getPropertyValueCount();

            assertEquals("Different number of property values, expected: " + expected + ", actual: " + size, expected,
                         size);
        }
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }

    }

    public void testGetAllArrayDesigns() {
        try {
            int expected = getDataSet().getTable("A2_ARRAYDESIGN").getRowCount();

            int actual = getAtlasDAO().getAllArrayDesigns().size();

            assertEquals("Wrong number of array designs", expected, actual);

            System.out.println(
                    "Expected number of array designs: " + expected + ", actual: " +
                            actual);
        }
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testGetArrayDesignByAccession() {
        try {
            String accession =
                    getDataSet().getTable("A2_ARRAYDESIGN").getValue(0, "accession")
                            .toString();
            int id =
                    Integer.parseInt(getDataSet().getTable("A2_ARRAYDESIGN")
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
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testGetDesignElementsByArrayAccession() {
        try {
            // fetch the accession of the first array design in our dataset
            String accession =
                    getDataSet().getTable("A2_ARRAYDESIGN").getValue(0, "accession")
                            .toString();

            Map<Integer, String> designElements =
                    getAtlasDAO().getDesignElementsByArrayAccession(accession);

            // check the returned data
            for (Integer deID : designElements.keySet()) {
                assertNotNull(deID);
                assertNotSame("Empty int for design element ID", deID, "");
                System.out.println("Got design element: " + deID);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testGetDesignElementsByGeneID() {
        try {
            // fetch the accession of the first gene in our dataset
            int id = Integer.parseInt(
                    getDataSet().getTable("A2_GENE").getValue(0, "geneid").toString());

            Map<Integer, String> designElements =
                    getAtlasDAO().getDesignElementsByGeneID(id);

            // check the returned data
            for (int deID : designElements.keySet()) {
                assertNotNull(deID);
                assertNotSame("Got 0 for design element ID", deID, 0);
                assertNotSame("Got -1 for design element ID", deID, -1);
                System.out.println("Got design element: " + deID);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testGetAtlasCountsByExperimentID() {
        try {
            // fetch the id of the first experiment in our dataset
            int id = Integer.parseInt(getDataSet().getTable("A2_EXPERIMENT").
                    getValue(0, "experimentid").toString());

            List<AtlasCount> atlasCounts =
                    getAtlasDAO().getAtlasCountsByExperimentID(id);

            // check the returned data
            assertNotSame("Zero atlas counts returned", atlasCounts.size(), 0);
            for (AtlasCount atlas : atlasCounts) {
                assertNotNull(atlas);
                assertNotNull("Got null property", atlas.getProperty());
                assertNotSame("Got null property value", atlas.getPropertyValue());
                assertNotNull("Got null updn" + atlas.getUpOrDown());
                assertNotNull("Got 0 gene count" + atlas.getGeneCount());
                System.out.println("AtlasCount: " + atlas.toString());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testGetExpressionAnalyticsByGeneID() {
        try {
            // fetch the accession of the first array design in our dataset
            int id = Integer.parseInt(
                    getDataSet().getTable("A2_GENE").getValue(0, "geneid").toString());

            System.out.println("Getting stats for Gene id: " + id);

            List<ExpressionAnalysis> eas =
                    getAtlasDAO().getExpressionAnalyticsByGeneID(id);

            // check we got results
            assertNotSame("Got 0 ExpressionAnalytics back", eas.size(), 0);

            // check the returned data
            for (ExpressionAnalysis ea : eas) {
                assertNotNull(ea);
                System.out.println("Got stats for " + id + ": " + ea.toString());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testGetOntologyMappingsForOntology() {
        String ontologyName = "EFO";

        List<OntologyMapping> ontologyMappings =
                getAtlasDAO().getOntologyMappingsByOntology(ontologyName);

        assertNotSame("Got zero ontology mappings", ontologyMappings.size(), 0);

        // todo: do some other checks once this code is implemented
    }

    public void testCallStoredProcedures() {
        try {
            Connection conn = getConnection().getConnection();

            Statement stmt = conn.createStatement();

            ResultSet rs = stmt.executeQuery("CALL SQRT(2)");
            while (rs.next()) {
                double expected = Math.sqrt(2);
                double actual = Double.parseDouble(rs.getString(1));

                assertEquals("Stored Procedure SQRT returns wrong answer",
                             expected, actual);
            }

            // cleanup
            rs.close();
            stmt.close();

            // now check we can call experimentset
            stmt = conn.createStatement();

            // just check this doesn't throw an exception
            stmt.executeQuery(
                    "CALL A2_EXPERIMENTSET('accession', 'description', 'performer', 'lab')");
            stmt.executeQuery(
                    "CALL A2_ASSAYSET('accession', 'E-ABCD-1234', 'A-ABCD-1234')");
            stmt.executeQuery(
                    "CALL A2_SAMPLESET('accession', null, null, 'species', 'channel')");

            // cleanup
            stmt.close();
            conn.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testWriteAssays() {
        // create test assay
        Assay assay = new Assay();
        assay.setAccession("assay-test-assay-1");
        assay.setExperimentAccession("E-PFIZ-2");
        assay.setArrayDesignAccession("A-MEXP-27");

        Property p1 = new Property();
        p1.setAccession("property1");
        p1.setName("property2");
        p1.setValue("hello");

        List<Property> p = new ArrayList<Property>();
        p.add(p1);

        //assay.setProperties(p);

        getAtlasDAO().writeAssay(assay);
    }
}