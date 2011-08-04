package uk.ac.ebi.gxa.annotator.dao;

import junit.framework.Assert;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.hibernate.Transaction;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.test.context.ContextConfiguration;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartAnnotationSource;
import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.gxa.dao.OrganismDAO;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityPropertyDAO;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.io.InputStream;

/**
 * User: nsklyar
 * Date: 31/05/2011
 */
@ContextConfiguration
public class TestAnnotationSourceDAO extends AtlasDAOTestCase {

    private static final String ATLAS_BE_DATA_RESOURCE = "atlas-be-db.xml";
    @Autowired
    protected OrganismDAO organismDAO;

     @Autowired
    protected AnnotationSourceDAO annSrcDAO;

    @Autowired
    protected BioEntityPropertyDAO propertyDAO;

    public IDataSet getDataSet() throws Exception {
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(ATLAS_BE_DATA_RESOURCE);

        return new FlatXmlDataSetBuilder().build(in);
    }

     public void setUp() throws Exception {
         super.setUp();

     }

    public void setAnnSrcDAO(AnnotationSourceDAO annSrcDAO) {
        this.annSrcDAO = annSrcDAO;
    }

//    @Test
//    public void testSave() throws Exception {
//        Software software = annSrcDAO.findOrCreateSoftware("Ens", "60");
//
//        Organism organism = organismDAO.getById(1);
//        BioMartAnnotationSource annotationSource = new BioMartAnnotationSource(software, organism);
//
//        AbstractDAO<BioMartProperty> bmPropertyDAO = new AbstractDAO<BioMartProperty>(sessionFactory, BioMartProperty.class) {
//        };
//        List<BioMartProperty> bioMartProperties = bmPropertyDAO.getAll();
//        Assert.assertTrue(bioMartProperties.size() > 0);
//
//        for (BioMartProperty bioMartProperty : bioMartProperties) {
//            annotationSource.addBioMartProperty(bioMartProperty);
//        }
//
//        annSrcDAO.save(annotationSource);
//        Assert.assertNotNull(annotationSource.getAnnotationSrcId());
//
//        BioMartAnnotationSource annotationSourceCopy = annotationSource.createCopyForNewSoftware(new Software("Ens", "61"));
//        Assert.assertNull(annotationSourceCopy.getAnnotationSrcId());
//        annSrcDAO.save(annotationSourceCopy);
//        Assert.assertNotNull(annotationSourceCopy.getAnnotationSrcId());
//
//        System.out.println("annotationSourceCopy = " + annotationSourceCopy);
//    }

    @Test
    public void testSaveNew() throws Exception {
        Software software = annSrcDAO.findOrCreateSoftware("plants", "8");
        Organism organism = organismDAO.getByName("arabidopsis thaliana");

        BioMartAnnotationSource annotationSource = new BioMartAnnotationSource(software, organism);
        annotationSource.setDatabaseName("plant");
        annotationSource.setDatasetName("athaliana_eg_gene");
        annotationSource.setUrl("http://plants.ensembl.org/biomart/martservice?");

        BioEntityProperty goterm = propertyDAO.getByName("goterm");
        Assert.assertNotNull(goterm);
        annotationSource.addBioMartProperty("name_1006", goterm);

        annSrcDAO.save(annotationSource);
        Assert.assertNotNull(annotationSource.getAnnotationSrcId());

    }

    @Test
    public void testSaveNew1() throws Exception {
        Software software = annSrcDAO.findOrCreateSoftware( "plants", "8");
        Organism organism = organismDAO.getByName("arabidopsis thaliana");

        BioMartAnnotationSource annotationSource = new BioMartAnnotationSource(software, organism);
        annotationSource.setDatabaseName("plant");
        annotationSource.setDatasetName("athaliana_eg_gene");
        annotationSource.setUrl("http://plants.ensembl.org/biomart/martservice?");

        BioEntityProperty goterm = propertyDAO.getByName("goterm");
        annotationSource.addBioMartProperty("name_1006", goterm);

//        BioEntityType type1 = new BioEntityType(null, "new_type", 0);
//        annotationSource.addBioentityType(type1);

        BioEntityType type2 = bioEntityDAO.findOrCreateBioEntityType("enstranscript");
        annotationSource.addBioentityType(type2);
        
        org.hibernate.Session session = SessionFactoryUtils.getSession(sessionFactory, true);
        Transaction transaction = session.getTransaction();
        transaction.begin();
        annSrcDAO.save(annotationSource);
        Assert.assertNotNull(annotationSource.getAnnotationSrcId());
        transaction.commit();

        System.out.println("annotationSource = " + annotationSource);
    }

    public void testGetById() {
        AnnotationSource annotationSource = annSrcDAO.getById(1000);
        Assert.assertNotNull(annotationSource);
        //ToDo: Test values
    }

    @Test
    public void testFindAnnotationSource() {
        Software software = annSrcDAO.findOrCreateSoftware("Ensembl", "60");
        Organism organism = organismDAO.getByName("Homo Sapiens");

        BioMartAnnotationSource annotationSource = annSrcDAO.findAnnotationSource(software, organism, BioMartAnnotationSource.class);
        Assert.assertNotNull(annotationSource);

    }
}
