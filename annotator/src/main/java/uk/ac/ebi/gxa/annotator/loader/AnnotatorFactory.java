package uk.ac.ebi.gxa.annotator.loader;

import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.loader.biomart.BioMartAnnotator;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityPropertyDAO;

/**
 * User: nsklyar
 * Date: 28/07/2011
 */
public class AnnotatorFactory {

    private AtlasBioEntityDataWriter beDataWriter;
    private AnnotationSourceDAO annSrcDAO;
    private BioEntityPropertyDAO propertyDAO;

    public AnnotatorFactory(AtlasBioEntityDataWriter beDataWriter, AnnotationSourceDAO annotationSourceDAO, BioEntityPropertyDAO propertyDAO) {
        this.beDataWriter = beDataWriter;
        this.annSrcDAO = annotationSourceDAO;
        this.propertyDAO = propertyDAO;
    }

    public BioMartAnnotator getEnsemblAnnotator() {
        return new BioMartAnnotator(annSrcDAO, propertyDAO, beDataWriter);
    }

}
