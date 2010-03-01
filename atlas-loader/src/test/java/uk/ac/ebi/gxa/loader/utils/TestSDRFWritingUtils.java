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

package uk.ac.ebi.gxa.loader.utils;

import junit.framework.TestCase;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AssayNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.HybridizationNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SourceNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.CharacteristicsAttribute;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.FactorValueAttribute;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.Sample;
import uk.ac.ebi.gxa.loader.utils.SDRFWritingUtils;

/**
 * Javadocs go here.
 *
 * @author Junit Generation Plugin for Maven, written by Tony Burdett
 * @date 07-10-2009
 */
public class TestSDRFWritingUtils extends TestCase {
  public void setUp() {
    // add setup logic here
  }

  public void tearDown() {
    // add logic required to terminate the class here
  }

  public void testWriteAssayProperties() {
    // create investigation
    MAGETABInvestigation investigation = new MAGETABInvestigation();
    investigation.accession = "TEST-INVESTIGATION";

    Assay assay = new Assay();
    assay.setAccession("TEST-ASSAY");

    AssayNode assayNode = new AssayNode();
    FactorValueAttribute fva = new FactorValueAttribute();
    fva.type = "Type";
    fva.setNodeName("specific factor value");
    assayNode.factorValues.add(fva);

    try {
      SDRFWritingUtils.writeAssayProperties(investigation, assay, assayNode);

      // now get properties of assay - we should have one matching our factor value
      assertSame("Wrong number of properties", assay.getProperties().size(), 1);
      for (Property p : assay.getProperties()) {
        assertEquals("Wrong property name", p.getName(), "Type");
        assertEquals("Wrong property value", p.getValue(),
                     "specific factor value");
      }
    }
    catch (ObjectConversionException e) {
      e.printStackTrace();
      fail();
    }
  }

  public void testWriteSampleProperties() {
    // create investigation
    MAGETABInvestigation investigation = new MAGETABInvestigation();
    investigation.accession = "TEST-INVESTIGATION";

    Sample sample = new Sample();
    sample.setAccession("TEST-SAMPLE");

    SourceNode sourceNode = new SourceNode();
    CharacteristicsAttribute fva = new CharacteristicsAttribute();
    fva.type = "Type";
    fva.setNodeName("specific factor value");
    sourceNode.characteristics.add(fva);

    try {
      SDRFWritingUtils.writeSampleProperties(investigation, sample, sourceNode);

      // now get properties of assay - we should have one matching our factor value
      assertSame("Wrong number of properties", sample.getProperties().size(),
                 1);
      for (Property p : sample.getProperties()) {
        assertEquals("Wrong property name", p.getName(), "Type");
        assertEquals("Wrong property value", p.getValue(),
                     "specific factor value");
      }
    }
    catch (ObjectConversionException e) {
      e.printStackTrace();
      fail();
    }
  }

  public void testWriteHybridizationProperties() {
    // create investigation
    MAGETABInvestigation investigation = new MAGETABInvestigation();
    investigation.accession = "TEST-INVESTIGATION";

    Assay assay = new Assay();
    assay.setAccession("TEST-SAMPLE");

    HybridizationNode hybridizationNode = new HybridizationNode();
    FactorValueAttribute fva = new FactorValueAttribute();
    fva.type = "Type";
    fva.setNodeName("specific factor value");
    hybridizationNode.factorValues.add(fva);

    try {
      SDRFWritingUtils.writeHybridizationProperties(investigation, assay,
                                                    hybridizationNode);

      // now get properties of assay - we should have one matching our factor value
      assertSame("Wrong number of properties", assay.getProperties().size(), 1);
      for (Property p : assay.getProperties()) {
        assertEquals("Wrong property name", p.getName(), "Type");
        assertEquals("Wrong property value", p.getValue(),
                     "specific factor value");
      }
    }
    catch (ObjectConversionException e) {
      e.printStackTrace();
      fail();
    }
  }
}
