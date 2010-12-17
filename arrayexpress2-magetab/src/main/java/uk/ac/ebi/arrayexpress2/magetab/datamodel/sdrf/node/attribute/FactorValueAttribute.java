package uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AbstractSDRFNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A factor value attribute node in the graph.  Factor Values are annotations on
 * specific named nodes, and can be applied to hybridization nodes. Factor
 * Values can have sub-attributes type, optionalType, unit and termSourceREF.
 * <p/>
 * Factor Values are unique amongst SDRF attributes, in that they have an
 * implicit channel they are keyed on.  In the SDRF document, this channel can
 * be inferred by the row in which the attribute occurs.  The same hybridization
 * can therefore have different factor values in different rows of the
 * spreadsheet, depending on the label specified in that row.  At first glance,
 * this would seem to break the graph representation, but actually this is just
 * a convenient way of indexing the factor value by channel.
 *
 * @author Tony Burdett
 * @date 26-Jan-2009
 */
public class FactorValueAttribute extends AbstractSDRFNode {
  public int scannerChannel;

  public String type;
  public String optionalType;
  public UnitAttribute unit;
  public String termSourceREF;
  public String termAccessionNumber;

  public String[] headers() {
    List<String> headersList = new ArrayList<String>();
    if (optionalType == null) {
      headersList.add("Factor Value[" + type + "]");
    }
    else {
      headersList.add("Factor Value[" + type + "](" + optionalType + ")");
    }
    if (unit != null) {
      Collections.addAll(headersList, unit.headers());
    }
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
    if (unit != null) {
      Collections.addAll(valuesList, unit.values());
    }
    if (termAccessionNumber != null) {
      valuesList.add(termAccessionNumber);
    }
    if (termSourceREF != null) {
      valuesList.add(termSourceREF);
    }
    String[] result = new String[valuesList.size()];
    return valuesList.toArray(result);
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    FactorValueAttribute that = (FactorValueAttribute) o;
    if (optionalType != null ? !optionalType.equals(that.optionalType)
        : that.optionalType != null) {
      return false;
    }
    if (scannerChannel != that.scannerChannel) {
      return false;
    }
    if (type != null ? !type.equals(that.type) : that.type != null) {
      return false;
    }

    return true;
  }

  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + scannerChannel;
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + (optionalType != null ? optionalType.hashCode() : 0);
    return result;
  }
}
