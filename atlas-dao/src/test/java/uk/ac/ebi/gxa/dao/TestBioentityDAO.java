package uk.ac.ebi.gxa.dao;

import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import uk.ac.ebi.microarray.atlas.model.BioEntity;
import uk.ac.ebi.microarray.atlas.model.DesignElement;
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

        ArrayDesignDAOInterface arrayDesignDAO = new ArrayDesignDAO();
        arrayDesignDAO.setJdbcTemplate(template);

        atlasDAO.setBioEntityDAO(bioEntityDAO);
        atlasDAO.setArrayDesignDAO(arrayDesignDAO);
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
        List<BioEntity> bioEntities = getBioEntityDAO().getAllGenesFast();

        // use dao to get properties
        getBioEntityDAO().getPropertiesForGenes(bioEntities);

        // now check properties on each gene, compared with dataset
        for (BioEntity bioEntity : bioEntities) {
            List<Property> props = bioEntity.getProperties();

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
