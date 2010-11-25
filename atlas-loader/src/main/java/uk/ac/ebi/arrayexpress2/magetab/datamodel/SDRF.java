package uk.ac.ebi.arrayexpress2.magetab.datamodel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.graph.Node;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.graph.UnresolvedPlaceholderNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.ProtocolApplicationNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SDRFNode;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.lang.AbstractProgressibleStatifiableFromTasks;
import uk.ac.ebi.arrayexpress2.magetab.lang.Status;
import uk.ac.ebi.arrayexpress2.magetab.utils.MAGETABUtils;
import uk.ac.ebi.arrayexpress2.magetab.utils.SDRFUtils;
import uk.ac.ebi.arrayexpress2.magetab.utils.SDRFWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.*;

/**
 * A datastructure that simply models SDRF in the MAGE-TAB spec.  Nodes are
 * stored in an internal node store, and can be accessed by the {@link
 * #lookupNode(String, Class)} method.  Because nodes are arranged in a
 * graph-like structure, each node contains a reference to the ID of the child
 * node.
 * <p/>
 * For example, lets consider we have an SDRF snippet like:
 * <p/>
 * <pre>
 * Source     Characteristic[organism]    ...
 * source_1   homo sapiens                ...
 * source_2   mus musculus                ...
 * </pre>
 * <p/>
 * The SDRF representation on the object model is slightly different from the
 * IDF, it stores the graph rather than a direct representation of the
 * spreadsheet.  Therefore, you can use the methods lookupNode() and
 * lookupNodes() on the SDRF to access the top-level nodes on the graph.  These
 * methods are generically typed, so for example you could do:
 * <p/>
 * <pre><code>
 * // this list will contain two nodes
 * List<SourceNode> sources = investigation.SDRF.lookupNodes(SourceNode.class);
 * <p/>
 * // will print "source_1" then "source_2"
 * for (SourceNode source : sources) {
 *   System.out.println(source.getNodeName());
 * }
 * </code></pre>
 * Alternatively, if you already know the node name, you can retrieve it
 * specifically
 * <p/>
 * <pre><code>
 * // this is the first source node, source_1 SourceNode source_1 =
 * investigation.SDRF.lookupNode("source_1", SourceNode.class);
 * </code></pre>
 * <p/>
 * You can then extract the attributes of these by accessing the public fields.
 * Some nodes can have a list of attributes, in which case the public field
 * returns a list.  For characteristics of this source_1 node, you might want
 * some code like this:
 * <p/>
 * <pre><code>
 * // this list will contain one element List<CharacteristicsAttribute>
 * characteristicsList = source_1.characteristics;
 * </code></pre>
 * <p/>
 * To get the specific types and values, now all you need to do with this list
 * of elements is the following;
 * <p/>
 * <pre><code>
 * for (CharacteristicsAttribute characteristics : characteristicsList) {
 *   // will return "organism" for the above snippet
 *   String organism = characteristics.type;
 *   // will return "homo sapiens" for source_1
 *   String homo_sapiens = characteristics.getNodeName();
 * }
 * </code></pre>
 * <p/>
 * Obviously, because the attributes are tied to each node, it isn't possible to
 * get all values from a single attribute column - you have to iterate over all
 * the nodes in the graph to "rebuild" the attribute columns.
 * <p/>
 * Note that protocols are the exception to this general pattern.  To obtain
 * Protocols, you should not use the {@link #lookupNodes(Class)} method, instead
 * look for the public field on the node to which this protocol is associated.
 * So, for example...
 * <p/>
 * <pre><code>
 *   SourceNode src = ...
 *   List<ProtocolNode> protocols = src.protocols
 * </code></pre>
 *
 * @author Tony Burdett
 * @date 12-Feb-2009
 * @see IDF
 * @see ADF
 */
public class SDRF extends AbstractProgressibleStatifiableFromTasks {
  private Map<Class<? extends SDRFNode>, Set<SDRFNode>> nodeStoreByClass =
      new HashMap<Class<? extends SDRFNode>, Set<SDRFNode>>();
  private Map<String, Set<SDRFNode>> nodeStoreByTag =
      new HashMap<String, Set<SDRFNode>>();

  /**
   * Indexes "unresolved" child nodes to the set of parents that reference them.
   * Whenever a node is stored, this collection is checked - if this Map
   * contains an unresolved node equal to the new node, references should be
   * updated.
   */
  private volatile Map<Node, Set<SDRFNode>> unresolvedChildren =
      new HashMap<Node, Set<SDRFNode>>();

  /**
   * Stores the mapping between labeled dyes and the channel it is assigned to.
   * This is required to determine which factor values for a hybridization or a
   * scan were assigned to which channel, as different values may be detected by
   * different dyes.
   */
  private Map<String, Integer> channelMapping = new HashMap<String, Integer>();

  private URL location;

  private static Log log = LogFactory.getLog(SDRF.class);

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
   * Explicitly sets the last status of the SDRF parsing operation.  You should
   * not normally use this, unless you have a reason to explicitly set the
   * status of SDRF parsing to failed.  Setting the status will override the
   * last known status, so that when the next handler updates no notifications
   * may occur, depending on whether this will result in a status update.
   *
   * @param nextStatus the status to set for this SDRF
   */
  public void setStatus(Status nextStatus) {
    super.setStatus(nextStatus);
  }

  /**
   * Store a node in the SDRF graph.  This node will be added to the internal
   * graph representation of nodes represented by this SDRF object.  In this
   * way, this SDRF object represents the data encoded by the spreadsheet rather
   * than directly representing the spreadsheet data structure.
   *
   * @param node the node to store
   * @throws uk.ac.ebi.arrayexpress2.magetab.exception.ParseException
   *          if the node being stored would violate the principle that the
   *          graph is acyclic
   */
  public synchronized void storeNode(SDRFNode node) throws ParseException {
    // store by class
    if (!nodeStoreByClass.containsKey(node.getClass())) {
      // no previous of this type, make a new list
      Set<SDRFNode> nodes = new HashSet<SDRFNode>();
      nodeStoreByClass.put(node.getClass(), nodes);
    }
    nodeStoreByClass.get(node.getClass()).add(node);

    // store by tag type
    String type = MAGETABUtils.digestHeader(node.getNodeType());
    if (!nodeStoreByTag.containsKey(type)) {
      // no previous of this type, make a new list
      Set<SDRFNode> nodes = new HashSet<SDRFNode>();
      nodeStoreByTag.put(type, nodes);
    }
    nodeStoreByTag.get(type).add(node);

    // finally, update all parent/child references to resolve graph structure
    resolveGraphStructure(node);

    // notify everything waiting on the SDRF that we've added new nodes
    notifyAll();
  }

  /**
   * Updates an node in the SDRF graph, on the assumption that its position in
   * the graph, or its children, have been altered.  Whenever you add or remove
   * children or parents of a node, you should call this method to ensure the
   * SDRF graph stays accurate
   *
   * @param node the node that needs updating
   * @throws uk.ac.ebi.arrayexpress2.magetab.exception.ParseException
   *          if the node being stored would violate the principle that the
   *          graph is acyclic
   */
  public synchronized void updateNode(SDRFNode node) throws ParseException {
    synchronized (node) {
      SDRFNode storedNode = lookupNode(node.getNodeName(), node.getNodeType());
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
                                             Class<? extends SDRFNode> nodeType) {
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
  public synchronized <T extends SDRFNode> T lookupNode(String nodeName,
                                                        Class<T> nodeType) {
    // check we have some nodes of this type
    if (nodeStoreByClass.containsKey(nodeType)) {
      // if so, check the list contains one with this id
      Set<SDRFNode> nodes = nodeStoreByClass.get(nodeType);
      for (SDRFNode node : nodes) {
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
  public synchronized SDRFNode lookupNode(String nodeName, String nodeType) {
    String type = MAGETABUtils.digestHeader(nodeType);

    if (nodeStoreByTag.containsKey(type)) {
      Set<? extends SDRFNode> nodes = nodeStoreByTag.get(type);
      for (SDRFNode node : nodes) {
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
  public synchronized <T extends SDRFNode> Collection<T> lookupNodes(
      Class<T> nodeType) {
    // check we have some nodes of this type
    if (nodeStoreByClass.containsKey(nodeType)) {
      Set<SDRFNode> result = new HashSet<SDRFNode>();
      result.addAll(nodeStoreByClass.get(nodeType));
      return (Collection<T>) result;
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
  public synchronized Collection<? extends SDRFNode> lookupNodes(
      String nodeType) {
    String type = MAGETABUtils.digestHeader(nodeType);

    if (nodeStoreByTag.containsKey(type)) {
      Set<SDRFNode> result = new HashSet<SDRFNode>();
      result.addAll(nodeStoreByTag.get(type));
      return result;
    }
    else {
      // no nodes of this type, return an empty lilst
      return new ArrayList<SDRFNode>();
    }
  }

  /**
   * Returns the number of channels in this experiment.  Most MAGE-TAB
   * investigations tend to be single channel experiments, so unless multiple
   * channels are observed (by labelling extracts with different dyes) this
   * method will return 1.  For multichannel experiments, this will increase
   * with every unique dye described.  This method does no validation, so typos
   * in the label column will be interpreted as new dyes and could falsely
   * increase the number of channels.  Checking the number of incoming edges
   * into every hybridization against the number of labelling dyes is a good way
   * to check these results are correct.
   *
   * @return the number of channels in this experiment
   */
  public synchronized int getNumberOfChannels() {
    if (channelMapping.size() == 0) {
      // no such thing as a zero channel experiment, so just assume single channel.
      return 1;
    }
    else {
      return channelMapping.size();
    }
  }

  /**
   * Returns the channel number assigned to any given label.  Factor Values are
   * attributes of hybridizations, but are assigned a channel number ({@link
   * uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.FactorValueAttribute}<code>.scannerChannel</code>).
   * This method returns the scanner channel assigned to a given label.
   *
   * @param label the label that uniquely identifies a channel in an SDRF
   * @return the channel number assigned to that label
   */
  public synchronized int getChannelNumber(String label) {
    if (!channelMapping.containsKey(label)) {
      // this is a new dye - we need to store it
      int occupiedChannels = channelMapping.size();
      channelMapping.put(label, occupiedChannels + 1);
    }
    return channelMapping.get(label);
  }

  /**
   * Returns the label that tags a given channel.  By passing the channel number
   * for a given factor value attribute to this method, you can acquire the
   * label that matches this channel.
   *
   * @param channelNumber the channel number assigned to a factor value
   *                      attribute
   * @return the label name this channel is labeled with
   */
  public synchronized String getLabelForChannel(int channelNumber) {
    if (channelMapping.containsValue(channelNumber)) {
      for (String label : channelMapping.keySet()) {
        if (channelMapping.get(label) == channelNumber) {
          return label;
        }
      }
    }
    throw new IllegalArgumentException(
        "No channel " + channelNumber + " assigned");
  }

  /**
   * Lookup all named protocols references in this SDRF.  Protocols are treated
   * as a special case in SDRF parsing - whenever a Protocol REF column is
   * observed in the SDRF, a new node type, ProtocolApplicationNode, is created.
   * This represents a unique node in the graph where the given protocol is
   * applied to the given parent node.  This ProtocolApplicationNode then
   * references the protocol named in the SDRF (or "Unknown Protocol" if the
   * reference is not specified).
   * <p/>
   * Using this method, you can retrieve a list of all protocol names referenced
   * within the SDRF.  Alternatively, to find the unique applications of these
   * protocols, you can do <code>lookupNodes(ProtocolApplicationNode.class);</code>.
   * By iterating over the list of returned protocol application nodes, and
   * extracting the protocol from each (use <code>protocolApplicationNode.protocol</code>)
   * you can reconstruct this list, or count the number of times a particular
   * protocol was applied, and so on.
   * <p/>
   * This method returns a collection that only contains the names of the
   * protocols referenced in the SDRF.  It does not contain duplicates, and ti
   * does not imply anything about the usage of these protocols or a collection
   * of the "nodes" that utilise these protocols.  You can acquire more detailed
   * information about protocol usages by retrieving a list of
   * <code>ProtocolApplicationNodes</code> using the {@link #lookupNodes(Class)}
   * and providing the parameter "<code>ProtocolApplicationNode.class</code>
   *
   * @return a collection of the names of all the protocols referenced in this
   *         SDRF
   */
  public synchronized Collection<String> lookupProtocols() {
    // set of unique protocol name strings
    Set<String> protocolNames = new HashSet<String>();
    // lookup all protocol applications
    Collection<ProtocolApplicationNode> protocolApplications =
        lookupNodes(ProtocolApplicationNode.class);
    // iterate over the set and store names
    for (ProtocolApplicationNode pan : protocolApplications) {
      protocolNames.add(pan.protocol);
    }
    return protocolNames;
  }

  /**
   * Lookup all 'root' nodes in the SDRF graph.  A root node is the node in the
   * graph declared at the far left hand side of the SDRF spreadsheet; the nodes
   * which all others are children of.  Normally, this is likely to be a
   * "Source" node, but the SDRF specification allows for any nodes to be
   * present here.  Use this method to return the full collection.
   * <p/>
   * This method is essentially a convenience method around {@link
   * #lookupNodes(Class)}, substituting the class parameter for the known root
   * node type.
   *
   * @return the collection of root nodes in the SDRF graph
   */
  public synchronized Collection<? extends SDRFNode> lookupRootNodes() {
    Set<SDRFNode> results = new HashSet<SDRFNode>();

    // iterate over every node
    for (Set<SDRFNode> nodes : nodeStoreByClass.values()) {
      for (SDRFNode node : nodes) {
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

  /**
   * Returns the total number of nodes stored by this SDRF graph.
   *
   * @return the number of nodes in this SDRF
   */
  public synchronized int getNodeCount() {
    int count = 0;
    for (Set<SDRFNode> nodeSet : nodeStoreByClass.values()) {
      count += nodeSet.size();
    }
    return count;
  }

  public String toString() {
    StringWriter writer = new StringWriter();
    try {
      SDRFWriter sdrfWriter = new SDRFWriter();
      sdrfWriter.writeSDRF(this, writer);

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
   * @throws uk.ac.ebi.arrayexpress2.magetab.exception.ParseException
   *          if the node being stored would violate the principle that the
   *          graph is acyclic
   */
  private synchronized void resolveGraphStructure(SDRFNode node)
      throws ParseException {
    synchronized (node) {
      // first, find any nodes that are already stored that reference this as a child
      if (unresolvedChildren.containsKey(node)) {
        // we've stored parents of this node already
        Set<SDRFNode> unlinkedParents = new HashSet<SDRFNode>();
        unlinkedParents.addAll(unresolvedChildren.get(node));
        for (SDRFNode parentNode : unlinkedParents) {
          checkForCycles(parentNode, node);

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

      // next, attempt to resolve the children of our new node
      for (Node child : node.getChildNodes()) {
        // if the child node is a placeholder, update the reference
        if (child instanceof UnresolvedPlaceholderNode) {
          // get the actual stored node, if possible
          SDRFNode realChild = lookupNode(
              child.getNodeName(), child.getNodeType());

          if (realChild != null) {
            // if we found the "real" child, check for cycles
            checkForCycles(node, realChild);

            // and then update the reference
            node.updateChildNode(realChild);
          }
          else {
            // otherwise, store this node in unresolvedChildren,
            // to be resolved when we store the proper child
            if (unresolvedChildren.containsKey(child)) {
              unresolvedChildren.get(child).add(node);
            }
            else {
              Set<SDRFNode> set = new HashSet<SDRFNode>();
              set.add(node);
              unresolvedChildren.put(child, set);
            }
          }
        }
        else {
          // our new node already references a "real" child node
          // so make sure the child references this as a parent
          if (!child.getParentNodes().contains(node)) {
            // make sure that adding the parent won't introduce a cycle
            checkForCycles(node, child);

            // add the reference
            child.addParentNode(node);
          }
        }
      }
    }
  }

  private synchronized void checkForCycles(Node node,
                                           Node childNode)
      throws ParseException {
    // check graph for integrity - must be acyclic

    // check the child node doesn't already contain
    // check nodes that are downstream of the child
    Collection<? extends Node> downstreamNodes =
        SDRFUtils.findDownstreamNodes(childNode, node.getNodeType());

    if (downstreamNodes.size() > 0) {
      log.debug("Possibility of SDRF graph cycle - there are " +
          downstreamNodes.size() + " nodes of type " + node.getNodeType() +
          " (class = " + node.getClass().getSimpleName() + ")" +
          " downstream of " + childNode.getNodeName());
    }

    if (downstreamNodes.contains(node)) {
      // this constitutes a serious error - SDRF graph contains a cycle!
      String message =
          "The SDRF graph is not acyclic - node '" + childNode.getNodeName() +
              "' is both upstream and downstream of " + node.getNodeName();

      ErrorItem error =
          ErrorItemFactory
              .getErrorItemFactory(getClass().getClassLoader())
              .generateErrorItem(
                  message,
                  ErrorCode.BAD_SDRF_ORDERING,
                  this.getClass());

      throw new ParseException(error, true, message);
    }
  }
}

