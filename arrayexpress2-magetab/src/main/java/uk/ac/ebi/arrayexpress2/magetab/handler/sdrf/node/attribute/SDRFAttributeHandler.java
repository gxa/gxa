package uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.attribute;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SDRFNode;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.SDRFHandler;

/**
 * An SDRF Handler that handles attributes within an SDRF file and attaches them
 * to the parent node.  This handler will create relevant objects in the
 * MAGE-TAB model, but it will associate them with the correct parent SDRFNode,
 * rather than creating them afresh.
 *
 * @author Tony Burdett
 * @date 27-Jan-2009
 */
public interface SDRFAttributeHandler extends SDRFHandler {
  /**
   * Configure this handler with the SDRFNode these attributes will be attached
   * to.
   *
   * @param parentNode the SDRFNode that will be read to
   */
  void setParentNode(SDRFNode parentNode);
}
