package uk.ac.ebi.gxa.annotator.loader.biomart;

import uk.ac.ebi.gxa.annotator.loader.AnnotationCommand;
import uk.ac.ebi.gxa.annotator.loader.listner.AnnotationLoaderListener;

/**
 * User: nsklyar
 * Date: 28/07/2011
 */
public class UpdateBioEntityAnnotationCommand extends AnnotationCommand {
    private String annSrcId;

    public UpdateBioEntityAnnotationCommand(String annSrcId) {
        this.annSrcId = annSrcId;
    }

    @Override
    public void execute(AnnotationLoaderListener listener){
        BioMartAnnotator bioMartAnnotator = factory.getEnsemblAnnotator();
        bioMartAnnotator.updateAnnotations(annSrcId);
        bioMartAnnotator.setListener(listener);
    }
}
