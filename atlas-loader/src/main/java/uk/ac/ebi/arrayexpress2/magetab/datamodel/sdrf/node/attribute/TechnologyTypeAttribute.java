package uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AbstractSDRFNode;

import java.util.ArrayList;
import java.util.List;

/**
 * A technology type attribute node in the SDRF graph.  The "technology type" is
 * an attribute attached to assay nodes in the MAGE-TAB 1.1 specification.
 * Therefore, attaching this attribute to a node in a 1.0 specification document
 * should be judged as a validation fail.  Technology type atributes can have
 * term source refs and accession numbers.
 *
 * @author Tony Burdett
 * @date 30-Apr-2009
 */
public class TechnologyTypeAttribute extends AbstractSDRFNode {
  public String termSourceREF;
  public String termAccessionNumber;

  public String[] headers() {
    List<String> headersList = new ArrayList<String>();
    headersList.add("Technology Type");
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
