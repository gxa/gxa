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

package uk.ac.ebi.gxa.loader.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.HybridizationNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SourceNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.CharacteristicsAttribute;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.FactorValueAttribute;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.AssayProperty;
import uk.ac.ebi.microarray.atlas.model.Sample;
import uk.ac.ebi.microarray.atlas.model.SampleProperty;

import java.util.List;

/**
 * A class filled with handy convenience methods for performing writing tasks common to lots of SDRF graph nodes.  This
 * class contains methods that help with writing {@link uk.ac.ebi.microarray.atlas.model.AssayProperty} objects out given some nodes in the SDRF graph.
 *
 * @author Tony Burdett
 */
public class SDRFWritingUtils {
    private static Logger log = LoggerFactory.getLogger(SDRFWritingUtils.class);

    /**
     * Write out the properties associated with a {@link Sample} in the SDRF graph.  These properties are obtained by
     * looking at the "characteristic" column in the SDRF graph, extracting the type and linking this type (the
     * property) to the name of the {@link SourceNode} provided (the property value).
     *
     * @param sample     the sample you want to attach properties to
     * @param sourceNode the sourceNode being read
     * @throws AtlasLoaderException if there is a problem creating the property object
     */
    public static void writeSampleProperties(
            Sample sample,
            SourceNode sourceNode)
            throws AtlasLoaderException {
        // fetch characteristics of this sourceNode
        for (CharacteristicsAttribute characteristicsAttribute : sourceNode.characteristics) {
            // create Property for this attribute
            if (characteristicsAttribute.type.contains("||") || characteristicsAttribute.getNodeName().contains("||")) {
                // generate error item and throw exception
                throw new AtlasLoaderException(
                        "Characteristics and their values must NOT contain '||' - " +
                                "this is a special reserved character used as a delimiter in the database");
            }

            // does this sample already contain this property/property value pair?
            boolean existing = false;
            for (SampleProperty sp : sample.getProperties()) {
                if (sp.getName().equals(characteristicsAttribute.type)) {
                existing = true;
                if (!sp.getValue().equals(characteristicsAttribute.getNodeName())) {
                    // generate error item and throw exception
                    throw new AtlasLoaderException(
                            "Inconsistent characteristic values for sample " + sample.getAccession() +
                                    ": property " + sp.getName() + " has values " + sp.getValue() + " and " +
                                    characteristicsAttribute.getNodeName() + " in different rows. Second value (" +
                                    characteristicsAttribute + ") will be ignored"
                    );
                }}
            }

            if (!existing) {
                sample.addProperty(characteristicsAttribute.type, characteristicsAttribute.getNodeName(), "");
                // todo - characteristics can have ontology entries, and units (which can also have ontology entries) - set these values
            }
        }
    }

    /**
     * Write out the properties associated with an {@link Assay} in the SDRF graph.  These properties are obtained by
     * looking at the "factorvalue" column in the SDRF graph, extracting the type and linking this type (the property)
     * to the name of the {@link uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.HybridizationNode} provided (the property
     * value).
     *
     * @param investigation the investigation being loaded
     * @param assay         the assay you want to attach properties to
     * @param assayNode     the assayNode being read
     * @throws AtlasLoaderException if there is a problem creating the property object
     */
    public static void writeAssayProperties(
            MAGETABInvestigation investigation,
            Assay assay,
            HybridizationNode assayNode)
            throws AtlasLoaderException {
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

            // does this assay already contain this property/property value pair?
            boolean existing = false;
            for (AssayProperty ap : assay.getProperties(factorValueAttribute.type)) {
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

            // try and lookup type
            String efType = null;
            List<String> efNames = investigation.IDF.experimentalFactorName;
            for (int i = 0; i < efNames.size(); i++) {
                if (efNames.get(i).equals(factorValueAttribute.type)) {
                    if (investigation.IDF.experimentalFactorType.size() > i) {
                        efType = investigation.IDF.experimentalFactorType.get(i);
                    }
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
                assay.addProperty(type, factorValueName, "");

                // todo - factor values can have ontology entries, set these values
            }
        }
    }
}
