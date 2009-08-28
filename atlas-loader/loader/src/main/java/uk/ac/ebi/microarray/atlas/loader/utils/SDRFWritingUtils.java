package uk.ac.ebi.microarray.atlas.loader.utils;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AssayNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.HybridizationNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SourceNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.CharacteristicsAttribute;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.FactorValueAttribute;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.microarray.atlas.loader.model.Assay;
import uk.ac.ebi.microarray.atlas.loader.model.Property;
import uk.ac.ebi.microarray.atlas.loader.model.Sample;

/**
 * Javadocs go here!
 *
 * @author Tony Burdett
 * @date 28-Aug-2009
 */
public class SDRFWritingUtils {
  public static void writeProperties(
      MAGETABInvestigation investigation,
      Sample sample,
      SourceNode sourceNode)
      throws ObjectConversionException {
    // fetch characteristics of this sourceNode
    for (CharacteristicsAttribute characteristicsAttribute :
        sourceNode.characteristics) {

      // create Property for this attribute
      Property p = new Property();
      p.setAccession(AtlasLoaderUtils.getNodeAccession(
          investigation, characteristicsAttribute));
      p.setName(
          characteristicsAttribute.getNodeType()); // fixme: need extra type attribute?
      p.setValue(characteristicsAttribute.getNodeName());
      p.setFactorValue(false);

      sample.addProperty(p);

      // todo - characteristics can have ontology entries, and units (which can also have ontology entries) - set these values
    }
  }

  public static void writeProperties(
      MAGETABInvestigation investigation,
      Assay assay,
      AssayNode assayNode)
      throws ObjectConversionException {
    // create Property for technology type
    Property p = new Property();
    p.setAccession(AtlasLoaderUtils.getNodeAccession(
        investigation, assayNode.technologyType));
    p.setName(
        assayNode.technologyType.getNodeType()); // fixme: this node type ok?
    p.setValue(assayNode.technologyType.getNodeName());
    p.setFactorValue(false);

    assay.addProperty(p);

    // todo - assays can have factor values?
  }

  public static void writeProperties(
      MAGETABInvestigation investigation,
      Assay assay,
      HybridizationNode hybridizationNode)
      throws ObjectConversionException {
    // fetch characteristics of this sourceNode
    for (FactorValueAttribute factorValueAttribute :
        hybridizationNode.factorValues) {
      // create Property for this attribute
      Property p = new Property();
      p.setAccession(AtlasLoaderUtils.getNodeAccession(
          investigation, factorValueAttribute));
      p.setName(
          factorValueAttribute.getNodeType()); // fixme: need extra type attribute?
      p.setValue(factorValueAttribute.getNodeName());
      p.setFactorValue(true);

      assay.addProperty(p);

      // todo - factor values can have ontology entries, set these values
    }
  }
}
