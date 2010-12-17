package uk.ac.ebi.arrayexpress2.magetab.handler.adf.node.attribute;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node.ADFNode;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.ADFGraphHandler;

/**
 * Handles attributes of nodes that are described in the graph part of the ADF.
 *
 * @author Tony Burdett
 * @date 15-Feb-2010
 */
public interface ADFAttributeHandler extends ADFGraphHandler {
  /**
   * Configure this handler with the ADFNode these attributes will be attached
   * to.
   *
   * @param parentNode the ADFNode that will be read to
   */
  void setParentNode(ADFNode parentNode);
}
