package uk.ac.ebi.gxa.dao;

import org.junit.Test;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;

import java.util.List;

public class TestBioEntityDAO extends AtlasDAOTestCase {
    @Test
    public void testGetAllGenes() throws Exception {
        int expected = 1;

        // get number of experiments from the DAO
        int actual = bioEntityDAO.getAllGenesFast().size();

        // test data contains 2 experiments, check size of returned list
        assertEquals("Wrong number of genes", expected, actual);
    }

    @Test
    public void testGetPropertiesForGenes() throws Exception {
        List<BioEntity> bioEntities = bioEntityDAO.getAllGenesFast();

        // use dao to get properties
        bioEntityDAO.getPropertiesForGenes(bioEntities);

        // now check properties on each gene, compared with dataset
        for (BioEntity bioEntity : bioEntities) {
            List<BEPropertyValue> props = bioEntity.getProperties();

            for (BEPropertyValue prop : props) {
                //loop over properties in the dataset to make sure we can find a matching one
                boolean found = false;
                int rows = getDataSet().getTable("A2_BIOENTITYPROPERTY").getRowCount();

                assertTrue(rows > 0);

                for (int i = 0; i < rows; i++) {
                    String propName =
                            getDataSet().getTable("A2_BIOENTITYPROPERTY").getValue(i, "name")
                                    .toString();

                    if (propName.equals(prop.getProperty().getName())) {
                        System.out.println(
                                "Expected property: " + propName + ", " +
                                        "actual property: " + prop.getProperty().getName());
                        found = true;
                        break;
                    }
                }

                assertTrue("Couldn't find Gene property named " + prop.getProperty().getName(),
                        found);
            }
        }
    }
}
