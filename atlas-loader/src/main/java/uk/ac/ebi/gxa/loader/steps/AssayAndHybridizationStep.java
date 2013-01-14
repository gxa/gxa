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

import com.google.common.base.Strings;
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
import uk.ac.ebi.gxa.dao.arraydesign.ArrayDesignService;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.cache.ExperimentBuilder;
import uk.ac.ebi.gxa.loader.dao.LoaderDAO;
import uk.ac.ebi.gxa.loader.service.PropertyValueMergeService;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.AssayProperty;
import uk.ac.ebi.microarray.atlas.model.Property;

import java.io.IOException;
import java.util.ArrayList;
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

    public void readAssays(MAGETABInvestigation investigation, ExperimentBuilder cache, LoaderDAO dao
                            , ArrayDesignService arrayDesignService, PropertyValueMergeService propertyValueMergeService) throws AtlasLoaderException {
        Collection<ScanNode> scanNodes = investigation.SDRF.getNodes(ScanNode.class);
        for (ScanNode scanNode : scanNodes) {
            if ((scanNode.comments.keySet().contains("ENA_RUN") && scanNode.comments.containsKey("FASTQ_URI"))) {
                writeScanNode(scanNode, cache, investigation, dao, arrayDesignService, propertyValueMergeService);
            }
        }

        if (!isHTS(investigation)) {
            for (HybridizationNode hybridizationNode : investigation.SDRF.getNodes(HybridizationNode.class)) {
                writeHybridizationNode(hybridizationNode, cache, investigation, dao, arrayDesignService, propertyValueMergeService);
            }

            for (AssayNode assayNode : investigation.SDRF.getNodes(AssayNode.class)) {
                writeHybridizationNode(assayNode, cache, investigation, dao, arrayDesignService, propertyValueMergeService);
            }
        }
    }

    private void writeHybridizationNode(
            HybridizationNode node,
            ExperimentBuilder cache,
            MAGETABInvestigation investigation,
            LoaderDAO dao,
            ArrayDesignService arrayDesignService,
            PropertyValueMergeService propertyValueMergeService) throws AtlasLoaderException {
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

        populateArrayDesign(node, assay, arrayDesignService);

        // now record any properties
        writeAssayProperties(investigation, assay, node, dao, propertyValueMergeService);

        // finally, assays must be linked to their upstream samples
        Collection<SourceNode> upstreamSources =
                GraphUtils.findUpstreamNodes(node, SourceNode.class);

        for (SourceNode source : upstreamSources) {
            // retrieve the samples with the matching accession
            cache.linkAssayToSample(assay, source.getNodeName());
        }
    }

    private void writeScanNode(
            ScanNode node,
            ExperimentBuilder cache,
            MAGETABInvestigation investigation,
            LoaderDAO dao,
            ArrayDesignService arrayDesignService,
            PropertyValueMergeService propertyValueMergeService) throws AtlasLoaderException {
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
            populateArrayDesign(assayNode, assay, arrayDesignService);
        }

        // now record any properties
        writeAssayProperties(investigation, assay, assayNode, dao, propertyValueMergeService);

        // finally, assays must be linked to their upstream samples
        Collection<SourceNode> upstreamSources =
                GraphUtils.findUpstreamNodes(assayNode, SourceNode.class);

        for (SourceNode source : upstreamSources) {
            // retrieve the samples with the matching accession
            cache.linkAssayToSample(assay, source.getNodeName());
        }
    }

    private void populateArrayDesign(HybridizationNode assayNode, Assay assay, ArrayDesignService arrayDesignService) throws AtlasLoaderException {
        // add array design accession
        if (assayNode.arrayDesigns.size() > 1) {
            throw new AtlasLoaderException(assayNode.arrayDesigns.size() == 0 ?
                    "Assay does not reference an Array Design - this cannot be loaded to the Atlas" :
                    "Assay references more than one array design, this is disallowed");
        }

        final String arrayDesignAccession = assayNode.arrayDesigns.size() == 1 ?
                assayNode.arrayDesigns.get(0).getNodeName()
                : StringUtils.EMPTY;

        if (assay.getArrayDesign() == null) {
            try {
                ArrayDesign arrayDesign = arrayDesignService.findOrCreateArrayDesignShallow(arrayDesignAccession, false);
                assay.setArrayDesign(arrayDesign);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                throw new AtlasLoaderException(e);
            }
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
     * @param propertyValueMergeService           servce responsible for merging property values
     * @throws uk.ac.ebi.gxa.loader.AtlasLoaderException
     *          if there is a problem creating the property object
     */
    public static void writeAssayProperties(MAGETABInvestigation investigation, Assay assay,
                                            HybridizationNode assayNode, LoaderDAO dao, PropertyValueMergeService propertyValueMergeService) throws AtlasLoaderException {

        List<Pair<String, FactorValueAttribute>> factorValueAttributes = new ArrayList<Pair<String, FactorValueAttribute>>();
        // fetch factor values of this assayNode
        for (FactorValueAttribute factorValueAttribute : assayNode.factorValues) {
            // create Property for this attribute
            if (factorValueAttribute.type.contains("||") || factorValueAttribute.getNodeName().contains("||")) {
                // generate error item and throw exception
                throw new AtlasLoaderException("Factors and their values must NOT contain '||' - " +
                        "this is a special reserved character used as a delimiter in the database");
            }

            factorValueAttributes.add(Pair.create(getFactor(investigation, factorValueAttribute), factorValueAttribute));
        }

        for (Pair<String, String> efEfv : propertyValueMergeService.getMergedFactorValues(factorValueAttributes))
               tryAddPropertyToAssay(efEfv.getKey(), efEfv.getValue(), assay, dao);
    }

    /**
     * @param investigation
     * @param factorValueAttribute
     * @return factor name derived from either factor type field in IDF, or failing that, from factorValueAttribute.type
     * @throws AtlasLoaderException - if no factor could be found usin gthe above methods
     */
    private static String getFactor(MAGETABInvestigation investigation, FactorValueAttribute factorValueAttribute)
            throws AtlasLoaderException {
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

        if (efType == null) {
            // if name->type mapping is null in IDF, warn and fallback to using type from SDRF
            log.warn("Experimental Factor type is null for '" + factorValueAttribute.type +
                    "', using type from SDRF");
            efType = factorValueAttribute.type;
        }

        if (Strings.isNullOrEmpty(efType))
            throw new AtlasLoaderException("Unable to find factor type for factor value: " + factorValueAttribute.getNodeName().trim());

        return efType;
    }

    /**
     * Try adding efType-factorValueName to assay, throwing an exception if a different value for efType already exists in assay.
     * @param efType
     * @param factorValueName
     * @param assay
     * @param dao
     * @throws AtlasLoaderException
     */
    private static void tryAddPropertyToAssay(String efType, String factorValueName, Assay assay, LoaderDAO dao)
            throws AtlasLoaderException {
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
            assay.addProperty(dao.getOrCreatePropertyValue(efType, factorValueName));
            // todo - factor values can have ontology entries, set these values
        }
    }
}
