package uk.ac.ebi.gxa.annotator.loader;

import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.bioentity.EnsemblAnnotator;

/**
 * User: nsklyar
 * Date: 28/07/2011
 */
public class BioMartBioentityAnnotationCommand extends AnnotationCommand {
    private String annSrcId;

    public BioMartBioentityAnnotationCommand(String annSrcId) {
        this.annSrcId = annSrcId;
    }

    @Override
    public void execute() {
        EnsemblAnnotator ensemblAnnotator = factory.getEnsemblAnnotator();
        try {
            ensemblAnnotator.updateAnnotations(annSrcId, null);
        } catch (AtlasLoaderException e) {
            //ToDo: revise exception handling
            LogUtil.createUnexpected("Problem when updating Bioentity annotations", e);
        }
    }
}
