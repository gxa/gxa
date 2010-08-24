package uk.ac.ebi.arrayexpress2.magetab.handler.visitor;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SDRFNode;
import uk.ac.ebi.arrayexpress2.magetab.handler.Handler;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.IDFHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.SDRFHandler;
import uk.ac.ebi.arrayexpress2.magetab.utils.MAGETABUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tracks the locations of parsed IDF and SDRF data from the original source
 * document.  This class then allows lookup of IDF header tags, returning a line
 * number, and SDRF Nodes (from the MAGETABInvestigation.SDRF graph model)
 * returning a set of {@link Point}s where this node may be found.
 * <p/>
 * Note that as yet, there is not support for SDRF attributes in this tracker
 * object.
 *
 * @author Tony Burdett
 * @date 23-Nov-2009
 */
public class LocationTracker implements HandlerVisitor {
  // map handlers to locations
  private Map<Handler, Point> handlerToPoint = new HashMap<Handler, Point>();

  // map tags to handlers
  private Map<String, IDFHandler> tagToIDFHandler =
      new HashMap<String, IDFHandler>();
  private Map<SDRFTuple, Set<SDRFHandler>> tupleToSDRFHandler =
      new HashMap<SDRFTuple, Set<SDRFHandler>>();

  /**
   * Store the location of a handler.  This maps the handler to the {@link
   * Point} in the original source document where the given handler has taken
   * it's data.  Note that the point, p, contains x and y co-ordinates, where
   * the x co-ordinate is the column number and the y co-ordinate the line
   * number in the source document.
   * <p/>
   * If handlers refer to an entire line or column, then the other co-ordinate
   * will default to zero: so, for example, IDF handlers should always pass a
   * location with a zero x co-ordinate and a y co-ordinate indicating the
   * line.
   *
   * @param handler the handler to store
   * @param p       the point in the source document, where x = column and y =
   *                line
   */
  public synchronized void trackLocation(Handler handler, Point p) {
    handlerToPoint.put(handler, p);
    handler.accept(this);
  }

  /**
   * Returns the line number representing the line in the original source
   * document where this IDF tag can be found.
   *
   * @param idfTag the tag to find the line number for
   * @return the line number where this IDF tag was located
   */
  public synchronized int getIDFLocations(String idfTag) {
    // strip the string to matching "tag" form
    MAGETABUtils.digestHeader(idfTag);

    if (tagToIDFHandler.containsKey(idfTag)) {
      IDFHandler h = tagToIDFHandler.get(idfTag);
      return handlerToPoint.get(h).y;
    }
    else {
      return -1;
    }
  }

  /**
   * Returns the set of points in an SDRF document where the supplied SDRFNodes
   * can be found.  Note that this returns a set, because a single SDRF node can
   * be referenced in several locations in one SDRF document (whenever there are
   * edges that either fork or merge).  This means a single node cannot be
   * tracked back to a single location in the graph.
   * <p/>
   * Locations are returned as points, where the x co-ordinate represents the
   * column and the y co-ordinate represents the row in which this node is
   * found.
   *
   * @param sdrfNode the SDRFNode to identify locations for
   * @return the point representing the location of this node
   */
  public synchronized Set<Point> getSDRFLocations(SDRFNode sdrfNode) {
    SDRFTuple tuple =
        new SDRFTuple(sdrfNode.getNodeType(), sdrfNode.getNodeName());

    Set<Point> results = new HashSet<Point>();
    if (tupleToSDRFHandler.containsKey(tuple)) {
      Set<SDRFHandler> handlers = tupleToSDRFHandler.get(tuple);
      for (SDRFHandler h : handlers) {
        results.add(handlerToPoint.get(h));
      }
    }

    return results;
  }

  public synchronized void visit(Handler handler) {
    if (handler instanceof IDFHandler) {
      visitIDFHandler((IDFHandler) handler);
    }

    if (handler instanceof SDRFHandler) {
      visitSDRFHandler((SDRFHandler) handler);
    }
  }

  private synchronized void visitIDFHandler(IDFHandler handler) {
    tagToIDFHandler.put(handler.handlesTag(), handler);
  }

  private synchronized void visitSDRFHandler(SDRFHandler handler) {
    String nodeType = handler.handlesTag();
    String nodeName = handler.handlesName();

    SDRFTuple tuple = new SDRFTuple(nodeType, nodeName);
    if (!tupleToSDRFHandler.containsKey(tuple)) {
      tupleToSDRFHandler.put(tuple, new HashSet<SDRFHandler>());
    }
    tupleToSDRFHandler.get(tuple).add(handler);
  }

  private class SDRFTuple {
    private final String nodeType;
    private final String nodeName;

    private SDRFTuple(String nodeType, String nodeName) {
      this.nodeType = MAGETABUtils.digestHeader(nodeType);
      this.nodeName = nodeName;
    }

    public String getNodeType() {
      return nodeType;
    }

    public String getNodeName() {
      return nodeName;
    }

    public int hashCode() {
      return 27 * (getNodeType().hashCode() + getNodeName().hashCode());
    }

    /**
     * SDRFNodes are determined to be equal() if they are of the same class,
     * have the same {@link #getNodeType()} value and the same {@link
     * #getNodeName()} value.
     *
     * @param obj the object to compare for equality
     * @return true if these objects are equal, false otherwise
     */
    public boolean equals(Object obj) {
      if (obj instanceof SDRFTuple) {
        SDRFTuple that = (SDRFTuple) obj;
        return that.getNodeType().equals(this.getNodeType()) &&
            that.getNodeName().equals(this.getNodeName());
      }
      else {
        return false;
      }
    }
  }
}
