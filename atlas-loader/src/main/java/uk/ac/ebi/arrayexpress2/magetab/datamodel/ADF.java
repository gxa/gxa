package uk.ac.ebi.arrayexpress2.magetab.datamodel;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node.ADFNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.graph.Node;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.graph.UnresolvedPlaceholderNode;
import uk.ac.ebi.arrayexpress2.magetab.lang.AbstractProgressibleStatifiableFromTasks;
import uk.ac.ebi.arrayexpress2.magetab.lang.Status;
import uk.ac.ebi.arrayexpress2.magetab.utils.ADFWriter;
import uk.ac.ebi.arrayexpress2.magetab.utils.MAGETABUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.*;

/**
 * A datastructure that models the ADF in the MAGE-TAB spec.  Fields for the
 * header part (which resembles the {@link IDF} arrangement) are public. Methods
 * are provided for accessing the graph part (resembling {@link SDRF}) by
 * retrieving nodes by type.
 *
 * @author Tony Burdett
 * @date 09-Feb-2010
 * @see IDF
 * @see SDRF
 */
public class ADF extends AbstractProgressibleStatifiableFromTasks {
  // fields that represent the data structure of ADF headers pretty precisely
  public volatile String magetabVersion = "1.1";

  public volatile String arrayDesignName = "";
  public volatile String version = "";

  public volatile String provider = "";
  public volatile String printingProtocol = "";

  public volatile List<String> technologyType = new ArrayList<String>();
  public volatile List<String> technologyTypeTermSourceRef =
      new ArrayList<String>();
  public volatile List<String> technologyTypeTermAccession =
      new ArrayList<String>();

  public volatile List<String> surfaceType = new ArrayList<String>();
  public volatile List<String> surfaceTypeTermSourceRef =
      new ArrayList<String>();
  public volatile List<String> surfaceTypeTermAccession =
      new ArrayList<String>();

  public volatile List<String> substrateType = new ArrayList<String>();
  public volatile List<String> substrateTypeTermSourceRef =
      new ArrayList<String>();
  public volatile List<String> substrateTypeTermAccession =
      new ArrayList<String>();

  public volatile List<String> sequencePolymerType = new ArrayList<String>();
  public volatile List<String> sequencePolymerTypeTermSourceRef =
      new ArrayList<String>();
  public volatile List<String> sequencePolymerTypeTermAccession =
      new ArrayList<String>();

  public volatile List<String> termSourceName = new ArrayList<String>();
  public volatile List<String> termSourceFile = new ArrayList<String>();
  public volatile List<String> termSourceVersion = new ArrayList<String>();

  private volatile Map<String, Set<String>> comments =
      new HashMap<String, Set<String>>();

  // fields for representing the ADF graph
  private Map<Class<? extends ADFNode>, Set<ADFNode>> nodeStoreByClass =
      new HashMap<Class<? extends ADFNode>, Set<ADFNode>>();
  private Map<String, Set<ADFNode>> nodeStoreByTag =
      new HashMap<String, Set<ADFNode>>();

  /**
   * Indexes "unresolved" child nodes to the set of parents that reference them.
   * Whenever a node is stored, this collection is checked - if this Map
   * contains an unresolved node equal to the new node, references should be
   * updated.
   */
  private Map<Node, Set<ADFNode>> unresolvedChildren =
      new HashMap<Node, Set<ADFNode>>();

  private URL location;

  /**
   * Get the known location of this SDRF, being the location of the file which
   * was parsed to generate it.  This may be used by handlers wishing to read
   * files declared with some relative location to the SDRF.
   *
   * @return the location of the SDRF file
   */
  public URL getLocation() {
    return location;
  }

  /**
   * Get the known location of this SDRF, being the location of the file which
   * was parsed to generate it.  This may be used by handlers wishing to read
   * files declared with some relative location to the SDRF.
   *
   * @param location the full URL of the SDRF file represented by this SDRF
   *                 object
   */
  public void setLocation(URL location) {
    this.location = location;
  }

  /**
   * Explicitly sets the last status of the IDF parsing operation.  You should
   * not normally use this, unless you have a reason to explicitly set the
   * status of IDF parsing to "FAILED".  Setting the status will override the
   * last known status, so that when the next handler updates no notifications
   * may occur, depending on whether this will result in a status update.
   *
   * @param nextStatus the status to set for this IDF
   */
  public void setStatus(Status nextStatus) {
    super.setStatus(nextStatus);
  }

  /**
   * Add a comment to the array design, keyed by type.
   *
   * @param type    the type of the ADF comment
   * @param comment the value of the comment
   */
  public synchronized void addComment(String type, String comment) {
    if (!comments.containsKey(type)) {
      comments.put(type, new HashSet<String>());
    }
    comments.get(type).add(comment);
  }

  /**
   * Get the map of all current comments.
   *
   * @return the comments on this IDF
   */
  public synchronized Map<String, Set<String>> getComments() {
    return comments;
  }

  /**
   * Store a node in the SDRF graph.  This node will be added to the internal
   * graph representation of nodes represented by this SDRF object.  In this
   * way, this SDRF object represents the data encoded by the spreadsheet rather
   * than directly representing the spreadsheet data structure.
   *
   * @param node the node to store
   */
  public synchronized void storeNode(ADFNode node) {
    // store by class
    if (!nodeStoreByClass.containsKey(node.getClass())) {
      // no previous of this type, make a new list
      Set<ADFNode> nodes = new HashSet<ADFNode>();
      nodeStoreByClass.put(node.getClass(), nodes);
    }
    nodeStoreByClass.get(node.getClass()).add(node);

    // store by tag type
    if (!nodeStoreByTag.containsKey(node.getNodeType())) {
      // no previous of this type, make a new list
      Set<ADFNode> nodes = new HashSet<ADFNode>();
      nodeStoreByTag.put(node.getNodeType(), nodes);
    }
    nodeStoreByTag.get(node.getNodeType()).add(node);

    // finally, update all parent/child references to resolve graph structure
    resolveGraphStructure(node);

    // notify everything waiting on the SDRF that we've added new nodes
    notifyAll();
  }

  /**
   * Updates an node in the ADF graph, on the assumption that its position in
   * the graph, or its children, have been altered.  Whenever you add or remove
   * children or parents of a node, you should call this method to ensure the
   * ADF graph stays accurate
   *
   * @param node the node that needs updating
   */
  public synchronized void updateNode(ADFNode node) {
    ADFNode storedNode = lookupNode(node.getNodeName(), node.getNodeType());
    if (storedNode != node) {
      // different object references, need merging
      throw new UnsupportedOperationException("Stored node and node " +
          "parameter do not reference the same object and therefore " +
          "require merging - this is not currently supported");
    }
    else {
      // update all parent/child references to resolve graph structure
      resolveGraphStructure(node);

      // notify everything waiting on the SDRF that we've added new nodes
      notifyAll();
    }
  }

  /**
   * Determine whether a given node has been handled already, and exists in the
   * graph.  Essentially this is a convenience method that delegates to {@link
   * #lookupNode(String, Class)}.
   *
   * @param nodeName the name of the node
   * @param nodeType the class type of the node
   * @return true if it exists in the graph already, false otherwise
   */
  public synchronized boolean hasBeenHandled(String nodeName,
                                             Class<? extends ADFNode> nodeType) {
    return lookupNode(nodeName, nodeType) != null;
  }

  /**
   * Lookup a node by name and class type in the SDRF graph.  This will return
   * the node if it exists, or null if not.
   *
   * @param nodeName the node name
   * @param nodeType the class type of the node
   * @param <T>      the generic node type to lookup
   * @return the node, if it exists, or null otherwise
   */
  public synchronized <T extends ADFNode> T lookupNode(String nodeName,
                                                       Class<T> nodeType) {
    // check we have some nodes of this type
    if (nodeStoreByClass.containsKey(nodeType)) {
      // if so, check the list contains one with this id
      Set<ADFNode> nodes = nodeStoreByClass.get(nodeType);
      for (ADFNode node : nodes) {
        if (node.getNodeName().equals(nodeName)) {
          return (T) node;
        }
      }
    }

    // if we get to here, either we have no node of this type or none with the same name
    return null;
  }

  /**
   * Lookup a node by name and type in the SDRF graph.  This will return the
   * node if it exists, or null if not.
   *
   * @param nodeName the node name
   * @param nodeType the tag that describes the node in the SDRF spec
   * @return the node, if it exists, or null otherwise
   */
  public synchronized ADFNode lookupNode(String nodeName, String nodeType) {
    String type = MAGETABUtils.digestHeader(nodeType);

    if (nodeStoreByTag.containsKey(type)) {
      Set<? extends ADFNode> nodes = nodeStoreByTag.get(type);
      for (ADFNode node : nodes) {
        if (node.getNodeName().equals(nodeName)) {
          return node;
        }
      }
    }

    // if we get to here, either we have no node of this type or none with the same name
    return null;
  }

  /**
   * Lookup all nodes of the given class type in the SDRF graph.  This will
   * return a collection of the nodes of this type, which will be empty if there
   * are none.
   *
   * @param nodeType the class type of the node
   * @param <T>      the generic node type to lookup
   * @return the node, if it exists, or null otherwise
   */
  public synchronized <T extends ADFNode> List<T> lookupNodes(
      Class<T> nodeType) {
    // check we have some nodes of this type
    if (nodeStoreByClass.containsKey(nodeType)) {
      ArrayList<ADFNode> result = new ArrayList<ADFNode>();
      result.addAll(nodeStoreByClass.get(nodeType));
      return (ArrayList<T>) result;
    }
    else {
      // no nodes of this type, return an empty lilst
      return new ArrayList<T>();
    }
  }

  /**
   * Lookup all nodes of the given type (by SDRF spec name) in the SDRF graph.
   * This will return a collection of the nodes of this type, which will be
   * empty if there are none.
   *
   * @param nodeType the class type of the node
   * @return the node, if it exists, or null otherwise
   */
  public synchronized List<? extends ADFNode> lookupNodes(
      String nodeType) {
    if (nodeStoreByTag.containsKey(nodeType)) {
      ArrayList<ADFNode> result = new ArrayList<ADFNode>();
      result.addAll(nodeStoreByTag.get(nodeType));
      return result;
    }
    else {
      // no nodes of this type, return an empty lilst
      return new ArrayList<ADFNode>();
    }
  }

  /**
   * Lookup all 'root' nodes in the ADF graph.  A root node is the node in the
   * graph declared at the far left hand side of the SDRF spreadsheet; the nodes
   * which all others are children of.  Normally, this is likely to be a
   * "Feature" or a "Composite Element" node, but the ADF specification allows
   * for any nodes to be present here.  Use this method to return the full
   * collection.
   * <p/>
   * This method is essentially a convenience method around {@link
   * #lookupNodes(Class)}, substituting the class parameter for the known root
   * node type.
   *
   * @return the collection of root nodes in the ADF graph
   */
  public synchronized Collection<? extends ADFNode> lookupRootNodes() {
    Set<ADFNode> results = new HashSet<ADFNode>();

    // iterate over every node
    for (Set<ADFNode> nodes : nodeStoreByClass.values()) {
      for (ADFNode node : nodes) {
        if (node.getParentNodes().size() == 0) {
          // no parents, this is a parent node...
          if (!(node instanceof UnresolvedPlaceholderNode)) {
            // ... as long as its not a placeholder
            results.add(node);
          }
        }
      }
    }

    return results;
  }

  public String toString() {
    StringWriter writer = new StringWriter();

    writer.write("MAGE-TAB Version\t" + magetabVersion + "\n");
    writer.write("Array Design Name\t" + arrayDesignName + "\n");
    writer.write("Version\t" + version + "\n");
    writer.write("Provider\t" + provider + "\n");
    writer.write("Printing Protocol\t" + printingProtocol + "\n");
    writer.write("Technology Type\t" + technologyType + "\n");
    writer.write(
        "Technology Type Term Source Ref\t" + technologyTypeTermSourceRef +
            "\n");
    writer.write("Surface Type\t" + surfaceType + "\n");
    writer.write(
        "Surface Type Term Source Ref\t" + surfaceTypeTermSourceRef + "\n");
    writer.write("Substrate Type\t" + substrateType + "\n");
    writer.write(
        "Substrate Type Term Source Ref\t" + substrateTypeTermSourceRef + "\n");
    writer.write("Sequence Polymer Type\t" + sequencePolymerType + "\n");
    writer.write("Sequence Polymer Type Term Source Ref\t" +
        sequencePolymerTypeTermSourceRef + "\n");
    writer.write("Term Source Name\t" + termSourceName + "\n");
    writer.write("Term Source File\t" + termSourceFile + "\n");
    writer.write("Term Source Version\t" + termSourceVersion + "\n");
    writer.write("\n");

    try {
      ADFWriter adfWriter = new ADFWriter();
      adfWriter.writeADF(this, writer);

      return writer.toString();
    }
    catch (IOException e) {
      throw new RuntimeException(
          "An error caused the SDRF writer to abort" +
              (e.getMessage() != null
                  ? ": " + e.getMessage()
                  : ".  Cause unknown."),
          e);
    }
  }

  /**
   * Update this node being stored with appropriate references to it's parents
   * and update any child node references that are already present in the
   * graph.
   *
   * @param node the node being stored
   */
  private void resolveGraphStructure(ADFNode node) {
    synchronized (node) {
      // first, find any nodes that are already stored that reference this as a child
      if (unresolvedChildren.containsKey(node)) {
        // we've stored parents of this node already
        Set<ADFNode> unlinkedParents = new HashSet<ADFNode>();
        unlinkedParents.addAll(unresolvedChildren.get(node));
        for (ADFNode parentNode : unlinkedParents) {
          // set the parent reference on our current node
          node.addParentNode(parentNode);
          // and update the child reference on the parent
          parentNode.updateChildNode(node);
          // finally, remove from unresolved children now we've updated correctly
          unresolvedChildren.get(node).remove(parentNode);
          // and remove the index if all children have been updated
          if (unresolvedChildren.get(node).size() == 0) {
            unresolvedChildren.remove(node);
          }
        }
      }

      // next, attempt to resolve the chidren of our new node
      for (Node child : node.getChildNodes()) {
        // if the child node is a placeholder, update the reference
        if (child instanceof UnresolvedPlaceholderNode) {
          // get the actual stored node, if possible
          ADFNode realChild = lookupNode(
              child.getNodeName(), child.getNodeType());

          if (realChild != null) {
            // if we found the "real" child, update the reference
            node.updateChildNode(realChild);
          }
          else {
            // otherwise, store this node in unresolvedChildren,
            // to be resolved when we store the proper child
            if (unresolvedChildren.containsKey(realChild)) {
              unresolvedChildren.get(realChild).add(node);
            }
            else {
              Set<ADFNode> set = new HashSet<ADFNode>();
              set.add(node);
              unresolvedChildren.put(child, set);
            }
          }
        }
        else {
          // our new node already references a "real" child node
          // so make sure the child references this as a parent
          if (!child.getParentNodes().contains(node)) {
            // add the reference
            child.addParentNode(node);
          }
        }
      }
    }
  }
}
