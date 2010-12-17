package uk.ac.ebi.arrayexpress2.magetab.datamodel.graph;

/**
 * A dedicated type of graph node that serves as a simple placeholder in lieu of
 * the node being fully parsed.  During graph construction, placeholder nodes
 * can be constructed before all details and attributes have been resolved. As
 * node equality is assessed on the basis of node type and name, this node will
 * be equal to "fully parsed" node objects, of the correct class type with all
 * attributes set. Graphs that use temporary placeholder nodes should then
 * implement a method for updating object references to the correct object.
 *
 * @author Tony Burdett
 * @date 28-Feb-2010
 */
public class UnresolvedPlaceholderNode extends AbstractNode {
  public UnresolvedPlaceholderNode(String tag, String value) {
    setNodeType(tag);
    setNodeName(value);
  }
}
