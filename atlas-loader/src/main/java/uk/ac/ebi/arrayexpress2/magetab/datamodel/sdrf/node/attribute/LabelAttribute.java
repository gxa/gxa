package uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AbstractSDRFNode;

import java.util.ArrayList;
import java.util.List;

/**
 * A label attribute node in the graph.  Labels are annotations on specific
 * named nodes, and can be applied to LabeledExtract nodes.  Labels can have the
 * sub-attribute termSourceREF.
 *
 * @author Tony Burdett
 * @date 26-Jan-2009
 */
public class LabelAttribute extends AbstractSDRFNode {
  public String termSourceREF;
  public String termAccessionNumber;

  public String[] headers() {
    List<String> headersList = new ArrayList<String>();
    headersList.add("Label");
    if (termAccessionNumber != null) {
      headersList.add("Term Accession Number");
    }
    if (termSourceREF != null) {
      headersList.add("Term Source REF");
    }
    String[] result = new String[headersList.size()];
    return headersList.toArray(result);
  }

  public String[] values() {
    List<String> valuesList = new ArrayList<String>();
    valuesList.add(getNodeName());
    if (termAccessionNumber != null) {
      valuesList.add(termAccessionNumber);
    }
    if (termSourceREF != null) {
      valuesList.add(termSourceREF);
    }
    String[] result = new String[valuesList.size()];
    return valuesList.toArray(result);
  }
}
