package uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.graph.Node;

/**
 * A node in the SDRF data structure.  This defines no new methods compared to
 * the general {@link Node} interface, but is used to mark nodes as belonging to
 * the SDRF graph
 *
 * @author Tony Burdett
 * @date 22-Jan-2009
 */
public interface SDRFNode extends Node {
  /**
   * Returns a string array representing the headers of this node and all it's
   * attributes.
   *
   * @return the headers unique to this node
   */
  String[] headers();

  /**
   * Returns the strings representing the values of this node and all it's
   * attributes, indexed by the matching headers.  So, {@link #headers()} and
   * {@link #values()} have exactly the same length and the same order.
   *
   * @return the values for this node
   */
  String[] values();
}
