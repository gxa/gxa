package uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AbstractSDRFNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A provider attribute node in the graph.  Providers are annotations on
 * specific named nodes, and can be applied to Source, nodes.  Characteristics
 * can have a sub-attribute of a set of comments.
 *
 * @author Tony Burdett
 * @date 26-Jan-2009
 */
public class ProviderAttribute extends AbstractSDRFNode {
  public Map<String, String> comments = new HashMap<String, String>();

  public String[] headers() {
    List<String> headersList = new ArrayList<String>();
    headersList.add("Provider");
    for (String commentType : comments.keySet()) {
      headersList.add("Comment[" + commentType + "]");
    }
    String[] result = new String[headersList.size()];
    return headersList.toArray(result);
  }

  public String[] values() {
    List<String> headersList = new ArrayList<String>();
    headersList.add(getNodeName());
    for (String commentType : comments.keySet()) {
      headersList.add(comments.get(commentType));
    }
    String[] result = new String[headersList.size()];
    return headersList.toArray(result);
  }
}
