package uk.ac.ebi.gxa.annotator.loader;

import uk.ac.ebi.gxa.annotator.loader.biomart.BioMartAnnotator;

/**
 * User: nsklyar
 * Date: 28/07/2011
 */
public class AnnotatorFactory {

    private AtlasBioEntityDataWriter beDataWriter;

    public AnnotatorFactory(AtlasBioEntityDataWriter beDataWriter) {
        this.beDataWriter = beDataWriter;
    }

    public BioMartAnnotator getEnsemblAnnotator() {
        return new BioMartAnnotator(beDataWriter);
    }

}
