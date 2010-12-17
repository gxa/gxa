package uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AbstractSDRFNode;

import java.util.ArrayList;
import java.util.List;

/**
 * A unit attribute node in the graph.  Units are annotations on specific named
 * nodes, and can be applied to characteristics or factor values attribute
 * nodes.  Units can have sub-attributes type and termSourceREF.
 *
 * @author Tony Burdett
 * @date 26-Jan-2009
 */
public class UnitAttribute extends AbstractSDRFNode {
  public String type;
  public String termSourceREF;
  public String termAccessionNumber;

  public String[] headers() {
    List<String> headersList = new ArrayList<String>();
    headersList.add("Unit[" + type + "]");
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
