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

package uk.ac.ebi.gxa.loader.handler.sdrf;

import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.ArrayDesignNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AssayNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SDRFNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SourceNode;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.AssayHandler;
import uk.ac.ebi.arrayexpress2.magetab.utils.SDRFUtils;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.gxa.loader.utils.AtlasLoaderUtils;
import uk.ac.ebi.gxa.loader.utils.LookupException;
import uk.ac.ebi.gxa.loader.utils.SDRFWritingUtils;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A dedicated handler for creating assay objects and storing them in the cache whenever a new assay node is
 * encountered.
 *
 * @author Tony Burdett
 * @date 26-Aug-2009
 */
public class AtlasLoadingAssayHandler extends AssayHandler {
    public void writeValues() throws ObjectConversionException {
        if (investigation.accession != null) {
            SDRFNode node;
            while ((node = getNextNodeForCompilation()) != null) {
                if (node instanceof AssayNode) {
                    getLog().debug("Writing assay from assay node '" + node.getNodeName() + "'");
                    AssayNode assayNode = (AssayNode) node;

                    // fetch cache
                    AtlasLoadCache cache = AtlasLoadCacheRegistry.getRegistry().retrieveAtlasLoadCache(investigation);

                    // create/retrieve the new assay
                    Assay assay;
                    if (cache.fetchAssay(AtlasLoaderUtils.getNodeAccession(investigation, node)) != null) {
                        // get the existing sample
                        assay = cache.fetchAssay(AtlasLoaderUtils.getNodeAccession(investigation, node));
                        getLog().debug("Integrated assay with existing assay (" + assay.getAccession() + "), " +
                                "count now = " + cache.fetchAllAssays().size());
                    }
                    else {
                        // create a new sample and add it to the cache
                        assay = new Assay();
                        assay.setAccession(AtlasLoaderUtils.getNodeAccession(investigation, assayNode));
                        assay.setExperimentAccession(investigation.accession);
                        cache.addAssay(assay);
                        getLog().debug("Created new assay (" + assay.getAccession() + "), " +
                                "count now = " + cache.fetchAllAssays().size());

                        // and notify, as the investigation has updated
                        synchronized (investigation) {
                            investigation.notifyAll();
                        }
                    }

                    // add array design accession
                    List<String> arrayDesignAccessions = new ArrayList<String>();
                    for (ArrayDesignNode arrayDesignNode : assayNode.arrayDesigns) {
                        arrayDesignAccessions.add(arrayDesignNode.getNodeName());
                    }

                    // spec allows multiple array design references, but atlas allows one
                    if (arrayDesignAccessions.size() > 1) {
                        String message = "Assay references more than one array design, " +
                                "this is disallowed";

                        ErrorItem error = ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                                .generateErrorItem(message, 1018, this.getClass());

                        throw new ObjectConversionException(error, true);
                    }
                    else if (arrayDesignAccessions.size() == 0) {
                      String message = "Assay does not reference an Array Design - this cannot be loaded to the Atlas";

                      ErrorItem error = ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                              .generateErrorItem(message, 1018, this.getClass());

                      throw new ObjectConversionException(error, true);
                    }
                    else {
                        // only one, so set the accession
                        if (assay.getArrayDesignAccession() == null) {
                            assay.setArrayDesignAccession(arrayDesignAccessions.get(0));
                        }
                        else if (!assay.getArrayDesignAccession().equals(arrayDesignAccessions.get(0))) {
                            String message = "The same assay in the SDRF references two different array designs";

                            ErrorItem error = ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                                    .generateErrorItem(message, 1018, this.getClass());

                            throw new ObjectConversionException(error, true);
                        }
                        else {
                            // already set, and equal, so ignore
                        }
                    }

                    // now record any properties
                    SDRFWritingUtils.writeAssayProperties(investigation, assay, assayNode);

                    // finally, assays must be linked to their upstream samples
                    Collection<SourceNode> upstreamSources =
                            SDRFUtils.findUpstreamNodes(assayNode, SourceNode.class);

                    for (SourceNode source : upstreamSources) {
                        // retrieve the samples with the matching accession
                        try {
                            Sample sample = AtlasLoaderUtils.waitForSample(
                                    source.getNodeName(), investigation, getClass().getSimpleName(), getLog());

                            if (sample != null) {
                                if (sample.getAssayAccessions() == null ||
                                        !sample.getAssayAccessions().contains(assay.getAccession())) {
                                    getLog().trace("Updating " + sample.getAccession() + " with assay accession");
                                    sample.addAssayAccession(assay.getAccession());
                                }
                            }
                            else {
                                // no sample to link to in the cache - generate error item and throw exception
                                String message = "Assay " + assay.getAccession() + " is linked to sample " +
                                        source.getNodeName() + " but this sample is not due to be loaded. " +
                                        "This assay will not be linked to a sample";

                                ErrorItem error = ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                                        .generateErrorItem(message, 511, this.getClass());

                                throw new ObjectConversionException(error, false);
                            }
                        }
                        catch (LookupException e) {
                            // no sample to link to in the cache - generate error item and throw exception
                            String message = "Assay " + assay.getAccession() + " is linked to sample " +
                                    source.getNodeName() + " but this sample is not due to be loaded. " +
                                    "This assay will not be linked to a sample";

                            ErrorItem error = ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                                    .generateErrorItem(message, 511, this.getClass());

                            throw new ObjectConversionException(error, false);
                        }
                    }
                }
                else {
                    // generate error item and throw exception
                    String message =
                            "Unexpected node type - AssayHandler should only make assay " +
                                    "nodes available for writing, but actually " +
                                    "got " + node.getNodeType();
                    ErrorItem error = ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                            .generateErrorItem(message, 999, this.getClass());

                    throw new ObjectConversionException(error, true);
                }
            }
        }
        else {
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
