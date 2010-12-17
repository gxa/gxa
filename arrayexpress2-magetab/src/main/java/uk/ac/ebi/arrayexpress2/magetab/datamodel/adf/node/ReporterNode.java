package uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node.attribute.ControlTypeAttribute;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node.attribute.ReporterGroupAttribute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Models "Reporter" nodes in the ADF graph, according to the MAGE-TAB
 * specification.  Reporters can have sequences, database entries, and reporter
 * group and control type attributes attached.  They can also have attached
 * comments.
 *
 * @author Tony Burdett
 * @date 10-Feb-2010
 */
public class ReporterNode extends AbstractADFNode {
  public String reporterSequence;
  public Map<String, List<String>> reporterDatabaseEntries =
      new HashMap<String, List<String>>();
  public List<ReporterGroupAttribute> reporterGroupAttributes =
      new ArrayList<ReporterGroupAttribute>();
  public List<ControlTypeAttribute> controlTypeAttributes =
      new ArrayList<ControlTypeAttribute>();
  public Map<String, String> comments = new HashMap<String, String>();

  public String[] headers() {
    return new String[0];
  }

  public String[] values() {
    return new String[0];
  }
}
