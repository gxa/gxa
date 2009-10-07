package uk.ac.ebi.microarray.atlas.loader.utils;

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
