package uk.ac.ebi.arrayexpress2.magetab.datamodel.graph;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * An abstract top-level implementation of a Node in the MAGE-TAB graph.  This
 * takes care of getting and setting node types and names, and the child nodes.
 *
 * @author Tony Burdett
 * @date 10-Feb-2010
 */
public class AbstractNode implements Node {
  private volatile String nodeType;
  private volatile String nodeName;

  private final Set<Node> childNodes = new HashSet<Node>();
  private final Set<Node> parentNodes = new HashSet<Node>();

  private Log log = LogFactory.getLog(getClass());

  public synchronized void setNodeType(String nodeType) {
    this.nodeType = nodeType;
  }

  public synchronized String getNodeType() {
    return nodeType;
  }

  public synchronized void setNodeName(String nodeName) {
    if (nodeName == null) {
      throw new NullPointerException(
          "Node name cannot be null, an ID is required for graph resolution");
    }
    this.nodeName = nodeName;
  }

  public synchronized String getNodeName() {
    return nodeName;
  }

  public synchronized void addChildNode(Node childNode) {
    if (childNodes.contains(childNode)) {
      updateChildNode(childNode);
    }
    else {
      childNodes.add(childNode);
    }
  }

  public synchronized void addChildNode(String tag, String value) {
    Node childNode = new UnresolvedPlaceholderNode(tag, value);
    synchronized (childNode) {
      if (childNodes.contains(childNode)) {
        // do nothing, this would at best be a downgrade
      }
      else {
        addChildNode(childNode);
      }
    }
  }

  public synchronized Set<Node> getChildNodes() {
    return childNodes;
  }

  public synchronized boolean updateChildNode(Node nodeToUpdate) {
    synchronized (nodeToUpdate) {
      if (childNodes.contains(nodeToUpdate)) {
        for (Node n : childNodes) {
          synchronized (n) {
            if (n.equals(nodeToUpdate) &&
                nodeToUpdate.getClass().equals(getClass())) {
              log.debug(
                  "Possible cycle alert- child has same type as parent! " +
                      "Node " + nodeToUpdate.getNodeName() + " (" +
                      nodeToUpdate.getNodeType() + ") was " +
                      n.getClass().getSimpleName() + ", now is " +
                      nodeToUpdate.getClass().getSimpleName());
            }
          }
        }
        childNodes.remove(nodeToUpdate);
        childNodes.add(nodeToUpdate);
        return true;
      }
      else {
        return false;
      }
    }
  }

  public synchronized void addParentNode(Node parentNode) {
    if (parentNodes.contains(parentNode)) {
      updateParentNode(parentNode);
    }
    else {
      parentNodes.add(parentNode);
    }
  }

  public synchronized void addParentNode(String tag, String value) {
    Node parentNode = new UnresolvedPlaceholderNode(tag, value);
    if (parentNodes.contains(parentNode)) {
      // do nothing, this would at best be a downgrade
    }
    else {
      parentNodes.add(parentNode);
    }
  }

  public synchronized Set<Node> getParentNodes() {
    return parentNodes;
  }

  public synchronized boolean updateParentNode(Node nodeToUpdate) {
    if (parentNodes.contains(nodeToUpdate)) {
      parentNodes.remove(nodeToUpdate);
      parentNodes.add(nodeToUpdate);
      return true;
    }
    else {
      return false;
    }
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(getNodeName());
    return sb.toString();
  }

  public int hashCode() {
    int typesHash = this.getNodeType().hashCode();
    int namesHash = this.getNodeName().hashCode();

    return 27 * (typesHash + namesHash);
  }

  /**
   * Nodes are determined to be equal() if they are of the same class, have the
   * same {@link #getNodeType()} value and the same {@link #getNodeName()}
   * value.
   *
   * @param obj the object to compare for equality
   * @return true if these objects are equal, false otherwise
   */
  public boolean equals(Object obj) {
    if (obj instanceof Node) {
      Node that = (Node) obj;
      boolean typesEqual = that.getNodeType().equals(getNodeType());
      boolean namesEqual = that.getNodeName().equals(getNodeName());

      return typesEqual && namesEqual;
    }
    else {
      return false;
    }
  }
}
