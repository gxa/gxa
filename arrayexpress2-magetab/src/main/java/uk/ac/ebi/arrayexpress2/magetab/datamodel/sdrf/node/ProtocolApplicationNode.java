package uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.ParameterValueAttribute;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.PerformerAttribute;

import java.util.*;

/**
 * A protocol "node" in the graph.  Protocols are special types of nodes, which
 * are described in the spec as "annotations on an edge" - effectively this
 * makes them an annotation/attribute node on the parent.  They are consequently
 * dealt with the same as other attribute nodes.
 * <p/>
 * To obtain Protocols, you should not use the SDRF {@link
 * uk.ac.ebi.arrayexpress2.magetab.datamodel.SDRF#lookupNodes(Class)} method,
 * instead look for the public field on the node to which this protocol is
 * associated.  So, for example..
 * <p/>
 * <pre><code>
 *   SourceNode src = ...
 *   List<ProtocolNode> protocols = src.protocols
 * </code></pre>
 *
 * @author Tony Burdett
 * @date 26-Jan-2009
 */
public class ProtocolApplicationNode extends AbstractSDRFNode {
  public String protocol;
  public String termSourceREF;
  public String termAccessionNumber;
  public List<ParameterValueAttribute> parameterValues =
      new ArrayList<ParameterValueAttribute>();
  public PerformerAttribute performer;
  public String date;
  public Map<String, String> comments = new HashMap<String, String>();

  public String[] headers() {
    List<String> headersList = new ArrayList<String>();
    headersList.add("Protocol REF");
    if (termAccessionNumber != null) {
      headersList.add("Term Accession Number");
    }
    if (termSourceREF != null) {
      headersList.add("Term Source REF");
    }
    for (ParameterValueAttribute pv : parameterValues) {
      Collections.addAll(headersList, pv.headers());
    }
    if (performer != null) {
      Collections.addAll(headersList, performer.headers());
    }
    if (date != null) {
      headersList.add("Date");
    }
    for (String commentType : comments.keySet()) {
      headersList.add("Comment[" + commentType + "]");
    }
    String[] result = new String[headersList.size()];
    return headersList.toArray(result);
  }

  public String[] values() {
    List<String> valuesList = new ArrayList<String>();
    valuesList.add(protocol);
    if (termAccessionNumber != null) {
      valuesList.add(termAccessionNumber);
    }
    if (termSourceREF != null) {
      valuesList.add(termSourceREF);
    }
    for (ParameterValueAttribute pv : parameterValues) {
      Collections.addAll(valuesList, pv.values());
    }
    if (performer != null) {
      Collections.addAll(valuesList, performer.values());
    }
    if (date != null) {
      valuesList.add(date);
    }
    for (String commentType : comments.keySet()) {
      valuesList.add(comments.get(commentType));
    }
    String[] result = new String[valuesList.size()];
    return valuesList.toArray(result);
  }
}
