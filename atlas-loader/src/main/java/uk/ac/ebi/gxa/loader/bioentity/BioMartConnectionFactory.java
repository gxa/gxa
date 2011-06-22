package uk.ac.ebi.gxa.loader.bioentity;

import uk.ac.ebi.microarray.atlas.model.annotation.BioMartAnnotationSource;
import uk.ac.ebi.microarray.atlas.model.annotation.MappingSource;

/**
 * User: nsklyar
 * Date: 22/06/2011
 */
public class BioMartConnectionFactory {

    public static BioMartConnection createConnectionForAnnSrc(BioMartAnnotationSource annSrc) throws BioMartAccessException {
        return new BioMartConnection(annSrc.getUrl(), annSrc.getDatabaseName(), annSrc.getDatasetName());
    }

    public static BioMartConnection createConnectionForMappingSrc(MappingSource mappingSource) {
        return null;
    }
}
