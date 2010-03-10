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
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.loader.handler.idf;

import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl.AccessionHandler;
import uk.ac.ebi.arrayexpress2.magetab.utils.ParsingUtils;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.microarray.atlas.model.Experiment;

/**
 * A dedicated handler for creating experiment objects and storing them in the cache whenever a new investigation
 * accession is encountered.
 *
 * @author Tony Burdett
 * @date 26-Aug-2009
 */
public class AtlasLoadingAccessionHandler extends AccessionHandler {
    protected void writeValues() throws ObjectConversionException {
        // make sure we wait until IDF has finsihed reading
        ParsingUtils.waitForIDFToParse(investigation.IDF);

        // now, pull out the bits we need to create experiment objects
        if (investigation.accession != null) {
            Experiment experiment = new Experiment();
            experiment.setAccession(investigation.accession);

            // add the experiment to the cache
            AtlasLoadCache cache = AtlasLoadCacheRegistry.getRegistry()
                    .retrieveAtlasLoadCache(investigation);
            cache.setExperiment(experiment);
            synchronized (investigation) {
                investigation.notifyAll();
            }
        }
        else {
            // generate error item and throw exception
            String message = "There is no accession number defined - " +
                    "cannot load to the Atlas without an accession, " +
                    "use Comment[ArrayExpressAccession]";

            ErrorItem error =
                    ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                            .generateErrorItem(message, 501, this.getClass());

            throw new ObjectConversionException(error, true);
        }
    }
}

