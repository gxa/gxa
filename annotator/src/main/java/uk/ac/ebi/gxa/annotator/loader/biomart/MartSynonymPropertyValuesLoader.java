package uk.ac.ebi.gxa.annotator.loader.biomart;

import uk.ac.ebi.gxa.annotator.loader.data.BioEntityAnnotationData;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.util.Collection;

import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

/**
 * @author Olga Melnichuk
 * @version 1/16/12 2:43 PM
 */
class MartSynonymPropertyValuesLoader {

    private final BioMartAnnotationSource annotSource;

    MartSynonymPropertyValuesLoader(BioMartAnnotationSource annotSource) {
       this.annotSource = annotSource;
    }

    public void load(BioEntityProperty propSynonym, BioEntityAnnotationData.Builder builder) throws BioMartException {
        BioMartDbDAO bioMartDbDAO = new BioMartDbDAO(annotSource.getMySqlDbUrl());

        BioEntityType ensgene = annotSource.getBioEntityType(BioEntityType.ENSGENE);
        if (ensgene == null) {
            throw createUnexpected("Annotation source for " +
                    annotSource.getOrganism().getName() + " is not for genes. Cannot fetch synonyms.");
        }

        Collection<Pair<String, String>> geneToSynonyms =
                bioMartDbDAO.getSynonyms(annotSource.getMySqlDbName(), annotSource.getSoftware().getVersion());
        for (Pair<String, String> geneToSynonym : geneToSynonyms) {
            BEPropertyValue pv = new BEPropertyValue(null, propSynonym, geneToSynonym.getSecond());
            builder.addPropertyValue(geneToSynonym.getFirst(), ensgene, pv);
        }
    }
}
