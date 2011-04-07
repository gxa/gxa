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

package uk.ac.ebi.gxa.loader.handler.adf;

import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node.ADFNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node.ReporterNode;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.node.ReporterHandler;
import uk.ac.ebi.gxa.loader.utils.ADFWritingUtils;
import uk.ac.ebi.gxa.loader.utils.AtlasLoaderUtils;
import uk.ac.ebi.gxa.loader.utils.LookupException;
import uk.ac.ebi.microarray.atlas.model.ArrayDesignBundle;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 23-Feb-2010
 * @deprecated
 */
public class AtlasLoadingReporterHandler extends ReporterHandler {
    protected void writeValues() throws ObjectConversionException {
        try {
            // wait at least until we have the accession
            AtlasLoaderUtils.waitForArrayDesignAccession(arrayDesign);

            // get the array design bundle
            ArrayDesignBundle arrayBundle = AtlasLoaderUtils.waitForArrayDesignBundle(
                    arrayDesign.accession, arrayDesign, this.getClass().getSimpleName(), getLog());

            ADFNode node;
            while ((node = getNextNodeForCompilation()) != null) {
                if (node instanceof ReporterNode) {
                    ReporterNode reporter = (ReporterNode) node;

                    // write a design element matching the details from this node
                    String deName = reporter.getNodeName();
                    getLog().debug("Writing design element from composite element node '" + deName + "'");

                    if (!arrayBundle.getDesignElementNames().contains(reporter.getNodeName())) {
                        // add new design element
                        arrayBundle.addDesignElementName(reporter.getNodeName());
                    }

                    // now write all database entries
                    ADFWritingUtils.writeReporterDatabaseEntries(arrayBundle, deName, reporter);
                }
                else {
                    // generate error item and throw exception
                    String message =
                            "Unexpected node type - ReporterHandler should only make reporter " +
                                    "nodes available for writing, but actually got " + node.getNodeType();
                    ErrorItem error = ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                            .generateErrorItem(message, 999, this.getClass());

                    throw new ObjectConversionException(error, true);
                }
            }
        }
        catch (LookupException e) {
            // generate error item and throw exception
            String message =
                    "There is no accession number defined - cannot load to the Atlas " +
                            "without an accession, use Comment[ArrayExpressAccession]";

            ErrorItem error = ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                    .generateErrorItem(message, 501, this.getClass());

            throw new ObjectConversionException(error, true);
        }
    }
}
