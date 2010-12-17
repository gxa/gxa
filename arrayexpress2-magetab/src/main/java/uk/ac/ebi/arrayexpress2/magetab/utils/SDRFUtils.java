package uk.ac.ebi.arrayexpress2.magetab.utils;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.graph.Node;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SDRFNode;

import java.util.Collection;
import java.util.HashSet;

/**
 * A class containing some useful utility methods for navigating the SDRF
 * graph.
 *
 * @author Tony Burdett
 * @date 04-Feb-2010
 */
public class SDRFUtils {
  /**
   * Finds nodes upstream of the given node in the SDRF graph, searching for
   * nodes of a particular type.  This returns all nodes upstream of the given
   * node, of the supplied type - this includes both those directly upstream or
   * those further hops away.  So, for example, if you have nodes a (type A), b1
   * (type B), b2 (type B) and c (type C) and your graph looks like:
   * <pre>
   *     a -> b1 -> b2 -> c
   * </pre>
   * If you call <code>findUpstreamNodes(sdrf, c, B.class);</code> your answer
   * will contain b1 and b2.
   * <p/>
   * Note that this method will return a snapshot of the nodes currently
   * upstream of the queried node if the SDRF graph is being actively
   * constructed.  This often won't matter, except where pooling occurs upstream
   * of the queried node.
   * <p/>
   * Note that there is a potential flaw in this method, and it is therefore
   * deprecated.  As the graph is resolved by the type of the upstream node
   * (given by the upstreamNodeType parameter), nodes that have been created as
   * UnresolvedPlaceholderNodes will not show up in this method.  Most of the
   * time, this is not a problem, but if your graph inadvertently contains a
   * cycle, unresolved placeholder nodes will not match the type of upstream
   * node requested and could therefore slip through undetected.
   *
   * @param currentNode      the current node - this method will locate all
   *                         nodes upstream of this node
   * @param upstreamNodeType the type of nodes you are interested in locating
   * @return the collection of SDRF nodes, of the "upstreamNodeType" type,
   *         upstream of "currentNode"
   * @deprecated
   */
  @Deprecated
  public static <T extends SDRFNode> Collection<T> findUpstreamNodes(
      SDRFNode currentNode, Class<T> upstreamNodeType) {
    Collection<T> results = new HashSet<T>();
    for (Node n : currentNode.getParentNodes()) {
      if (n instanceof SDRFNode) {
        SDRFNode nextParent = (SDRFNode) n;
        traverseUpstream(results, nextParent, upstreamNodeType);
      }
    }
    return results;
  }

  /**
   * Finds nodes upstream of the given node in the SDRF graph, searching for
   * nodes of a particular type.  This returns all nodes upstream of the given
   * node, where the value of upstreamNode.getNodeType() equals the type
   * specified.  This includes both those directly upstream or those further
   * hops away.  So, for example, if you have nodes a (type A), b1 (type B), b2
   * (type B) and c (type C) and your graph looks like:
   * <pre>
   *     a -> b1 -> b2 -> c
   * </pre>
   * If you call <code>findUpstreamNodes(sdrf, c, "B");</code> your answer will
   * contain b1 and b2.
   * <p/>
   * Note that this method will return a snapshot of the nodes currently
   * upstream of the queried node if the SDRF graph is being actively
   * constructed.  This often won't matter, except where pooling occurs upstream
   * of the queried node.
   * <p/>
   *
   * @param currentNode      the current node - this method will locate all
   *                         nodes upstream of this node
   * @param upstreamNodeType the type of nodes you are interested in locating
   * @return the collection of SDRF nodes, of the "upstreamNodeType" type,
   *         upstream of "currentNode"
   */
  public static Collection<? extends Node> findUpstreamNodes(
      Node currentNode, String upstreamNodeType) {
    Collection<Node> results = new HashSet<Node>();
    for (Node parent : currentNode.getParentNodes()) {
      if (parent.getNodeType().equals(upstreamNodeType)) {
        traverseUpstream(results, parent, upstreamNodeType);
      }
    }
    return results;
  }

  /**
   * Finds nodes downstream of the given node in the SDRF graph, searching for
   * nodes of a particular type.
   * <p/>
   * Note that if the SDRF graph is being actively constructed, there are no
   * guarantees that this method will return the exhaustive set, but will rather
   * return all nodes known to be downstream of the given node at the time of
   * the query.
   * <p/>
   * This method should never be used in classes that extend {@link
   * uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.SDRFHandler}s: by the contract
   * of the SDRF parser, the SDRF graph is constructed from left to right.
   * Therefore, if you query for downstream nodes of a node currently being
   * parsed, you are likely to get {@link NullPointerException}s or unexpected
   * results.
   * <p/>
   * Note that there is a potential flaw in this method, and that it is
   * therefore deprecated.  As the graph is resolved by the type of the
   * downstream node (given by the downstreamNodeType parameter), nodes that
   * have been created as UnresolvedPlaceholderNodes will not show up in this
   * method.  Most of the time, this is not a problem, but if your graph
   * inadvertently contains a cycle, unresolved placeholder nodes will not match
   * the type of downstream node requested and could therefore slip through
   * undetected.
   *
   * @param currentNode        the current node - this method will locate all
   *                           nodes downstream of this node
   * @param downstreamNodeType the type of nodes you are interested in locating
   * @return the collection of SDRF nodes, of the "downstreamNodeType" type,
   *         downstream of "currentNode"
   * @deprecated
   */
  @Deprecated
  public static <T extends SDRFNode> Collection<T> findDownstreamNodes(
      SDRFNode currentNode, Class<T> downstreamNodeType) {
    Collection<T> results = new HashSet<T>();
    for (Node n : currentNode.getChildNodes()) {
      if (n instanceof SDRFNode) {
        SDRFNode nextChild = (SDRFNode) n;
        traverseDownstream(results, nextChild, downstreamNodeType);
      }
    }
    return results;
  }

  /**
   * Finds nodes downstream of the given node in the SDRF graph, searching for
   * nodes of a particular type.
   * <p/>
   * Note that if the SDRF graph is being actively constructed, there are no
   * guarantees that this method will return the exhaustive set, but will rather
   * return all nodes known to be downstream of the given node at the time of
   * the query.
   * <p/>
   * This method should never be used in classes that extend {@link
   * uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.SDRFHandler}s: by the contract
   * of the SDRF parser, the SDRF graph is constructed from left to right.
   * Therefore, if you query for downstream nodes of a node currently being
   * parsed, you are likely to get {@link NullPointerException}s or unexpected
   * results.
   *
   * @param currentNode        the current node - this method will locate all
   *                           nodes downstream of this node
   * @param downstreamNodeType the type of nodes you are interested in locating
   * @return the collection of SDRF nodes, of the "downstreamNodeType" type,
   *         downstream of "currentNode"
   */
  public static Collection<? extends Node> findDownstreamNodes(
      Node currentNode, String downstreamNodeType) {
    Collection<Node> results = new HashSet<Node>();
    for (Node child : currentNode.getChildNodes()) {
      if (child.getNodeType().equals(downstreamNodeType)) {
        traverseDownstream(results, child, downstreamNodeType);
      }
    }

    return results;
  }

  /**
   * Determine whether the query node, "maybeUpstreamNode", is upstream of the
   * current node in the given SDRF graph. This only takes into account nodes
   * that are DIRECTLY upstream - if there is a node of the same type as the
   * maybeUpstreamNode between it and the currentNode, this method returns
   * false.
   *
   * @param currentNode       the target node: determine whether the query node
   *                          is upstream of this
   * @param maybeUpstreamNode the node to assess
   * @return true if "maybeUpstreamNode" is upstream of currentNode, false
   *         otherwise
   */
  public static boolean isDirectlyUpstream(SDRFNode currentNode,
                                           SDRFNode maybeUpstreamNode) {
    // check for nulls - remember the graph might just not yet be complete
    if (currentNode.getParentNodes().size() == 0) {
      return false;
    }

    // check parents of currentNode
    if (currentNode.getParentNodes().contains(maybeUpstreamNode)) {
      // does have the maybeDownstreamNode as a child
      return true;
    }
    else {
      // not an immediate child, so recurse to next depth
      // (but not through a node with matching type)
      for (Node node : currentNode.getParentNodes()) {
        if (node instanceof SDRFNode) {
          // only continue to iterate downstream if this node doesn't match the type
          if (!node.getNodeType().equals(maybeUpstreamNode.getNodeType())) {
            SDRFNode nextNode = (SDRFNode) node;
            // if we found the child here, return true
            if (isDirectlyUpstream(nextNode, maybeUpstreamNode)) {
              return true;
            }
          }
        }
      }
    }

    // if we got to here, no children of node match, so return false
    return false;
  }

  /**
   * Determine whether the query node, "maybeDownstreamNode", is downstream of
   * the current node in the given SDRF graph. This only takes into account
   * nodes that are DIRECTLY downstream - if there is a node of the same type as
   * the maybeDownstreamNode between it and the currentNode, this method returns
   * false.
   * <p/>
   * You should bear in mind when using this method DURING PARSING, that if you
   * attempt to check nodes downstream of one that has just been parsed you may
   * get null or erroneous results, as nodes are parsed from root to leaf of the
   * tree.  As such, just parsed nodes may have at most one level of children
   * and not the complete branch as children.
   *
   * @param currentNode         the target node: determine whether the query
   *                            node is downstream of this
   * @param maybeDownstreamNode the node to assess
   * @return true if "maybeDownstreamNode" is downstream of currentNode, false
   *         otherwise
   */
  public static boolean isDirectlyDownstream(SDRFNode currentNode,
                                             SDRFNode maybeDownstreamNode) {
    // check for nulls - remember the graph might just not yet be complete,
    // but nodes upstream of the one we want will DEFINATELY be present
    if (currentNode.getChildNodes().size() == 0) {
      return false;
    }

    // check children of currentNode
    if (currentNode.getChildNodes().contains(maybeDownstreamNode)) {
      // does have the maybeDownstreamNode as a child
      return true;
    }
    else {
      // not an immediate child, so recurse to next depth
      // (but not through a node with matching type)
      for (Node node : currentNode.getChildNodes()) {
        if (node instanceof SDRFNode) {
          // only continue to iterate downstream if this node doesn't match the type
          if (!node.getNodeType().equals(maybeDownstreamNode.getNodeType())) {
            SDRFNode nextNode = (SDRFNode) node;
            // if we found the child here, return true
            if (isDirectlyDownstream(nextNode, maybeDownstreamNode)) {
              return true;
            }
          }
        }
      }
    }

    // if we got to here, no children of node match, so return false
    return false;
  }

  @Deprecated
  private static <T extends SDRFNode> void traverseUpstream(
      Collection<T> results,
      SDRFNode currentNode,
      Class<T> upstreamNodeType) {
    if (currentNode.getClass().equals(upstreamNodeType)) {
      // if the current node matches the target type, add to the collection
      results.add((T) currentNode);
    }
    else {
      // check parents
      if (currentNode.getParentNodes().size() > 0) {
        // walk up the tree to each parent
        for (Node node : currentNode.getParentNodes()) {
          if (node instanceof SDRFNode) {
            SDRFNode nextNode = (SDRFNode) node;
            traverseUpstream(results, nextNode, upstreamNodeType);
          }
        }
      }
    }
  }

  private static void traverseUpstream(
      Collection<Node> results,
      Node currentNode,
      String upstreamNodeType) {
    if (currentNode.getNodeType().equals(upstreamNodeType)) {
      // if the current node matches the target type, add to the collection
      results.add(currentNode);
    }
    else {
      // check parents
      if (currentNode.getParentNodes().size() > 0) {
        // walk up the tree to each parent
        for (Node node : currentNode.getParentNodes()) {
          traverseUpstream(results, node, upstreamNodeType);
        }
      }
    }
  }

  @Deprecated
  private static <T extends SDRFNode> void traverseDownstream(
      Collection<T> results,
      SDRFNode currentNode,
      Class<T> downstreamNodeType) {
    if (currentNode.getClass().equals(downstreamNodeType)) {
      // if the current node matches the target type, add to the collection
      results.add((T) currentNode);
    }
    else {
      // check children
      if (currentNode.getChildNodes().size() > 0) {
        // walk down the tree to each child
        for (Node node : currentNode.getChildNodes()) {
          if (node instanceof SDRFNode) {
            SDRFNode nextNode = (SDRFNode) node;
            traverseDownstream(results, nextNode, downstreamNodeType);
          }
        }
      }
    }
  }

  private static void traverseDownstream(
      Collection<Node> results,
      Node currentNode,
      String downstreamNodeType) {
    if (currentNode.getNodeType().equals(downstreamNodeType)) {
      // if the current node matches the target type, add to the collection
      results.add(currentNode);
    }
    else {
      // check children
      if (currentNode.getChildNodes().size() > 0) {
        // walk down the tree to each child
        for (Node node : currentNode.getChildNodes()) {
          traverseDownstream(results, node, downstreamNodeType);
        }
      }
    }
  }
}
