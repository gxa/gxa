package uk.ac.ebi.arrayexpress2.magetab.datamodel.graph;

import java.util.Set;

/**
 * A node in a MAGETAB graph data structure (either ADF or SDRF).  Nodes have
 * defined types, depending on the header in the specification, and can have a
 * subset of attributes. These attributes vary according to the type. This
 * interface provides methods for setting the node type (i.e. the header value -
 * this may be qualified with subtypes in some cases by including [] or ()
 * values), the node name (the actual value entered into a cell) and the set of
 * child nodes in the graph, as encoded in the MAGE-TAB spreadsheet.
 *
 * @author Tony Burdett
 * @date 22-Jan-2009
 */
public interface Node {
  /**
   * Set the type of this node in the graph.  The node is typed by the
   * standardised "Name" tag for the node, including any option qualifiers (such
   * as contents of square brackets, parentheses, or prefixes in some cases).
   *
   * @param nodeType the standardised header string for this node
   */
  void setNodeType(String nodeType);

  /**
   * Get the type of this node in the graph.
   *
   * @return the standardised header string for this node
   */
  String getNodeType();

  /**
   * Set the value associated to this node in the spreadsheet.  In other words,
   * the particular entry in the current row under the given type.
   *
   * @param name the value for this node name entry
   */
  void setNodeName(String name);

  /**
   * Get the value associated to this node in the spreadsheet.  In other words,
   * the particular entry in the current row under the given type.
   *
   * @return the value for this node name entry
   */
  String getNodeName();

  /**
   * Adds the child node to this Node.
   *
   * @param childNode the node to add
   */
  void addChildNode(Node childNode);

  /**
   * Adds a child node to this Node.  The child node will be a placeholder until
   * all references are correctly resolved, but this placeholder will be equal
   * to the actual node.
   *
   * @param tag   the type (or column header, or tag) of the node to add
   * @param value the value (or node name) of the node to add
   */
  void addChildNode(String tag, String value);

  /**
   * Updates a child node equal to the supplied node with a new object
   * reference.  Use this whenever temporary nodes (usually of type {@link
   * uk.ac.ebi.arrayexpress2.magetab.datamodel.graph.UnresolvedPlaceholderNode})
   * have been stored, and are subsequently updated with a correct type.
   * <p/>
   * This method capitalises on the fact that nodes with the same type and name
   * parameter are equal, even if the actual object references are different.
   * Invoking this method updates the node to the new reference.
   *
   * @param nodeToUpdate the new object reference to use
   * @return true if the node was replaced, false otherwise
   */
  boolean updateChildNode(Node nodeToUpdate);

  /**
   * Returns the set of nodes that are children of this node.
   *
   * @return the nodes that are directly linked downstream of this node
   */
  Set<Node> getChildNodes();

  /**
   * Adds a parent node to this node
   *
   * @param parentNode the parent node to add
   */
  void addParentNode(Node parentNode);

  /**
   * Adds a parent node to this Node.  The parent node will be a placeholder
   * until all references are correctly resolved, but this placeholder will be
   * equal to the actual node.
   *
   * @param tag   the type (or column header, or tag) of the node to add
   * @param value the value (or node name) of the node to add
   */
  void addParentNode(String tag, String value);

  /**
   * Updates a parent node equal to the supplied node with a new object
   * reference.  Use this whenever temporary nodes (usually of type {@link
   * uk.ac.ebi.arrayexpress2.magetab.datamodel.graph.UnresolvedPlaceholderNode})
   * have been stored, and are subsequently updated with a correct type.
   * <p/>
   * This method capitalises on the fact that nodes with the same type and name
   * parameter are equal, even if the actual object references are different.
   * Invoking this method updates the node to the new reference.
   *
   * @param nodeToUpdate the new object reference to use
   * @return true if the node was replaced, false otherwise
   */
  boolean updateParentNode(Node nodeToUpdate);

  /**
   * Returns the set of nodes that are parents of this node.
   *
   * @return the parents of this node
   */
  Set<Node> getParentNodes();
}
