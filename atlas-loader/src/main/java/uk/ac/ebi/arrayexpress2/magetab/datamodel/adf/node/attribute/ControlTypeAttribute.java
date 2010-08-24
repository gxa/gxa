package uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node.attribute;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node.AbstractADFNode;

/**
 * Models control type attributes in the MAGE-TAB ADF specification.  Control
 * type attributes can have a type (enclosed by square brackets), a term source
 * reference, and a term source accession.
 *
 * @author Tony Burdett
 * @date 10-Feb-2010
 */
public class ControlTypeAttribute extends AbstractADFNode {
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
