package uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.attribute;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SDRFNode;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.AbstractSDRFHandler;

/**
 * An abstract implementation of an attribute handler that can handles
 * attributes within an SDRF file and attach them to the parent node.  This
 * implementation provides the functionality for setting the parent node this
 * attribute is attached to.
 *
 * @author Tony Burdett
 * @date 27-Jan-2009
 */
public abstract class AbstractSDRFAttributeHandler extends AbstractSDRFHandler
    implements SDRFAttributeHandler {
  protected SDRFNode parentNode;

  public void setParentNode(SDRFNode parentNode) {
    this.parentNode = parentNode;
  }

  public void setData(String[] headers, String[] values) {
    this.headers = headers;
    this.values = values;
  }
}
