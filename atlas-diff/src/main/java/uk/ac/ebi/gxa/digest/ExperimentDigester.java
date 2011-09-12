/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.digest;

import uk.ac.ebi.gxa.utils.DigestUtil;
import uk.ac.ebi.microarray.atlas.model.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;
import static uk.ac.ebi.gxa.utils.DigestUtil.update;

public class ExperimentDigester {
    public byte[] digest(final Experiment experiment) {
        try {
            MessageDigest digest = DigestUtil.getDigestInstance();

            // TODO: replace this with annotations and reflection
            update(digest, experiment.getAccession());
            update(digest, experiment.getAbstract());
            update(digest, experiment.getDescription());
            update(digest, experiment.getLab());
            update(digest, experiment.getPerformer());
            update(digest, experiment.getPubmedId());
            update(digest, "ASSAYS");
            for (Assay assay : experiment.getAssays()) {
                update(digest, assay.getAccession());
                update(digest, assay.getArrayDesign().getAccession());
                update(digest, "samples for " + assay.getAccession());
                for (Sample sample : assay.getSamples()) {
                    update(digest, sample.getAccession());
                }
                update(digest, "properties for " + assay.getAccession());
                for (AssayProperty property : assay.getProperties()) {
                    update(digest, property.getName());
                    update(digest, property.getValue());
                    for (OntologyTerm term : property.getTerms())
                        update(digest, term.getAccession());
                }
            }
            update(digest, "SAMPLES");
            for (Sample sample : experiment.getSamples()) {
                update(digest, sample.getAccession());
                update(digest, sample.getChannel());
                update(digest, sample.getOrganism().getName());
                update(digest, "properties for " + sample.getAccession());
                for (SampleProperty property : sample.getProperties()) {
                    update(digest, property.getName());
                    update(digest, property.getValue());
                    for (OntologyTerm term : property.getTerms()) {
                        update(digest, term.getAccession());
                    }
                }
            }

            // TODO: add NetCDF data to the digest
            // TODO: add asset data to the digest

//            digest.update(netcdfDigest);
//            digest.update(assetDigest);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw createUnexpected("Cannot get a digester");
        }
    }
}
