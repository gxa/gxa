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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.graph.utils.GraphUtils;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AssayNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.HybridizationNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.ScanNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SourceNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.FactorValueAttribute;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.cache.ExperimentBuilder;
import uk.ac.ebi.gxa.loader.dao.LoaderDAO;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.AssayProperty;
import uk.ac.ebi.microarray.atlas.model.Property;

import java.util.Collection;
import java.util.List;

import static uk.ac.ebi.gxa.loader.service.AtlasMAGETABLoader.isHTS;

/**
 * Experiment loading step that stores assay and hybridization nodes information
 * from SDRF structures into Atlas internal experiment model.
 * Based on the original handlers code by Tony Burdett.
 *
 * @author Nikolay Pultsin
 */
public class AssayAndHybridizationStep {
    private final static Logger log = LoggerFactory.getLogger(AssayAndHybridizationStep.class);

    public static String displayName() {
        return "Processing assay and hybridization nodes";
    }

    public void readAssays(MAGETABInvestigation investigation, ExperimentBuilder cache, LoaderDAO dao) throws AtlasLoaderException {
        Collection<ScanNode> scanNodes = investigation.SDRF.getNodes(ScanNode.class);
        for (ScanNode scanNode : scanNodes) {
            if ((scanNode.comments.keySet().contains("ENA_RUN") && scanNode.comments.containsKey("FASTQ_URI"))) {
                writeScanNode(scanNode, cache, investigation, dao);
            }
        }

        if (!isHTS(investigation)) {
            for (HybridizationNode hybridizationNode : investigation.SDRF.getNodes(HybridizationNode.class)) {
                writeHybridizationNode(hybridizationNode, cache, investigation, dao);
            }

            for (AssayNode assayNode : investigation.SDRF.getNodes(AssayNode.class)) {
                writeHybridizationNode(assayNode, cache, investigation, dao);
            }
        }
    }

    private void writeHybridizationNode(HybridizationNode node, ExperimentBuilder cache, MAGETABInvestigation investigation, LoaderDAO dao) throws AtlasLoaderException {
        assert !isHTS(investigation);

        log.debug("Writing assay from hybridization node '" + node.getNodeName() + "'");

        // create/retrieve the new assay
        Assay assay = cache.fetchAssay(node.getNodeName());
        if (assay != null) {
            // get the existing sample
            log.debug("Integrated assay with existing assay (" + assay.getAccession() + "), " +
                    "count now = " + cache.fetchAllAssays().size());
        } else {
            // create a new assay and add it to the cache
            assay = new Assay(node.getNodeName());
            cache.addAssay(assay);
            log.debug("Created new assay (" + assay.getAccession() + "), " +
                    "count now = " + cache.fetchAllAssays().size());
        }

        populateArrayDesign(node, assay, dao);

        // now record any properties
        writeAssayProperties(investigation, assay, node, dao);

        // finally, assays must be linked to their upstream samples
        Collection<SourceNode> upstreamSources =
                GraphUtils.findUpstreamNodes(node, SourceNode.class);

        for (SourceNode source : upstreamSources) {
            // retrieve the samples with the matching accession
            cache.linkAssayToSample(assay, source.getNodeName());
        }
    }

    private void writeScanNode(ScanNode node, ExperimentBuilder cache, MAGETABInvestigation investigation, LoaderDAO dao) throws AtlasLoaderException {
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
            cache.addAssay(assay);
            log.debug("Created new assay (" + assay.getAccession() + "), " +
                    "count now = " + cache.fetchAllAssays().size());
        }

        // add array design accession
        Collection<AssayNode> assayNodes = GraphUtils.findUpstreamNodes(node, AssayNode.class);

        AssayNode assayNode;
        // now check we have 1:1 mappings so that we can resolve our scans
        if (assayNodes.size() == 1) {
            assayNode = assayNodes.iterator().next();
            log.debug("Scan node " + node.getNodeName() + "/" + enaRunName + " resolves to " + assayNode.getNodeName());

        } else {
            // many to one scan-to-assay, we can't load this generate error item and throw exception
            throw new AtlasLoaderException(
                    "Unable to update resolve expression values to assays for " +
                            investigation.getAccession() + " - data matrix file references scans, " +
                            "and in this experiment scans do not map one to one with assays.  " +
                            "This is not supported, as it would result in " +
                            (assayNodes.size() == 0 ? "zero" : "multiple") + " expression " +
                            "values per assay."
            );
        }

        if (!isHTS(investigation)) {
            populateArrayDesign(assayNode, assay, dao);
        }

        // now record any properties
        writeAssayProperties(investigation, assay, assayNode, dao);

        // finally, assays must be linked to their upstream samples
        Collection<SourceNode> upstreamSources =
                GraphUtils.findUpstreamNodes(assayNode, SourceNode.class);

        for (SourceNode source : upstreamSources) {
            // retrieve the samples with the matching accession
            cache.linkAssayToSample(assay, source.getNodeName());
        }
    }

    private void populateArrayDesign(HybridizationNode assayNode, Assay assay, LoaderDAO dao) throws AtlasLoaderException {
        // add array design accession
        if (assayNode.arrayDesigns.size() > 1) {
            throw new AtlasLoaderException(assayNode.arrayDesigns.size() == 0 ?
                    "Assay does not reference an Array Design - this cannot be loaded to the Atlas" :
                    "Assay references more than one array design, this is disallowed");
        }

        final String arrayDesignAccession = assayNode.arrayDesigns.size() == 1 ?
                assayNode.arrayDesigns.get(0).getNodeName()
                : StringUtils.EMPTY;

        // only one, so set the accession
        if (assay.getArrayDesign() == null) {
            final ArrayDesign ad = dao.getArrayDesignShallow(arrayDesignAccession);
            if (ad == null) {
                throw new AtlasLoaderException("There is no array design with accession " + arrayDesignAccession + " in Atlas database");
            }
            assay.setArrayDesign(ad);
        } else if (!assay.getArrayDesign().getAccession().equals(arrayDesignAccession)) {
            throw new AtlasLoaderException("The same assay in the SDRF references two different array designs");
        } else {
            // already set, and equal, so ignore
        }
    }

    /**
     * Write out the properties associated with an {@link uk.ac.ebi.microarray.atlas.model.Assay} in the SDRF graph.  These properties are obtained by
     * looking at the "factorvalue" column in the SDRF graph, extracting the type and linking this type (the property)
     * to the name of the {@link uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.HybridizationNode} provided (the property
     * value).
     *
     * @param investigation the investigation being loaded
     * @param assay         the assay you want to attach properties to
     * @param assayNode     the assayNode being read
     * @param dao           the LoaderDAO to consult for the objects necessary
     * @throws uk.ac.ebi.gxa.loader.AtlasLoaderException
     *          if there is a problem creating the property object
     */
    public static void writeAssayProperties(MAGETABInvestigation investigation, Assay assay,
                                            HybridizationNode assayNode, LoaderDAO dao) throws AtlasLoaderException {
        // fetch factor values of this assayNode
        for (FactorValueAttribute factorValueAttribute : assayNode.factorValues) {
            // create Property for this attribute
            if (factorValueAttribute.type.contains("||") || factorValueAttribute.getNodeName().contains("||")) {
                // generate error item and throw exception
                throw new AtlasLoaderException("Factors and their values must NOT contain '||' - " +
                        "this is a special reserved character used as a delimiter in the database");
            }
            String factorValueName = factorValueAttribute.getNodeName();
            if (factorValueName.length() == 0) {
                factorValueName = "(empty)";
            }

            // try and lookup factor type for factor name: factorValueAttribute.type
            String efType = null;
            List<String> efNames = investigation.IDF.experimentalFactorName;
            for (int i = 0; i < efNames.size(); i++) {
                if (efNames.get(i).equals(factorValueAttribute.type)) {
                    if (investigation.IDF.experimentalFactorType.size() > i) {
                        efType = investigation.IDF.experimentalFactorType.get(i);
                    }
                }
            }

            // If assay already contains values for efType then:
            // If factorValueName is one of the existing values, don't re-add it; otherwise, throw an Exception
            // as one factor type cannot have more then one value in a single assay (Atlas cannot currently cope
            // with such experiments)
            boolean existing = false;
            for (AssayProperty ap : assay.getProperties(Property.getSanitizedPropertyAccession(efType))) {
                existing = true;
                if (!ap.getValue().equals(factorValueName)) {
                    throw new AtlasLoaderException(
                            "Assay " + assay.getAccession() + " has multiple factor values for " +
                                    ap.getName() + "(" + ap.getValue() + " and " + factorValueName +
                                    ") on different rows.  This may be because this is a 2 channel experiment, " +
                                    "which cannot currently be loaded into the atlas. Or, this could be a result " +
                                    "of inconsistent annotations"
                    );
                }
            }

            if (!existing) {
                final String type;
                if (efType == null) {
                    // if name->type mapping is null in IDF, warn and fallback to using type from SDRF
                    log.warn("Experimental Factor type is null for '" + factorValueAttribute.type +
                            "', using type from SDRF");
                    type = factorValueAttribute.type;
                } else {
                    type = efType;
                }

                assay.addProperty(dao.getOrCreatePropertyValue(type, factorValueName));

                // todo - factor values can have ontology entries, set these values
            }
        }
    }
}
