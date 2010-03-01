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

package uk.ac.ebi.gxa.loader.handler.sdrf;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AssayNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.HybridizationNode;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.FactorValueNodeHandler;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.gxa.loader.utils.SDRFWritingUtils;
import uk.ac.ebi.microarray.atlas.model.Assay;

import java.util.List;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 19-Feb-2010
 */
public class AtlasLoadUpdatingFactorValueNodeHandler extends FactorValueNodeHandler {
    protected void writeValues() throws ObjectConversionException {
        // get the cache
        AtlasLoadCache cache = AtlasLoadCacheRegistry.getRegistry().retrieveAtlasLoadCache(investigation);

        // lookup hyb/assay nodes in the graph
        synchronized (investigation) {
            List<AssayNode> assayNodes = investigation.SDRF.lookupNodes(AssayNode.class);
            List<HybridizationNode> hybridizationNodes = investigation.SDRF.lookupNodes(HybridizationNode.class);

            // now, diff assay nodes with the assays in the cache
            for (AssayNode assayNode : assayNodes) {
                Assay assay = cache.fetchAssay(assayNode.getNodeName());

                if (assay != null) {
                    synchronized (assay) {
                        if (assay.getProperties() == null) {
                            if (assayNode.factorValues.size() != 0) {
                                getLog().debug("Factor Values need adding for " + assay.getAccession());
                                SDRFWritingUtils.writeAssayProperties(investigation, assay, assayNode);
                            }
                        }
                        else {
                            if (assay.getProperties().size() != assayNode.factorValues.size()) {
                                getLog().debug("Factor Values need updating for " + assay.getAccession());
                                SDRFWritingUtils.writeAssayProperties(investigation, assay, assayNode);
                            }
                        }
                    }
                }
            }

            // now, diff hyb nodes with the assays in the cache
            for (HybridizationNode hybridizationNode : hybridizationNodes) {
                Assay assay = cache.fetchAssay(hybridizationNode.getNodeName());

                if (assay != null) {
                    synchronized (assay) {
                        if (assay.getProperties() == null) {
                            if (hybridizationNode.factorValues.size() != 0) {
                                getLog().debug("Factor Values need adding for " + assay.getAccession());
                                SDRFWritingUtils.writeHybridizationProperties(investigation, assay, hybridizationNode);
                            }
                        }
                        else {
                            if (assay.getProperties().size() != hybridizationNode.factorValues.size()) {
                                getLog().debug("Factor Values need updating for " + assay.getAccession());
                                SDRFWritingUtils.writeHybridizationProperties(investigation, assay, hybridizationNode);
                            }
                        }
                    }
                }
            }
        }
    }
}
