package uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.CharacteristicsAttribute;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.MaterialTypeAttribute;

import java.util.*;

/**
 * A sample node in the graph.  Sample nodes are top level nodes and can have
 * associated characteristics, material types, descriptions, and sets of
 * comments.
 *
 * @author Tony Burdett
 * @date 22-Jan-2009
 */
public class SampleNode extends AbstractSDRFNode {
  public List<CharacteristicsAttribute> characteristics =
      new ArrayList<CharacteristicsAttribute>();
  public MaterialTypeAttribute materialType;
  public String description;
  public Map<String, String> comments = new HashMap<String, String>();

  public String[] headers() {
    List<String> headersList = new ArrayList<String>();
    headersList.add("Sample Name");
    for (CharacteristicsAttribute c : characteristics) {
      Collections.addAll(headersList, c.headers());
    }
    if (materialType != null) {
      Collections.addAll(headersList, materialType.headers());
    }
    if (description != null) {
      headersList.add("Description");
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
    for (CharacteristicsAttribute c : characteristics) {
      Collections.addAll(valuesList, c.values());
    }
    if (materialType != null) {
      Collections.addAll(valuesList, materialType.values());
    }
    if (description != null) {
      valuesList.add(description);
    }
    for (String commentType : comments.keySet()) {
      valuesList.add(comments.get(commentType));
    }
    String[] result = new String[valuesList.size()];
    return valuesList.toArray(result);
  }
}
