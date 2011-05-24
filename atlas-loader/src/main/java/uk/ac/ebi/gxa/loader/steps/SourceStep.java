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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SourceNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.CharacteristicsAttribute;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.dao.LoaderDAO;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;
import uk.ac.ebi.microarray.atlas.model.Sample;
import uk.ac.ebi.microarray.atlas.model.SampleProperty;

/**
 * Experiment loading step that stores source nodes information from
 * SDRF structures into Atlas internal experiment model.
 * Based on the original handlers code by Tony Burdett.
 *
 * @author Nikolay Pultsin
 */
public class SourceStep {
    private final static Logger log = LoggerFactory.getLogger(SourceStep.class);

    public static String displayName() {
        return "Processing source nodes";
    }

    public void readSamples(MAGETABInvestigation investigation, AtlasLoadCache cache, LoaderDAO dao) throws AtlasLoaderException {
        for (SourceNode node : investigation.SDRF.lookupNodes(SourceNode.class)) {
            log.debug("Writing sample from source node '" + node.getNodeName() + "'");
            Sample sample = cache.fetchOrCreateSample(node.getNodeName());
            // write the characteristic values as properties
            readSampleProperties(sample, node, dao);
        }
    }

    /**
     * Write out the properties associated with a {@link uk.ac.ebi.microarray.atlas.model.Sample} in the SDRF graph.  These properties are obtained by
     * looking at the "characteristic" column in the SDRF graph, extracting the type and linking this type (the
     * property) to the name of the {@link uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SourceNode} provided (the property value).
     *
     *
     * @param sample     the sample you want to attach properties to
     * @param sourceNode the sourceNode being read
     * @param dao
     * @throws uk.ac.ebi.gxa.loader.AtlasLoaderException
     *          if there is a problem creating the property object
     */
    public void readSampleProperties(Sample sample, SourceNode sourceNode, LoaderDAO dao) throws AtlasLoaderException {
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
                    }
                }
            }

            if (!existing) {
                final PropertyValue property = dao.getOrCreateProperty(characteristicsAttribute.type, characteristicsAttribute.getNodeName());
                sample.addProperty(property);

                if ("organism".equals(property.getDefinition().getName().toLowerCase())) {
                    sample.setOrganism(dao.getOrCreateOrganism(property.getValue()));
                }

                // TODO: 4alf: todo - characteristics can have ontology entries, and units (which can also have ontology entries) - set these values
            }
        }
    }
}
