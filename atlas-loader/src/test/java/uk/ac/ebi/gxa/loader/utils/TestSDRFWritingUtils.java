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

import junit.framework.TestCase;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AssayNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.HybridizationNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SourceNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.CharacteristicsAttribute;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.FactorValueAttribute;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.MockFactory;
import uk.ac.ebi.gxa.loader.dao.LoaderDAO;
import uk.ac.ebi.gxa.loader.steps.AssayAndHybridizationStep;
import uk.ac.ebi.gxa.loader.steps.SourceStep;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.AssayProperty;
import uk.ac.ebi.microarray.atlas.model.Sample;
import uk.ac.ebi.microarray.atlas.model.SampleProperty;

public class TestSDRFWritingUtils extends TestCase {
    public static final String TYPE = "Type";
    public void testWriteAssayProperties() throws AtlasLoaderException {
        // create investigation
        MAGETABInvestigation investigation = new MAGETABInvestigation();
        investigation.IDF.experimentalFactorType.add(TYPE);
        investigation.IDF.experimentalFactorName.add(TYPE);

        Assay assay = new Assay("TEST-ASSAY");

        AssayNode assayNode = new AssayNode();
        FactorValueAttribute fva = new FactorValueAttribute();
        fva.type = TYPE;
        fva.setAttributeValue("specific factor value");
        assayNode.factorValues.add(fva);

        final LoaderDAO dao = MockFactory.createLoaderDAO();

        AssayAndHybridizationStep.writeAssayProperties(investigation, assay, assayNode, dao, MockFactory.createPropertyValueMergeService());

        // now get properties of assay - we should have one matching our factor value
        assertSame("Wrong number of properties", assay.getProperties().size(), 1);
        for (AssayProperty p : assay.getProperties()) {
            assertEquals("Wrong property name", p.getName(), TYPE);
            assertEquals("Wrong property value", p.getValue(),
                    "specific factor value");
        }
    }

    public void testWriteSampleProperties() throws AtlasLoaderException {
        // create investigation
        MAGETABInvestigation investigation = new MAGETABInvestigation();

        Sample sample = new Sample("TEST-SAMPLE");

        SourceNode sourceNode = new SourceNode();
        CharacteristicsAttribute fva = new CharacteristicsAttribute();
        fva.type = TYPE;
        fva.setAttributeValue("specific factor value");
        sourceNode.characteristics.add(fva);

        new SourceStep().readSampleProperties(sample, sourceNode, MockFactory.createLoaderDAO());

        // now get properties of assay - we should have one matching our factor value
        assertSame("Wrong number of properties", sample.getProperties().size(),
                1);
        for (SampleProperty p : sample.getProperties()) {
            assertEquals("Wrong property name", p.getName(), TYPE);
            assertEquals("Wrong property value", p.getValue(),
                    "specific factor value");
        }

    }

    public void testWriteHybridizationProperties() throws AtlasLoaderException {
        // create investigation
        MAGETABInvestigation investigation = new MAGETABInvestigation();

        Assay assay = new Assay("TEST-SAMPLE");

        HybridizationNode hybridizationNode = new HybridizationNode();
        FactorValueAttribute fva = new FactorValueAttribute();
        fva.type = TYPE;
        fva.setAttributeValue("specific factor value");
        hybridizationNode.factorValues.add(fva);

        AssayAndHybridizationStep.writeAssayProperties(investigation, assay, hybridizationNode, MockFactory.createLoaderDAO(), MockFactory.createPropertyValueMergeService());

        // now get properties of assay - we should have one matching our factor value
        assertSame("Wrong number of properties", assay.getProperties().size(), 1);
        for (AssayProperty p : assay.getProperties()) {
            assertEquals("Wrong property name", p.getName(), TYPE);
            assertEquals("Wrong property value", p.getValue(),
                    "specific factor value");
        }
    }
}
