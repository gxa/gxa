package uk.ac.ebi.gxa.annotator.loader.biomart;

import uk.ac.ebi.gxa.annotator.loader.AnnotationCommand;
import uk.ac.ebi.gxa.annotator.loader.listner.AnnotationLoaderListner;

/**
 * User: nsklyar
 * Date: 28/07/2011
 */
public class BioMartBioentityAnnotationCommand extends AnnotationCommand {
    private String annSrcId;
    private AnnotationLoaderListner listner;


    public BioMartBioentityAnnotationCommand(String annSrcId, AnnotationLoaderListner listner) {
        this.listner = listner;
        this.annSrcId = annSrcId;

    }

    @Override
    public void execute() {
        EnsemblAnnotator ensemblAnnotator = factory.getEnsemblAnnotator();
        ensemblAnnotator.updateAnnotations(annSrcId, listner);
    }
}
