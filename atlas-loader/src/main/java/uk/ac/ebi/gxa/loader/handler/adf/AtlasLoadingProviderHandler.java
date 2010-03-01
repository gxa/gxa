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

package uk.ac.ebi.gxa.loader.handler.adf;

import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl.ProviderHandler;
import uk.ac.ebi.gxa.loader.utils.AtlasLoaderUtils;
import uk.ac.ebi.gxa.loader.utils.LookupException;
import uk.ac.ebi.microarray.atlas.model.ArrayDesignBundle;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 23-Feb-2010
 */
public class AtlasLoadingProviderHandler extends ProviderHandler {
    protected void writeValues() throws ObjectConversionException {
        try {
            // wait until we have acquired the array design accession from parsing
            AtlasLoaderUtils.waitForArrayDesignAccession(arrayDesign);

            ArrayDesignBundle arrayBundle = AtlasLoaderUtils.waitForArrayDesignBundle(
                    arrayDesign.accession, arrayDesign, this.getClass().getSimpleName(), getLog());

            arrayBundle.setProvider(arrayDesign.ADF.provider);
        }
        catch (LookupException e) {
            // generate error item and throw exception
            String message =
                    "Can't lookup array design bundle, no accession.  Creation will fail";
            ErrorItem error =
                    ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                            .generateErrorItem(message, 501, this.getClass());

            throw new ObjectConversionException(error, true);
        }
    }
}
