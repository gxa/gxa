package uk.ac.ebi.gxa.annotator.loader.biomart;

import uk.ac.ebi.gxa.annotator.loader.AnnotationCommand;
import uk.ac.ebi.gxa.annotator.loader.listner.AnnotationLoaderListner;

/**
 * User: nsklyar
 * Date: 28/07/2011
 */
public class UpdateMappingCommand extends AnnotationCommand {
    private String annSrcId;
    private AnnotationLoaderListner listner;

    public UpdateMappingCommand(String annSrcId, AnnotationLoaderListner listner) {
        this.annSrcId = annSrcId;
        this.listner = listner;
    }

    @Override
    public void execute() {
        EnsemblAnnotator ensemblAnnotator = factory.getEnsemblAnnotator();
        ensemblAnnotator.updateMappings(annSrcId, listner);
    }
}
