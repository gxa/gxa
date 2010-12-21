package uk.ac.ebi.arrayexpress2.magetab.handler.adf.node.attribute;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node.ADFNode;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.AbstractADFGraphHandler;

/**
 * An abstract implementation of an attribute handler.  This deals with some
 * simple aspects of setting the data on a handler and resolving the point in
 * the graph at which this attribute is inserted.
 *
 * @author Tony Burdett
 * @date 17-Feb-2010
 */
public abstract class AbstractADFAttributeHandler
    extends AbstractADFGraphHandler
    implements ADFAttributeHandler {
  protected ADFNode parentNode;

  public void setParentNode(ADFNode parentNode) {
    this.parentNode = parentNode;
  }

  public void setData(String[] headers, String[] values) {
    this.headers = headers;
    this.values = values;
  }

}
