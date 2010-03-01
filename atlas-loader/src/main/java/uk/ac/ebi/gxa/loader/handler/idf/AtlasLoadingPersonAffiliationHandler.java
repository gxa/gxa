/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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
 * http://ostolop.github.com/gxa/
 */

package uk.ac.ebi.gxa.loader.handler.idf;

import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.PersonAffiliationHandler;
import uk.ac.ebi.arrayexpress2.magetab.utils.ParsingUtils;
import uk.ac.ebi.gxa.loader.utils.AtlasLoaderUtils;
import uk.ac.ebi.gxa.loader.utils.LookupException;
import uk.ac.ebi.microarray.atlas.model.Experiment;

/**
 * A dedicated handler for attaching a persons affiliation to the appropriate experiment object.
 *
 * @author Tony Burdett
 * @date 26-Aug-2009
 */
public class AtlasLoadingPersonAffiliationHandler
        extends PersonAffiliationHandler {
    protected void writeValues() throws ObjectConversionException {
        // make sure we wait until IDF has finsihed reading
        ParsingUtils.waitForIDFToParse(investigation.IDF);

        try {
            Experiment expt = AtlasLoaderUtils.waitForExperiment(
                    investigation.accession, investigation,
                    this.getClass().getSimpleName(), getLog());
            expt.setLab(investigation.IDF.personAffiliation.size() > 0
                    ? investigation.IDF.personAffiliation.get(0)
                    : "");
        }
        catch (LookupException e) {
            // generate error item and throw exception
            String message =
                    "Can't lookup experiment, no accession.  Creation will fail";
            ErrorItem error =
                    ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                            .generateErrorItem(
                                    message,
                                    501,
                                    this.getClass());

            throw new ObjectConversionException(error, true);
        }
    }
}