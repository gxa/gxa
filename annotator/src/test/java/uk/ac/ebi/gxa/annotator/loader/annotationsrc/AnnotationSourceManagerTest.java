package uk.ac.ebi.gxa.annotator.loader.annotationsrc;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.loader.arraydesign.ArrayDesignService;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.gxa.dao.OrganismDAO;
import uk.ac.ebi.gxa.dao.SoftwareDAO;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityPropertyDAO;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityTypeDAO;

import java.util.Collection;

/**
 * User: nsklyar
 * Date: 27/10/2011
 */
@ContextConfiguration
public class AnnotationSourceManagerTest extends AtlasDAOTestCase {

    @Autowired
    private AnnotationSourceDAO annSrcDAO;
    @Autowired
    private OrganismDAO organismDAO;
    @Autowired
    private SoftwareDAO softwareDAO;
    @Autowired
    private BioEntityTypeDAO typeDAO;
    @Autowired
    private BioEntityPropertyDAO propertyDAO;

    @Autowired
    private ArrayDesignService arrayDesignService;

    private AnnotationSourceManager manager;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        manager = new AnnotationSourceManager();
        manager.setAnnSrcDAO(annSrcDAO);
        manager.setSoftwareDAO(softwareDAO);
    }
    @Test
    public void testGetCurrentAnnotationSources() throws Exception {
        final Collection<AnnotationSource> currentAnnotationSources = manager.getCurrentAnnotationSources();
        assertEquals(2, currentAnnotationSources.size());
    }

    @Test
    public void testGetAnnSrcString() throws Exception {

    }

    @Test
    public void testSaveAnnSrc() throws Exception {

    }
}
