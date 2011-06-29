package uk.ac.ebi.gxa.dao;

import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.annotation.AnnotationSource;
import uk.ac.ebi.microarray.atlas.model.annotation.BioMartAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.annotation.BioMartProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.io.InputStream;
import java.util.HashSet;
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

        annotationSource.setBioMartProperties(new HashSet<BioMartProperty>(bioMartProperties));

        annotationSourceDAO.save(annotationSource);
        assertNotNull(annotationSource.getAnnotationSrcId());

        BioMartAnnotationSource annotationSourceCopy = annotationSource.createCopyForNewSoftware(new Software("Ens", "61"));
        assertNull(annotationSourceCopy.getAnnotationSrcId());
        annotationSourceDAO.save(annotationSourceCopy);
        assertNotNull(annotationSourceCopy.getAnnotationSrcId());

        System.out.println("annotationSourceCopy = " + annotationSourceCopy);
    }

    public void testSaveNew() throws Exception {
        Software software = new Software(null, "plants", "8");
        Organism organism = organismDAO.getByName("arabidopsis thaliana");

        BioMartAnnotationSource annotationSource = new BioMartAnnotationSource(software, organism);
        annotationSource.setDatabaseName("plant");
        annotationSource.setDatasetName("athaliana_eg_gene");
        annotationSource.setUrl("http://plants.ensembl.org/biomart/martservice?");

        BioEntityPropertyDAO bioEntityPropertyDAO = new BioEntityPropertyDAO(sessionFactory);

        BioEntityProperty goterm = bioEntityPropertyDAO.getByName("goterm");
        assertNotNull(goterm);
        annotationSource.addBioMartProperty("name_1006", goterm);

        annotationSourceDAO.save(annotationSource);
        assertNotNull(annotationSource.getAnnotationSrcId());

    }

    public void testSaveNew1() throws Exception {
        Software software = new Software(null, "plants", "8");
        Organism organism = organismDAO.getByName("arabidopsis thaliana");

        BioMartAnnotationSource annotationSource = new BioMartAnnotationSource(software, organism);
        annotationSource.setDatabaseName("plant");
        annotationSource.setDatasetName("athaliana_eg_gene");
        annotationSource.setUrl("http://plants.ensembl.org/biomart/martservice?");

        BioEntityPropertyDAO bioEntityPropertyDAO = new BioEntityPropertyDAO(sessionFactory);
        BioEntityProperty goterm = bioEntityPropertyDAO.getByName("goterm");
        annotationSource.addBioMartProperty("name_1006", goterm);

        BioEntityType type1 = new BioEntityType(null, "new_type", 0);
        annotationSource.addBioentityType(type1);

        BioEntityType type2 = bioEntityDAO.findOrCreateBioEntityType("enstranscript");
        annotationSource.addBioentityType(type2);
        
        org.hibernate.Session session = SessionFactoryUtils.getSession(sessionFactory, true);
        Transaction transaction = session.getTransaction();
        transaction.begin();
        annotationSourceDAO.save(annotationSource);
        assertNotNull(annotationSource.getAnnotationSrcId());
        transaction.commit();

        System.out.println("annotationSource = " + annotationSource);
    }

    public void testGetById() {
        AnnotationSource annotationSource = annotationSourceDAO.getById(1000);
        assertNotNull(annotationSource);
        //ToDo: Test values
    }

    public void testFindAnnotationSource() {
        Software software = softwareDAO.findOrCreate("Ensembl", "60");
        Organism organism = organismDAO.getByName("Homo Sapiens");

        BioMartAnnotationSource annotationSource = annotationSourceDAO.findAnnotationSource(software, organism, BioMartAnnotationSource.class);
        assertNotNull(annotationSource);

    }
}
