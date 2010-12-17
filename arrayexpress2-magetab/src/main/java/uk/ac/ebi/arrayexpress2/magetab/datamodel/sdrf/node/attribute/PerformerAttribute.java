package uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AbstractSDRFNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A performer attribute node in the graph.  Performers are annotations on
 * specific named nodes, and can be applied along protocol edges.  Performers
 * can have sub-attributes type, unit and termSourceREF.
 *
 * @author Tony Burdett
 * @date 26-Jan-2009
 */
public class PerformerAttribute extends AbstractSDRFNode {
  public Map<String, String> comments = new HashMap<String, String>();

  public String[] headers() {
    List<String> headersList = new ArrayList<String>();
    headersList.add("Performer");
    for (String commentType : comments.keySet()) {
      headersList.add("Comment[" + commentType + "]");
    }
    String[] result = new String[headersList.size()];
    return headersList.toArray(result);
  }

  public String[] values() {
    List<String> valuesList = new ArrayList<String>();
    valuesList.add(getNodeName());
    for (String commentType : comments.keySet()) {
      valuesList.add(comments.get(commentType));
    }
    String[] result = new String[valuesList.size()];
    return valuesList.toArray(result);
  }
}
