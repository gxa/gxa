/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

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
