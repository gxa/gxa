package uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An array design node in the graph.  Array Data nodes are top level nodes and
 * can have associated termSourceREFs and comments.  Array Data nodes are
 * slightly special, as the parser is entirely agnostic as to whether they are
 * suffixed by "File" or "REF"
 *
 * @author Tony Burdett
 * @date 26-Jan-2009
 */
public class ArrayDesignNode extends AbstractSDRFNode {
  public String termSourceREF;
  public String termAccessionNumber;
  public Map<String, String> comments = new HashMap<String, String>();

  public String[] headers() {
    List<String> headersList = new ArrayList<String>();
    headersList.add("Array Design REF");
    if (termSourceREF != null) {
      headersList.add("Term Source REF");
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
    if (termSourceREF != null) {
      valuesList.add(termSourceREF);
    }
    for (String commentType : comments.keySet()) {
      valuesList.add(comments.get(commentType));
    }
    String[] result = new String[valuesList.size()];
    return valuesList.toArray(result);
  }
}
