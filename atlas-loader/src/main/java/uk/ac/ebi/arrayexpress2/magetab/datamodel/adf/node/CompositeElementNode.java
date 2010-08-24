package uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Models "Composite Element" nodes in the ADF graph, according to the MAGE-TAB
 * specification.  Composite elements can have composite element database
 * entries and comments attached.
 *
 * @author Tony Burdett
 * @date 10-Feb-2010
 */
public class CompositeElementNode extends AbstractADFNode {
  public Map<String, List<String>> compositeElementDatabaseEntries =
      new HashMap<String, List<String>>();
  public Map<String, String> comments = new HashMap<String, String>();

  public String[] headers() {
    return new String[0];
  }

  public String[] values() {
    return new String[0];
  }
}
