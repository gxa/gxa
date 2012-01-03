package uk.ac.ebi.gxa.annotator.process;

import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.loader.AtlasBioEntityDataWriter;
import uk.ac.ebi.gxa.annotator.loader.biomart.BioMartAnnotator;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.model.genesigdb.GeneSigAnnotationSource;
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
        return new BioMartAnnotator((BioMartAnnotationSource) annSrc, annSrcDAO, propertyDAO, beDataWriter);
    }

    public FileBasedAnnotator createFileBasedAnnotator(GeneSigAnnotationSource annSrc) {
        return new FileBasedAnnotator((GeneSigAnnotationSource) annSrc, beDataWriter);
    }
}
