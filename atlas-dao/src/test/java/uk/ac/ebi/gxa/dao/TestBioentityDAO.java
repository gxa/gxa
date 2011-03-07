package uk.ac.ebi.gxa.dao;

import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import uk.ac.ebi.microarray.atlas.model.DesignElement;
import uk.ac.ebi.microarray.atlas.model.Gene;
import uk.ac.ebi.microarray.atlas.model.Property;

import java.io.InputStream;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: nsklyar
 * Date: 07/03/2011
 * Time: 14:15
 * To change this template use File | Settings | File Templates.
 */
public class TestBioentityDAO extends AtlasDAOTestCase {

    private static final String ATLAS_BE_DATA_RESOURCE = "atlas-be-db.xml";

    protected IDataSet getDataSet() throws Exception {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(ATLAS_BE_DATA_RESOURCE);

        return new FlatXmlDataSet(in);
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

    public void testGetDesignElementsByGeneID() throws Exception {
        // fetch the accession of the first gene in our dataset
        long id = 169968252;


        List<DesignElement> designElements = getBioEntityDAO().getDesignElementsByGeneID(id);

        // check the returned data
        assertNotNull(designElements);

        assertTrue("No design elements found", designElements.size() > 0);
        for (DesignElement designElement : designElements) {
            assertNotNull(designElement);
        }

    }
}
