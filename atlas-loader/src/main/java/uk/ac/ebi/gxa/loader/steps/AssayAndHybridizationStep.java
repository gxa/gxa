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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AssayNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.HybridizationNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.ScanNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SourceNode;
import uk.ac.ebi.arrayexpress2.magetab.utils.SDRFUtils;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.gxa.loader.utils.SDRFWritingUtils;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Sample;

import java.util.Collection;

/**
 * Experiment loading step that stores assay and hybridization nodes information
 * from SDRF structures into Atlas internal experiment model.
 * Based on the original handlers code by Tony Burdett.
 *
 * @author Nikolay Pultsin
 */
public class AssayAndHybridizationStep implements Step {
    private final MAGETABInvestigation investigation;
    private final AtlasLoadCache cache;
    private final Log log = LogFactory.getLog(this.getClass());

    public AssayAndHybridizationStep(MAGETABInvestigation investigation) {
        this.investigation = investigation;
        this.cache = AtlasLoadCacheRegistry.getRegistry().retrieveAtlasLoadCache(investigation);
    }

    public String displayName() {
        return "Processing assay and hybridization nodes";
    }

    public void run() throws AtlasLoaderException {
        boolean isRNASeq = false;

        Collection<ScanNode> scanNodes = investigation.SDRF.lookupNodes(ScanNode.class);
        for (ScanNode scanNode : scanNodes) {
            if ((scanNode.comments.keySet().contains("ENA_RUN") && scanNode.comments.containsKey("FASTQ_URI"))) {
//                log.info("Comment[ENA_RUN] found.");
                writeScanNode(scanNode);
                isRNASeq = true;
            }
        }

        if (!isRNASeq) {
            for (HybridizationNode hybridizationNode : investigation.SDRF.lookupNodes(HybridizationNode.class)) {
                writeHybridizationNode(hybridizationNode);
            }

            for (AssayNode assayNode : investigation.SDRF.lookupNodes(AssayNode.class)) {
                writeHybridizationNode(assayNode);
            }
        }
    }

    private void writeHybridizationNode(HybridizationNode node) throws AtlasLoaderException {
        log.debug("Writing assay from hybridization node '" + node.getNodeName() + "'");

        // create/retrieve the new assay
        Assay assay = cache.fetchAssay(node.getNodeName());
        if (assay != null) {
            // get the existing sample
            log.debug("Integrated assay with existing assay (" + assay.getAccession() + "), " +
                      "count now = " + cache.fetchAllAssays().size());
        } else {
            // create a new sample and add it to the cache
            assay = new Assay(node.getNodeName());
            assay.setExperimentAccession(investigation.accession);
            cache.addAssay(assay);
            log.debug("Created new assay (" + assay.getAccession() + "), " +
                      "count now = " + cache.fetchAllAssays().size());
        }

        // add array design accession
        if (node.arrayDesigns.size() > 1) {
            throw new AtlasLoaderException(node.arrayDesigns.size() == 0 ?
                "Assay does not reference an Array Design - this cannot be loaded to the Atlas" :
                "Assay references more than one array design, this is disallowed");
        }

        final String arrayDesignAccession = node.arrayDesigns.size() == 1?
                node.arrayDesigns.get(0).getNodeName()
                :StringUtils.EMPTY;

        // only one, so set the accession
        if (assay.getArrayDesignAccession() == null) {
            assay.setArrayDesignAccession(arrayDesignAccession);
        } else if (!assay.getArrayDesignAccession().equals(arrayDesignAccession)) {
            throw new AtlasLoaderException("The same assay in the SDRF references two different array designs");
        } else {
            // already set, and equal, so ignore
        }

        // now record any properties
        SDRFWritingUtils.writeAssayProperties(investigation, assay, node);

        // finally, assays must be linked to their upstream samples
        Collection<SourceNode> upstreamSources =
            SDRFUtils.findUpstreamNodes(node, SourceNode.class);

        for (SourceNode source : upstreamSources) {
            // retrieve the samples with the matching accession
            Sample sample = cache.fetchSample(source.getNodeName());

            if (sample != null) {
                if (!sample.getAssayAccessions().contains(assay.getAccession())) {
                    log.trace("Updating " + sample.getAccession() + " with assay accession");
                    sample.addAssayAccession(assay.getAccession());
                }
            } else {
                // no sample to link to in the cache - generate error item and throw exception
                throw new AtlasLoaderException("Assay " + assay.getAccession() + " is linked to sample " +
                                           source.getNodeName() + " but this sample is not due to be loaded. " +
                                           "This assay will not be linked to a sample");
            }
        }
    }

    private void writeScanNode(ScanNode node) throws AtlasLoaderException {

        String enaRunName = node.comments.get("ENA_RUN");

        log.debug("Writing assay from scan node '" + node.getNodeName() + "'" + " ENA_RUN name: " + enaRunName);

        // create/retrieve the new assay
        Assay assay = cache.fetchAssay(enaRunName);
        if (assay != null) {
            // get the existing sample
            log.debug("Integrated assay with existing assay (" + assay.getAccession() + "), " +
                    "count now = " + cache.fetchAllAssays().size());
        } else {
            // create a new sample and add it to the cache
            assay = new Assay(enaRunName);
            assay.setExperimentAccession(investigation.accession);
            cache.addAssay(assay);
            log.debug("Created new assay (" + assay.getAccession() + "), " +
                    "count now = " + cache.fetchAllAssays().size());
        }

        // add array design accession
        Collection<AssayNode> assayNodes = SDRFUtils.findUpstreamNodes(node, AssayNode.class);

        AssayNode assayNode;
        // now check we have 1:1 mappings so that we can resolve our scans
        if (assayNodes.size() == 1) {
            assayNode = assayNodes.iterator().next();
            log.debug("Scan node " + node.getNodeName() + "/" + enaRunName + " resolves to " + assayNode.getNodeName());

        } else {
            // many to one scan-to-assay, we can't load this generate error item and throw exception
            throw new AtlasLoaderException(
                    "Unable to update resolve expression values to assays for " +
                            investigation.accession + " - data matrix file references scans, " +
                            "and in this experiment scans do not map one to one with assays.  " +
                            "This is not supported, as it would result in " +
                            (assayNodes.size() == 0 ? "zero" : "multiple") + " expression " +
                            "values per assay."
            );
        }

        // add array design accession
        if (assayNode.arrayDesigns.size() > 1) {
            throw new AtlasLoaderException(assayNode.arrayDesigns.size() == 0 ?
                    "Assay does not reference an Array Design - this cannot be loaded to the Atlas" :
                    "Assay references more than one array design, this is disallowed");
        }

        //Case of HTS, no array design available, create one.
        //ToDo: add more checks if the experiment is really HTS
        //ToDo: get organism from Characteristics[Organism]
        final String arrayDesignAccession = assayNode.arrayDesigns.size() == 1 ?
                assayNode.arrayDesigns.get(0).getNodeName()
                : StringUtils.EMPTY;

        // only one, so set the accession
        if (assay.getArrayDesignAccession() == null) {
            assay.setArrayDesignAccession(arrayDesignAccession);
        } else if (!assay.getArrayDesignAccession().equals(arrayDesignAccession)) {
            throw new AtlasLoaderException("The same assay in the SDRF references two different array designs");
        } else {
            // already set, and equal, so ignore
        }

        // now record any properties
        SDRFWritingUtils.writeAssayProperties(investigation, assay, assayNode);

        // finally, assays must be linked to their upstream samples
        Collection<SourceNode> upstreamSources =
                SDRFUtils.findUpstreamNodes(assayNode, SourceNode.class);

        for (SourceNode source : upstreamSources) {
            // retrieve the samples with the matching accession
            Sample sample = cache.fetchSample(source.getNodeName());

            if (sample != null) {
                if (!sample.getAssayAccessions().contains(assay.getAccession())) {
                    log.trace("Updating " + sample.getAccession() + " with assay accession");
                    sample.addAssayAccession(assay.getAccession());
                }
            } else {
                // no sample to link to in the cache - generate error item and throw exception
                throw new AtlasLoaderException("Assay " + assay.getAccession() + " is linked to sample " +
                        source.getNodeName() + " but this sample is not due to be loaded. " +
                        "This assay will not be linked to a sample");
            }
        }
    }
}
