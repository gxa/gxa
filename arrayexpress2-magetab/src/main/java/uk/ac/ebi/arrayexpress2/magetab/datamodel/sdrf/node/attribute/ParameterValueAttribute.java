package uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AbstractSDRFNode;

import java.util.*;

/**
 * A parameter value attribute node in the graph.  Parameter values are
 * annotations on specific named nodes, and can be applied along protocol ref
 * edges.  Parameter values can have sub-attributes type, unit and a set of
 * comment.
 *
 * @author Tony Burdett
 * @date 26-Jan-2009
 */
public class ParameterValueAttribute extends AbstractSDRFNode {
  public String type;
  public UnitAttribute unit;
  public Map<String, String> comments = new HashMap<String, String>();

  public String[] headers() {
    List<String> headersList = new ArrayList<String>();
    headersList.add("Parameter Value[" + type + "]");
    if (unit != null) {
      Collections.addAll(headersList, unit.headers());
    }
    for (String commentType : comments.keySet()) {
      headersList.add("Comment[" + commentType + "]");
    }
    String[] result = new String[headersList.size()];
    return headersList.toArray(result);
  }

  public String[] values() {
    List<String> valuesList = new ArrayList<String>();
    valuesList.add(getNodeName());
    if (unit != null) {
      Collections.addAll(valuesList, unit.values());
    }
    for (String commentType : comments.keySet()) {
      valuesList.add(comments.get(commentType));
    }
    String[] result = new String[valuesList.size()];
    return valuesList.toArray(result);
  }
}
