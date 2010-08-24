package uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An image node in the graph.  Image nodes are top level nodes and can have
 * associated sets of comments.
 *
 * @author Tony Burdett
 * @date 22-Jan-2009
 */
public class ImageNode extends AbstractSDRFNode {
  public Map<String, String> comments = new HashMap<String, String>();

  public String[] headers() {
    List<String> headersList = new ArrayList<String>();
    headersList.add("Image Name");
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
