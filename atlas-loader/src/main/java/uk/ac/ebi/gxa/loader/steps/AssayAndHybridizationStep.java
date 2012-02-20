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

import com.google.common.base.Joiner;
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
import uk.ac.ebi.gxa.efo.Efo;
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

    // These constants are used in merging compounds and doses together
    private static final String COMPOUND = "compound";
    private static final String DOSE = "dose";

    // Units that should never be pluralised when being joined to factor values
    private static final String OTHER = "other";
    private static final String PERCENT = "percent";
    // separator in units in which only the first work should be pluralised (e.g. "micromole per kilogram")
    private static final String PER = "per";
    // The only case other than the above in which only the first word should be pluralised (e.g. "degree celcius")
    private static final String DEGREE = "degree";

    public static String displayName() {
        return "Processing assay and hybridization nodes";
    }

    public void readAssays(MAGETABInvestigation investigation, ExperimentBuilder cache, LoaderDAO dao, Efo efo) throws AtlasLoaderException {
        Collection<ScanNode> scanNodes = investigation.SDRF.getNodes(ScanNode.class);
        for (ScanNode scanNode : scanNodes) {
            if ((scanNode.comments.keySet().contains("ENA_RUN") && scanNode.comments.containsKey("FASTQ_URI"))) {
                writeScanNode(scanNode, cache, investigation, dao, efo);
            }
        }

        if (!isHTS(investigation)) {
            for (HybridizationNode hybridizationNode : investigation.SDRF.getNodes(HybridizationNode.class)) {
                writeHybridizationNode(hybridizationNode, cache, investigation, dao, efo);
            }

            for (AssayNode assayNode : investigation.SDRF.getNodes(AssayNode.class)) {
                writeHybridizationNode(assayNode, cache, investigation, dao, efo);
            }
        }
    }

    private void writeHybridizationNode(
            HybridizationNode node,
            ExperimentBuilder cache,
            MAGETABInvestigation investigation,
            LoaderDAO dao,
            Efo efo) throws AtlasLoaderException {
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
        writeAssayProperties(investigation, assay, node, dao, efo);

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
            Efo efo) throws AtlasLoaderException {
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
        writeAssayProperties(investigation, assay, assayNode, dao, efo);

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
     * Pluralise a unit only if:
     * - unit is not empty
     * - factor value it describes is not equal to 1
     * - it is not equal to OTHER or contains PERCENT in it
     * - it does not already end in "s"
     * <p/>
     * Pluralisation method is as follows:
     * - if a unit contains PER, pluralise the term preceding it (unless that term ends in "s" already)
     * - else if a unit starts with DEGREE, pluralise word DEGREE unless the unit starts with DEGREE + "s"
     * - else unless the units already ends in "s", pluralise thh whole unit.
     * <p/>
     * See the junit test case of this method for the full list of test cases.
     * <p/> c.f. examples of units in EFO in ticket 3356:
     * MAGE-OM_to_EFO_Units.txt
     * OtherUnitsMappedToEFO.txt
     *
     * @param unit
     * @param factorValue
     * @return
     */
    public static String pluraliseUnitIfNeeded(String unit, String factorValue) {
        try {
            if (Strings.isNullOrEmpty(factorValue) || Integer.parseInt(factorValue) == 1)
                return unit;
        } catch (NumberFormatException nfe) {
            // quiesce
        }

        if (!Strings.isNullOrEmpty(unit) && !unit.equals(OTHER) && !unit.contains(PERCENT)) {
            int idx = unit.indexOf(PER);
            if (idx != -1) {
                String firstWord = unit.substring(0, idx - 1).trim();
                if (!firstWord.endsWith("s"))
                    return firstWord + "s " + unit.substring(idx);
            } else if (unit.startsWith(DEGREE) && !unit.equals(DEGREE + "s")) {
                return DEGREE + "s" + unit.substring(DEGREE.length());
            } else if (!unit.endsWith("s"))
                return unit + "s";
        }

        return unit;
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
     * @param efo           all loaded units need to exist in EFO - this param is used to check that
     * @throws uk.ac.ebi.gxa.loader.AtlasLoaderException
     *          if there is a problem creating the property object
     */
    public static void writeAssayProperties(MAGETABInvestigation investigation, Assay assay,
                                            HybridizationNode assayNode, LoaderDAO dao, Efo efo) throws AtlasLoaderException {
        String compoundFactorValue = null;
        String doseFactorValue = null;
        // fetch factor values of this assayNode
        for (FactorValueAttribute factorValueAttribute : assayNode.factorValues) {
            // create Property for this attribute
            if (factorValueAttribute.type.contains("||") || factorValueAttribute.getNodeName().contains("||")) {
                // generate error item and throw exception
                throw new AtlasLoaderException("Factors and their values must NOT contain '||' - " +
                        "this is a special reserved character used as a delimiter in the database");
            }
            String factorValueName = factorValueAttribute.getNodeName().trim();
            if (Strings.isNullOrEmpty(factorValueName)) {
                continue; // We don't load empty factor values
            } else if (factorValueAttribute.unit != null) {
                String unitValue = factorValueAttribute.unit.getAttributeValue();
                if (Strings.isNullOrEmpty(unitValue))
                    throw new AtlasLoaderException("Unable to find unit value for factor value: " + factorValueName);
                unitValue = pluraliseUnitIfNeeded(unitValue.trim(), factorValueName);
                if (efo.searchTerm(unitValue).isEmpty()) {
                    throw new AtlasLoaderException("Unit: " + unitValue + " not found in EFO");
                }
                factorValueName = Joiner.on(" ").join(factorValueName, unitValue);
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

            if (efType == null) {
                // if name->type mapping is null in IDF, warn and fallback to using type from SDRF
                log.warn("Experimental Factor type is null for '" + factorValueAttribute.type +
                        "', using type from SDRF");
                efType = factorValueAttribute.type;
            }

            if (Strings.isNullOrEmpty(efType))
                throw new AtlasLoaderException("Unable to find factor type for factor value: " + factorValueName);

            if (COMPOUND.equalsIgnoreCase(efType))
                compoundFactorValue = factorValueName;
            else if (DOSE.equalsIgnoreCase(efType))
                doseFactorValue = factorValueName;

            if (COMPOUND.equalsIgnoreCase(efType) || DOSE.equalsIgnoreCase(efType)) {
                if (!Strings.isNullOrEmpty(compoundFactorValue) && !Strings.isNullOrEmpty(doseFactorValue)) {
                    efType = COMPOUND;
                    factorValueName = Joiner.on(" ").join(compoundFactorValue, doseFactorValue);
                    compoundFactorValue = null;
                    doseFactorValue = null;
                } else {
                    // Don't add either compound or dose factor values to assay on their own, until:
                    // - you have both of them, in which case merge them together and then add to assay
                    // - you know that either dose or compound values are missing, in which case add to assay that one
                    //   (dose or compound) that is present
                    continue;
                }
            }
            tryAddPropertyToAssay(efType, factorValueName, assay, dao);
        }

        if (!Strings.isNullOrEmpty(compoundFactorValue) && Strings.isNullOrEmpty(doseFactorValue)) {
            tryAddPropertyToAssay(COMPOUND, compoundFactorValue, assay, dao);
            log.warn("Adding " + COMPOUND + " : " + compoundFactorValue + " to assay with no corresponding value for factor: " + DOSE);
        } else if (!Strings.isNullOrEmpty(doseFactorValue) && Strings.isNullOrEmpty(compoundFactorValue)) {
            tryAddPropertyToAssay(DOSE, doseFactorValue, assay, dao);
            log.warn("Adding " + DOSE + " : " + doseFactorValue + " to assay with no corresponding value for factor: " + COMPOUND);
        }
    }

    private static void tryAddPropertyToAssay(String ef, String efv, Assay assay, LoaderDAO dao)
            throws AtlasLoaderException {
        // If assay already contains values for efType then:
        // If factorValueName is one of the existing values, don't re-add it; otherwise, throw an Exception
        // as one factor type cannot have more then one value in a single assay (Atlas cannot currently cope
        // with such experiments)
        boolean existing = false;
        for (AssayProperty ap : assay.getProperties(Property.getSanitizedPropertyAccession(ef))) {
            existing = true;
            if (!ap.getValue().equals(efv)) {
                throw new AtlasLoaderException(
                        "Assay " + assay.getAccession() + " has multiple factor values for " +
                                ap.getName() + "(" + ap.getValue() + " and " + efv +
                                ") on different rows.  This may be because this is a 2 channel experiment, " +
                                "which cannot currently be loaded into the atlas. Or, this could be a result " +
                                "of inconsistent annotations"
                );
            }
        }

        if (!existing) {
            assay.addProperty(dao.getOrCreatePropertyValue(ef, efv));
            // todo - factor values can have ontology entries, set these values
        }
    }
}
