package uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.graph.AbstractNode;

/**
 * An abstract top-level implementation of an SDRFNode.  This provides no
 * additional functionality and exists as a marker abstract class, designating
 * nodes that extend it as belonging to the SDRF graph.
 *
 * @author Tony Burdett
 * @date 22-Jan-2009
 */
public abstract class AbstractSDRFNode
    extends AbstractNode
    implements SDRFNode {
  public String toString() {
    StringBuilder hb = new StringBuilder();
    for (String header : headers()) {
      hb.append(header).append("\t");
    }
    String header = hb.toString().trim();

    StringBuilder vb = new StringBuilder();
    for (String value : values()) {
      vb.append(value).append("\t");
    }
    String value = vb.toString().trim();

    return header + "\n" + value;
  }
}
