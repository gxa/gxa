package uk.ac.ebi.gxa.dao;

import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.AnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioMartAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioMartProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * User: nsklyar
 * Date: 31/05/2011
 */
public class TestAnnotationSourceDAO extends AtlasDAOTestCase {

    private static final String ATLAS_BE_DATA_RESOURCE = "atlas-be-db.xml";
    protected OrganismDAO organismDAO;

    protected IDataSet getDataSet() throws Exception {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(ATLAS_BE_DATA_RESOURCE);

        return new FlatXmlDataSetBuilder().build(in);
    }

     protected void setUp() throws Exception {
         super.setUp();
         organismDAO = new OrganismDAO(sessionFactory);

     }
    public void testSave() throws Exception {
        Software software = new Software(null, "Ens", "60");

        Organism organism = organismDAO.getById(1);
        BioMartAnnotationSource annotationSource = new BioMartAnnotationSource(software, organism);

        AbstractDAO<BioMartProperty> bmPropertyDAO = new AbstractDAO<BioMartProperty>(sessionFactory, BioMartProperty.class) {
        };
        List<BioMartProperty> bioMartProperties = bmPropertyDAO.getAll();
        assertTrue(bioMartProperties.size() > 0);

        annotationSource.setBioMartProperties(bioMartProperties);

        annotationSourceDAO.save(annotationSource);
        assertNotNull(annotationSource.getAnnotationSrcId());

        BioMartAnnotationSource annotationSourceCopy = annotationSource.createCopy(new Software("Ens", "61"));
        assertNull(annotationSourceCopy.getAnnotationSrcId());
        annotationSourceDAO.save(annotationSourceCopy);
        assertNotNull(annotationSourceCopy.getAnnotationSrcId());
    }

    public void testGetById() {
        AnnotationSource annotationSource = annotationSourceDAO.getById(1000);
        assertNotNull(annotationSource);
        //ToDo: Test values
    }

    public void testFindAnnotationSource() {
        Software software = softwareDAO.getById(1);
        Organism organism = organismDAO.getById(1);

        BioMartAnnotationSource annotationSource = annotationSourceDAO.findAnnotationSource(software, organism, BioMartAnnotationSource.class);
        assertNotNull(annotationSource);
    }
}
