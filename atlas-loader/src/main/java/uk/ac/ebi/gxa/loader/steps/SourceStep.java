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
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SourceNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.CharacteristicsAttribute;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.cache.ExperimentBuilder;
import uk.ac.ebi.gxa.loader.dao.LoaderDAO;
import uk.ac.ebi.gxa.loader.service.PropertyValueMergeService;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;
import uk.ac.ebi.microarray.atlas.model.Sample;
import uk.ac.ebi.microarray.atlas.model.SampleProperty;

import java.util.List;

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

    public void readSamples(MAGETABInvestigation investigation, ExperimentBuilder cache, LoaderDAO dao, PropertyValueMergeService propertyValueMergeService) throws AtlasLoaderException {
        for (SourceNode node : investigation.SDRF.getNodes(SourceNode.class)) {
            log.debug("Writing sample from source node '" + node.getNodeName() + "'");
            Sample sample = cache.fetchOrCreateSample(node.getNodeName());
            // write the characteristic values as properties
            readSampleProperties(sample, node, dao, propertyValueMergeService);
        }
    }

    /**
     * Write out the properties associated with a {@link uk.ac.ebi.microarray.atlas.model.Sample} in the SDRF graph.  These properties are obtained by
     * looking at the "characteristic" column in the SDRF graph, extracting the type and linking this type (the
     * property) to the name of the {@link uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SourceNode} provided (the property value).
     *
     * @param sample                    the sample you want to attach properties to
     * @param sourceNode                the sourceNode being read
     * @param dao
     * @param propertyValueMergeService
     * @throws uk.ac.ebi.gxa.loader.AtlasLoaderException
     *          if there is a problem creating the property object
     */
    public void readSampleProperties(Sample sample, SourceNode sourceNode, LoaderDAO dao, PropertyValueMergeService propertyValueMergeService) throws AtlasLoaderException {
        // fetch characteristics of this sourceNode
        List<Pair<String, CharacteristicsAttribute>> characteristicTypesValues = Lists.newArrayList();
        for (CharacteristicsAttribute characteristicsAttribute : sourceNode.characteristics) {
            // create Property for this attribute
            String characteristicValue = characteristicsAttribute.getNodeName().trim();
            if (Strings.isNullOrEmpty(characteristicValue)) {
                continue; // We don't load empty sample characteristic values
            } else if (characteristicsAttribute.type.contains("||") || characteristicValue.contains("||")) {
                // generate error item and throw exception
                throw new AtlasLoaderException(
                        "Characteristics and their values must NOT contain '||' - " +
                                "this is a special reserved character used as a delimiter in the database");
            }
            characteristicTypesValues.add(Pair.create(characteristicsAttribute.type, characteristicsAttribute));
        }

        for (Pair<String, CharacteristicsAttribute> scScv : characteristicTypesValues)
            addPropertyToSample(
                    scScv.getKey(),
                    propertyValueMergeService.getCharacteristicValueWithUnit(scScv.getValue()), sample, dao);
    }

    /**
     * Add characteristicsAttribute.type-characteristicValue to sample. If sample already contains values for
     * characteristicsAttribute.type then. If characteristicValue is one of the existing values, don't re-add it
     *
     * @param characteristicsType
     * @param characteristicValue
     * @param sample
     * @param dao
     */
    private static void addPropertyToSample(String characteristicsType, String characteristicValue, Sample sample, LoaderDAO dao) {
        boolean existing = false;
        for (SampleProperty sp : sample.getProperties(Property.getSanitizedPropertyAccession(characteristicsType))) {
            if (sp.getValue().equals(characteristicValue))
                existing = true;
        }

        if (!existing) {
            final PropertyValue property = dao.getOrCreatePropertyValue(characteristicsType, characteristicValue);
            sample.addProperty(property);

            if ("organism".equals(property.getDefinition().getName().toLowerCase())) {
                sample.setOrganism(dao.getOrCreateOrganism(property.getValue()));
            }
            // TODO: 4alf: todo - characteristics can have ontology entries, and units (which can also have ontology entries) - set these values
        }
    }
}
