package uk.ac.ebi.gxa.annotator.loader;

import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.model.FileBasedAnnotationSource;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityPropertyDAO;

/**
 * User: nsklyar
 * Date: 03/01/2012
 */

public class AnnotatorFactory {

    @Autowired
    private AtlasBioEntityDataWriter beDataWriter;
    @Autowired
    private AnnotationSourceDAO annSrcDAO;
    @Autowired
    private BioEntityPropertyDAO propertyDAO;

    public BioMartAnnotator createBioMartAnnotator(BioMartAnnotationSource annSrc) {
        return new BioMartAnnotator( annSrc, annSrcDAO, propertyDAO, beDataWriter);
    }

    public <T extends FileBasedAnnotationSource> FileBasedAnnotator<T> createFileBasedAnnotator(T annSrc) {
        return new FileBasedAnnotator<T>(annSrc, beDataWriter);
    }
}
