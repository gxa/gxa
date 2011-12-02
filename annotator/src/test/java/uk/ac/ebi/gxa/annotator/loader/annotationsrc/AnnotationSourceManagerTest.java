package uk.ac.ebi.gxa.annotator.loader.annotationsrc;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.model.AnnotationSourceClass;
import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.gxa.dao.SoftwareDAO;

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
    private SoftwareDAO softwareDAO;

    @Autowired
    private ConverterFactory annotationSourceConverterFactory;
    private AnnotationSourceManager manager;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        manager = new AnnotationSourceManager();
        manager.setAnnotationSourceConverterFactory(annotationSourceConverterFactory);
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
        final String annSrcString = manager.getAnnSrcString("1000", AnnotationSourceClass.BIOMART);
        assertEquals(BioMartAnnotationSourceConverterTest.ANN_SRC_DB, annSrcString.trim());
    }

    @Test
    public void testSaveAnnSrc() throws Exception {
        manager.saveAnnSrc(null, AnnotationSourceClass.FILE, FileBasedAnnotationSourceConverterTest.ANN_SRC);
        final Collection<? extends AnnotationSource> sources = annSrcDAO.getAnnotationSourcesOfType(AnnotationSourceClass.FILE.getClazz());
        assertEquals(2, sources.size());
    }
}
