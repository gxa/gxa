package uk.ac.ebi.gxa.annotator.loader.biomart;

import uk.ac.ebi.gxa.annotator.AtlasAnnotationException;
import uk.ac.ebi.gxa.annotator.loader.AnnotationCommand;
import uk.ac.ebi.gxa.annotator.loader.listner.AnnotationLoaderListener;

/**
 * User: nsklyar
 * Date: 28/07/2011
 */
public class BioMartBioEntityAnnotationCommand extends AnnotationCommand {
    private String annSrcId;

    public BioMartBioEntityAnnotationCommand(String annSrcId) {
        this.annSrcId = annSrcId;
    }

    @Override
    public void execute(AnnotationLoaderListener listener) throws AtlasAnnotationException {
        EnsemblAnnotator ensemblAnnotator = factory.getEnsemblAnnotator();
        ensemblAnnotator.updateAnnotations(annSrcId, listener);
    }
}
