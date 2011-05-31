package uk.ac.ebi.gxa.dao;

import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;

import java.io.InputStream;
import java.util.List;

public class TestBioentityDAO extends AtlasDAOTestCase {

    private static final String ATLAS_BE_DATA_RESOURCE = "atlas-be-db.xml";

    protected IDataSet getDataSet() throws Exception {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(ATLAS_BE_DATA_RESOURCE);

        return new FlatXmlDataSetBuilder().build(in);
    }

    protected void setUp() throws Exception {

        // do dbunit setup
        super.setUp();

        // do our setup

        atlasDataSource = new SingleConnectionDataSource(
                getConnection().getConnection(), false);
//        atlasDAO = new AtlasDAO();
        JdbcTemplate template = new JdbcTemplate(atlasDataSource);
//        atlasDAO.setJdbcTemplate(template);
//        bioEntityDAO = new BioEntityDAO();
//        bioEntityDAO.setJdbcTemplate(template);

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
    }

    public void testGetPropertiesForGenes() throws Exception {
        List<BioEntity> bioEntities = getBioEntityDAO().getAllGenesFast();

        // use dao to get properties
        getBioEntityDAO().getPropertiesForGenes(bioEntities);

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
