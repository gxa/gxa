package uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node.attribute;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node.AbstractADFNode;

/**
 * Models a reporter group attribute in the MAGE-TAB SDRF specification.
 * Reporter groups can be typed (with square brackets) and have a term source
 * reference and accession.
 *
 * @author Tony Burdett
 * @date 10-Feb-2010
 */
public class ReporterGroupAttribute extends AbstractADFNode {
  public String type;
  public String termSourceREF;
  public String termAccessionNumber;

  public String[] headers() {
    return new String[0];
  }

  public String[] values() {
    return new String[0];
  }
}
