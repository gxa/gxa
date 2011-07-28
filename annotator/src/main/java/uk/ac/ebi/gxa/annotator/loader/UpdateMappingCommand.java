package uk.ac.ebi.gxa.annotator.loader;

import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.bioentity.EnsemblAnnotator;

/**
 * User: nsklyar
 * Date: 28/07/2011
 */
public class UpdateMappingCommand extends AnnotationCommand {
    private String annSrcId;

    public UpdateMappingCommand(String annSrcId) {
        this.annSrcId = annSrcId;
    }

    @Override
    public void execute() {
        EnsemblAnnotator ensemblAnnotator = factory.getEnsemblAnnotator();
        try {
            ensemblAnnotator.updateMappings(annSrcId, null);
        } catch (AtlasLoaderException e) {
                //ToDo: revise exception handling
            LogUtil.createUnexpected("Problem when updating design element mappings", e);
        }
    }
}
