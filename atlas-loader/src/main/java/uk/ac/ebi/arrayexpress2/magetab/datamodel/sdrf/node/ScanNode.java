package uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.FactorValueAttribute;

import java.util.*;

/**
 * A scan node in the graph.  Scan nodes are top level nodes and can have
 * associated sets of comments.
 *
 * @author Tony Burdett
 * @date 22-Jan-2009
 */
public class ScanNode extends AbstractSDRFNode {
  public List<FactorValueAttribute> factorValues =
      new ArrayList<FactorValueAttribute>();
  public Map<String, String> comments = new HashMap<String, String>();

  public String[] headers() {
    List<String> headersList = new ArrayList<String>();
    headersList.add("Scan Name");
    for (String commentType : comments.keySet()) {
      headersList.add("Comment[" + commentType + "]");
    }
    for (FactorValueAttribute fv : factorValues) {
      // we don't need to duplicate headers from all channels - just use channel one to capture the header
      if (fv.scannerChannel == 1) {
        Collections.addAll(headersList, fv.headers());
      }
    }
    String[] result = new String[headersList.size()];
    return headersList.toArray(result);
  }

  /**
   * Request values by factor value attribute scanner channel.  The returned
   * String array will include only Cy3 or Cy5, and single channel, factor
   * values depending on those requested.
   *
   * @param channel the channel to return values for
   * @return the values, formatted by scanner channel
   */
  public String[] values(int channel) {
    List<String> valuesList = new ArrayList<String>();
    valuesList.add(getNodeName());
    for (String commentType : comments.keySet()) {
      valuesList.add(comments.get(commentType));
    }
    for (FactorValueAttribute fv : factorValues) {
      // return only factor values that match the specified channel
      if (fv.scannerChannel == channel) {
        Collections.addAll(valuesList, fv.values());
      }
    }
    String[] result = new String[valuesList.size()];
    return valuesList.toArray(result);
  }

  /**
   * Delegates to {@link #values(int)}, using the channel labeled as channel
   * one.  In single channel experiments, this will be all values, but in two or
   * three channel experiments using this form of the method will only return
   * channel one values, resulting in the loss of data.  You should not use this
   * form of the method in multichannel experiments.
   *
   * @return the default range of values, depending on experiment type
   */
  public String[] values() {
    return values(1);
  }
}
