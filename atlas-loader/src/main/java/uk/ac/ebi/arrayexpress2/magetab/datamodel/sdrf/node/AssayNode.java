package uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.FactorValueAttribute;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.TechnologyTypeAttribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An Assay Node, in the SDRF graph.  Assay nodes are specializations of
 * HybridizationNodes that have an additional technology type.
 *
 * @author Tony Burdett
 * @date 30-Apr-2009
 */
public class AssayNode extends HybridizationNode {
  public TechnologyTypeAttribute technologyType;

  public String[] headers() {
    List<String> headersList = new ArrayList<String>();
    headersList.add("Assay Name");
    if (technologyType != null) {
      Collections.addAll(headersList, technologyType.headers());
    }
    for (ArrayDesignNode ad : arrayDesigns) {
      Collections.addAll(headersList, ad.headers());
    }
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
    if (technologyType != null) {
      Collections.addAll(valuesList, technologyType.values());
    }
    for (ArrayDesignNode ad : arrayDesigns) {
      Collections.addAll(valuesList, ad.values());
    }
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
}
