package uk.ac.ebi.gxa.annotator.loader.biomart;

import uk.ac.ebi.gxa.annotator.model.biomart.BioMartAnnotationSource;

/**
 * User: nsklyar
 * Date: 22/06/2011
 */
public class BioMartConnectionFactory {

    public static BioMartConnection createConnectionForAnnSrc(BioMartAnnotationSource annSrc) throws BioMartAccessException {
        return new BioMartConnection(annSrc.getUrl(), annSrc.getDatabaseName(), annSrc.getDatasetName());
    }
}
