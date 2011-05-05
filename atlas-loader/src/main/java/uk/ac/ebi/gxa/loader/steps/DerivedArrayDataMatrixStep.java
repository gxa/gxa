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

package uk.ac.ebi.gxa.loader.steps;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AssayNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.DerivedArrayDataMatrixNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.HybridizationNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SDRFNode;
import uk.ac.ebi.arrayexpress2.magetab.utils.SDRFUtils;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.gxa.loader.datamatrix.DataMatrixFileBuffer;
import uk.ac.ebi.gxa.loader.service.MAGETABInvestigationExt;
import uk.ac.ebi.microarray.atlas.model.Assay;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Experiment loading step that prepares derived data matrix to be stored into a NetCDF file.
 * Based on the original handlers code by Tony Burdett.
 *
 * @author Nikolay Pultsin
 * @date Aug-2010
 */


public class DerivedArrayDataMatrixStep implements Step {
    private final MAGETABInvestigationExt investigation;
    private final AtlasLoadCache cache;
    private final Log log = LogFactory.getLog(this.getClass());

    public DerivedArrayDataMatrixStep(MAGETABInvestigationExt investigation) {
        this.investigation = investigation;
        this.cache = AtlasLoadCacheRegistry.getRegistry().retrieveAtlasLoadCache(investigation);
    }

    public String displayName() {
        return "Processing derived data matrix";
    }

    public void run() throws AtlasLoaderException {
        if (investigation.userData.get(ArrayDataStep.SUCCESS_KEY) == ArrayDataStep.SUCCESS_KEY) {
            log.info("Raw data are used; processed data will not be processed");
            return;
        }

        for (DerivedArrayDataMatrixNode node : investigation.SDRF.lookupNodes(DerivedArrayDataMatrixNode.class)) {
            log.info("Writing expression values from data file referenced by " +
                    "derived array data matrix node '" + node.getNodeName() + "'");

            // sdrf location
            URL sdrfURL = investigation.SDRF.getLocation();

            File sdrfFilePath = new File(sdrfURL.getFile());
            File relPath = new File(sdrfFilePath.getParentFile(), node.getNodeName());

            // try to get the relative filename
            URL dataMatrixURL;
            try {
                // NB. making sure we replace File separators with '/' to guard against windows issues
                dataMatrixURL = sdrfURL.getPort() == -1
                        ? new URL(sdrfURL.getProtocol(),
                        sdrfURL.getHost(),
                        relPath.toString().replaceAll("\\\\", "/"))
                        : new URL(sdrfURL.getProtocol(),
                        sdrfURL.getHost(),
                        sdrfURL.getPort(),
                        relPath.toString().replaceAll("\\\\", "/"));

                // now, obtain a buffer for this dataMatrixFile
                log.debug("Opening buffer of data matrix file at " + dataMatrixURL);

                DataMatrixFileBuffer buffer;
                try {
                    buffer = cache.getDataMatrixFileBuffer(dataMatrixURL, null);
                } catch (AtlasLoaderException e) {
                    String zipUrl = node.comments != null ?
                            node.comments.get("Derived ArrayExpress FTP file") : null;
                    if (zipUrl != null) {
                        buffer = cache.getDataMatrixFileBuffer(new URL(zipUrl), node.getNodeName());
                    } else {
                        throw e;
                    }
                }

                // find the type of nodes we need - lookup from data matrix buffer
                String refNodeName = buffer.getReferenceColumnName();

                // fetch the references from the buffer
                List<String> refNames = buffer.getReferences();

                // for each refName, identify the assay the expression values relate to
                for (int refIndex = 0; refIndex < refNames.size(); ++refIndex) {
                    String refName = refNames.get(refIndex);
                    log.debug("Attempting to attach expression values to next reference " + refName);
                    Assay assay;
                    if (refNodeName.equals("scanname")) {
                        // this requires mapping the assay upstream of this node to the scan
                        // no need to block, since if we are reading data, we've parsed the scans already
                        SDRFNode refNode = investigation.SDRF.lookupNode(refName, refNodeName);
                        if (refNode == null) {
                            // generate error item and throw exception
                            throw new AtlasLoaderException("Could not find " + refName + " [" + refNodeName + "] in SDRF");
                        }

                        // collect all the possible 'assay' forming nodes
                        Collection<HybridizationNode> hybTypeNodes = SDRFUtils.findUpstreamNodes(
                                refNode, HybridizationNode.class);
                        Collection<AssayNode> assayTypeNodes = SDRFUtils.findUpstreamNodes(
                                refNode, AssayNode.class);

                        // lump the two together
                        Collection<SDRFNode> assayNodes = new HashSet<SDRFNode>();
                        assayNodes.addAll(hybTypeNodes);
                        assayNodes.addAll(assayTypeNodes);

                        // now check we have 1:1 mappings so that we can resolve our scans
                        if (assayNodes.size() == 1) {
                            SDRFNode assayNode = assayNodes.iterator().next();
                            log.debug("Scan node " + refNodeName + " resolves to " +
                                    assayNode.getNodeName());

                            assay = cache.fetchAssay(assayNode.getNodeName());
                        } else {
                            // many to one scan-to-assay, we can't load this
                            // generate error item and throw exception
                            throw new AtlasLoaderException(
                                    "Unable to update resolve expression values to assays for " +
                                            investigation.accession + " - data matrix file references scans, " +
                                            "and in this experiment scans do not map one to one with assays.  " +
                                            "This is not supported, as it would result in " +
                                            (assayNodes.size() == 0 ? "zero" : "multiple") + " expression " +
                                            "values per assay."
                            );
                        }
                    } else if (refNodeName.equals("assayname") || refNodeName.equals("hybridizationname")) {
                        // just check it is possible to recover the SDRF node referenced in the data file
                        SDRFNode refNode = investigation.SDRF.lookupNode(refName, refNodeName);
                        if (refNode == null) {
                            // generate error item and throw exception
                            throw new AtlasLoaderException("Could not find " + refName + " [" + refNodeName + "] in SDRF");
                        }

                        // refNode is not null, meaning we recovered this assay - it's safe to wait for it
                        assay = cache.fetchAssay(refNode.getNodeName());
                    } else {
                        assay = null;
                    }

                    if (assay != null) {
                        log.trace("Updating assay " + assay.getAccession() + " with expression values, " +
                                "must be stored first...");
                        cache.setAssayDataMatrixRef(assay, buffer.getStorage(), refIndex);
                        cache.setDesignElements(assay.getArrayDesign().getAccession(), buffer.getDesignElements());
                    } else {
                        // generate error item and throw exception
                        throw new AtlasLoaderException("Data file references elements that are not present in the SDRF (" + refNodeName + ", " + refName + ")");
                    }
                }
            } catch (MalformedURLException e) {
                // generate error item and throw exception
                throw new AtlasLoaderException(
                        "Cannot formulate the URL to retrieve the " +
                                "DerivedArrayDataMatrix from " + node.getNodeName() + ", " +
                                "this file could not be found relative to " + sdrfURL
                );
            }
        }
    }
}
