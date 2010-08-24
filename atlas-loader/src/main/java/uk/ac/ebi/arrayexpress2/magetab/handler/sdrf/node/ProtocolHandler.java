package uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node;

import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.graph.Node;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.ProtocolApplicationNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SDRFNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.ParameterValueAttribute;
import uk.ac.ebi.arrayexpress2.magetab.exception.IllegalLineLengthException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.exception.UnmatchedTagException;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.AbstractSDRFHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.attribute.ParameterValueHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.attribute.PerformerHandler;
import uk.ac.ebi.arrayexpress2.magetab.lang.Status;

/**
 * A handler that handles Protocol edges in the SDRF graph.  Protocols are not
 * actually true nodes in the SDRF spreadsheet - they are actually edges,
 * labelled with the name of the protocol used to generate the child node from
 * the parent.  This is represented in the data model as a Protocol Application
 * node, which is the "implicit" node that exists in the spreadsheet.  To
 * accomplish this, this handler needs to do extra work - it has to read
 * backwards to the parent node and forwards to the child (and any parameter
 * values that modify this protocol).  The unique name for the protocol
 * application node can then be generated from a combination of these
 * identifiers.
 * <p/>
 * Tag: Protocol Ref<br/> Allowed attributes: Parameter Value, Performer, Date,
 * Term Source Ref, Term Accession NUmber, Comment
 *
 * @author Tony Burdett
 * @date 01-Feb-2010
 */
public class ProtocolHandler extends AbstractSDRFHandler {
  protected int startIndex;

  public ProtocolHandler() {
    setTag("protocolref");
  }

  public boolean canHandle(String tag) {
    return tag.endsWith(getTag());
  }

  public void setStartIndex(int startIndex) {
    this.startIndex = startIndex;
  }

  public String handlesName() {
    if (nodesToCompile.isEmpty()) {
      return "";
    }
    else {
      return nodesToCompile.get(0).getNodeName();
    }
  }

  public void read() throws ParseException {
    if (!headers[startIndex].endsWith(getTag())) {
      String message =
          "Tag is wrong for this handler - " + getClass().getSimpleName() +
              " accepts '*" + getTag() + "' but got '" +
              headers[startIndex] + "'";

      ErrorItem error =
          ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
              .generateErrorItem(
                  message,
                  ErrorCode.UNKNOWN_SDRF_HEADING,
                  this.getClass());

      throw new UnmatchedTagException(error, false, message);
    }
    else if (headers.length < values.length) {
      String message =
          "Wrong number of elements on this line - expected: " + headers.length
              + " found: " + values.length;

      ErrorItem error =
          ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
              .generateErrorItem(
                  message,
                  ErrorCode.BAD_SDRF_ORDERING,
                  this.getClass());

      throw new IllegalLineLengthException(error, false, message);
    }
    else {
      // everything ok, so update status
      if (getTaskIndex() != -1) {
        investigation.SDRF.updateTaskList(getTaskIndex(), Status.READING);
      }
      // read
      readValues();
    }
    getLog().trace("SDRF Handler finished reading");
  }

  public void write() throws ObjectConversionException {
    if (headers.length < 1) {
      String message =
          "Nothing to convert! This handler has no data";

      ErrorItem error =
          ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
              .generateErrorItem(
                  message,
                  ErrorCode.SDRF_FIELD_PRESENT_BUT_NO_DATA,
                  this.getClass());

      throw new ObjectConversionException(error, true, message);
    }
    else {
      if (!headers[startIndex].endsWith(getTag())) {
        String message =
            "Tag is wrong for this handler - " + getClass().getSimpleName() +
                " accepts '" + getTag() + "' but got '" +
                headers[startIndex] + "'";

        ErrorItem error =
            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                .generateErrorItem(
                    message,
                    ErrorCode.UNKNOWN_SDRF_HEADING,
                    this.getClass());

        throw new ObjectConversionException(error, true, message);
      }
      else if (headers.length < values.length) {
        String message =
            "Wrong number of elements on this line - expected: " +
                headers.length
                + " found: " + values.length;

        ErrorItem error =
            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                .generateErrorItem(
                    message,
                    ErrorCode.BAD_SDRF_ORDERING,
                    this.getClass());

        throw new ObjectConversionException(error, true, message);
      }
      else {
        // everything ok, so update status
        if (getTaskIndex() != -1) {
          investigation.SDRF.updateTaskList(getTaskIndex(), Status.COMPILING);
        }
        // write
        writeValues();
      }
    }
    getLog().trace("SDRF Handler finished writing");
  }

  public void validate() throws ObjectConversionException {
    if (headers.length < 1) {
      String message =
          "Nothing to convert! This handler has no data";

      ErrorItem error =
          ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
              .generateErrorItem(
                  message,
                  ErrorCode.SDRF_FIELD_PRESENT_BUT_NO_DATA,
                  this.getClass());

      throw new ObjectConversionException(error, false, message);
    }
    else {
      if (!headers[startIndex].endsWith(getTag())) {
        String message =
            "Tag is wrong for this handler - " + getClass().getSimpleName() +
                " accepts '" + getTag() + "' but got '" +
                headers[startIndex] + "'";

        ErrorItem error =
            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                .generateErrorItem(
                    message,
                    ErrorCode.UNKNOWN_SDRF_HEADING,
                    this.getClass());

        throw new ObjectConversionException(error, true, message);
      }
      else if (headers.length < values.length) {
        String message =
            "Wrong number of elements on this line - expected: " +
                headers.length
                + " found: " + values.length;

        ErrorItem error =
            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                .generateErrorItem(
                    message,
                    ErrorCode.BAD_SDRF_ORDERING,
                    this.getClass());

        throw new ObjectConversionException(error, true, message);
      }
      else {
        // everything ok, so update status
        if (getTaskIndex() != -1) {
          investigation.SDRF.updateTaskList(getTaskIndex(), Status.VALIDATING);
        }
        // write
        validateValues();
      }
    }
    getLog().trace("SDRF Handler finished validating");
  }

  public int assess() {
    for (int i = 1; i < values.length;) {
      if (headers[i].equals("termsourceref")) {
        // ok
      }
      else if (headers[i].equals("termaccessionnumber")) {
        // ok
      }
      else if (headers[i].startsWith("parametervalue")) {
        // ok
        ParameterValueHandler handler = new ParameterValueHandler();
        i += assessAttribute(handler, headers, values, i);
      }
      else if (headers[i].equals("performer")) {
        // ok
        PerformerHandler handler = new PerformerHandler();
        i += assessAttribute(handler, headers, values, i);
      }
      else if (headers[i].equals("date")) {
        // ok
      }
      else if (headers[i].startsWith("comment")) {
        // ok
      }
      else if (headers[i].equals("")) {
        // skip the case where the header is an empty string
      }
      else {
        // got to something we don't recognise
        // this is either the end, or a non-handled column name
        return i;
      }
      i++;
    }

    // iterated over every column, so must have reached the end
    return values.length;
  }

  public void readValues() throws ParseException {
    // find the SourceNode to modify
    ProtocolApplicationNode protApp;

    if (headers[startIndex].endsWith(tag)) {
      // create a new node, we don't know anything about it yet
      protApp = new ProtocolApplicationNode();

      // check startindex - if the protocol is the last column, this is an error
      if (startIndex + 1 == values.length) {
        String message =
            "It appears that protocol '" + values[startIndex] + "' is not " +
                "being used to generate anything - there are no nodes to " +
                "the right of this Protocol REF";

        ErrorItem error =
            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                .generateErrorItem(
                    message,
                    ErrorCode.BAD_SDRF_ORDERING,
                    this.getClass());

        throw new ParseException(error, false, message);
      }

      // parse all the attributes
      boolean emptyHeaderSkipped = false;
      for (int i = startIndex + 1; i < values.length;) {
        if (headers[i].equals("termsourceref")) {
          protApp.termSourceREF = values[i];
        }
        else if (headers[i].equals("termaccessionnumber")) {
          protApp.termAccessionNumber = values[i];
        }
        else if (headers[i].startsWith("parametervalue")) {
          ParameterValueHandler handler = new ParameterValueHandler();
          i += handleAttribute(protApp, handler, headers, values,
                               i);
        }
        else if (headers[i].equals("performer")) {
          PerformerHandler handler = new PerformerHandler();
          i += handleAttribute(protApp, handler, headers, values,
                               i);
        }
        else if (headers[i].equals("date")) {
          protApp.date = values[i];
        }
        else if (headers[i].startsWith("comment")) {
          String type = headers[i].substring(headers[i].lastIndexOf("[") + 1,
                                             headers[i].lastIndexOf("]"));
          protApp.comments.put(type, values[i]);
        }
        else if (headers[i].equals("")) {
          // skip the case where the header is an empty string
          emptyHeaderSkipped = true;
        }
        else {
          // got to something we don't recognise
          // this is either the end, or a bad column name
          // check the name of the next node and put it in this one
          // but be wary of Factor Value nodes
          int j = i;
          while (j < headers.length &&
              (headers[j].startsWith("factorvalue") ||
                  headers[j].startsWith("unit") ||
                  headers[j].startsWith("termsourceref") ||
                  headers[j].startsWith("comment"))) {
            // loop over every factor value column
            j++;
          }
          i = j;

          // now we need to infer the node name and assess equality
          protApp = resolveNode(protApp);

          // if we failed to resolve (probably, missing value)
          // there is NO protocol application, so just exit
          if (protApp != null) {
            addNextNodeForCompilation(protApp);

            // update the child node
            updateChildNode(protApp, i);
          }

          break;
        }
        i++;
      }
      // iterated over every column, so must have reached the end
      // update node in SDRF
      if (protApp != null) {
        investigation.SDRF.updateNode(protApp);
      }

      // throw exception if we had to skip something
      if (emptyHeaderSkipped) {
        String message =
            "One or more columns with empty headers were detected " +
                "and skipped";

        ErrorItem error =
            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                .generateErrorItem(
                    message,
                    ErrorCode.UNKNOWN_SDRF_HEADING,
                    this.getClass());

        throw new UnmatchedTagException(error, false, message);
      }
    }
    else {
      String message =
          "This handler starts at tag: " + tag + ", not " + headers[0];

      ErrorItem error =
          ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
              .generateErrorItem(
                  message,
                  ErrorCode.UNKNOWN_SDRF_HEADING,
                  this.getClass());

      throw new UnmatchedTagException(error, false, message);
    }
  }

  private ProtocolApplicationNode resolveNode(ProtocolApplicationNode protApp)
      throws ParseException {
    ProtocolApplicationNode result;

    // if the protocol ref column is empty, just skip
    String protocolName;
    if (values[startIndex] == null || values[startIndex].equals("")) {
      return null;
    }
    else {
      protocolName = values[startIndex];
    }

    String nodeName = protocolName;
    for (ParameterValueAttribute pv : protApp.parameterValues) {
      nodeName = nodeName.concat("[").concat(pv.type).concat("=")
          .concat(pv.getNodeName()).concat("]");
    }

    // first values, so lookup or make a new protocol application
    String protocolApplicationName =
        readBackwardsToParent().concat(":").concat(nodeName);
    synchronized (investigation.SDRF) {
      // now attempt to retrieve existing node with the same name
      ProtocolApplicationNode retrievedNode =
          investigation.SDRF
              .lookupNode(protocolApplicationName,
                          ProtocolApplicationNode.class);
      if (retrievedNode == null) {
        // we've got a new protocol application, so store
        protApp.setNodeType(headers[startIndex]);
        protApp.setNodeName(protocolApplicationName);
        protApp.protocol = protocolName;
        investigation.SDRF.storeNode(protApp);
        result = protApp;
      }
      else {
        // compare for equality
        boolean equality = true;
        if (protApp.equals(retrievedNode)) {
          // check parameters, performer, protocol name, children
          for (ParameterValueAttribute pv : protApp.parameterValues) {
            if (retrievedNode.parameterValues.size() == 0 ||
                !retrievedNode.parameterValues.contains(pv)) {
              equality = false;
              break;
            }
          }

          if (!retrievedNode.performer.equals(protApp.performer)) {
            equality = false;
          }

          for (Node childNode : protApp.getChildNodes()) {
            if (retrievedNode.getChildNodes().size() == 0 ||
                !retrievedNode.getChildNodes().contains(childNode)) {
              equality = false;
              break;
            }
          }
        }

        if (!equality) {
          String message =
              "There are identical protocol applications with mismatched " +
                  "attributes present";

          ErrorItem error =
              ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                  .generateErrorItem(
                      message,
                      ErrorCode.MISMATCHED_PARAMETERS,
                      this.getClass());

          throw new ParseException(error, false, message);
        }
        else {
          result = retrievedNode;
        }
      }
    }

    return result;
  }

  private String readBackwardsToParent() throws ParseException {
    SDRFNode parentNode;
    String suffix = "";
    int i;
    for (i = startIndex - 1; i >= 0; i--) {
      if (headers[i].endsWith("protocolref")) {
        // if the protocol ref column is empty, fill in with "Unknown Protocol"
        if (values[i] == null || values[i].equals("")) {
          // add nothing to the suffix, just ignore empty protocols
        }
        else {
          suffix = ":".concat(values[i]).concat(suffix);
        }
      }
      else if (headers[i].startsWith("parametervalue")) {
        String parameterType =
            headers[i].substring(headers[i].lastIndexOf("[") + 1,
                                 headers[i].lastIndexOf("]"));
        if (values[i] == null || values[i].equals("")) {
          // add nothing to the suffix, just ignore empty params
        }
        else {
          suffix = "[".concat(parameterType).concat("=").concat(values[i])
              .concat("]").concat(suffix);
        }
      }

      String nodeName = values[i] + suffix;
      if (suffix.equals("")) {
        // we're still looking for this type
        parentNode = investigation.SDRF.lookupNode(
            nodeName, headers[i]);
      }
      else {
        // we're actually looking for the protocol
        parentNode = investigation.SDRF.lookupNode(
            nodeName, ProtocolApplicationNode.class);
      }

      if (parentNode != null) {
        return parentNode.getNodeName();
      }
    }

    // if we get to here, we found no parent node
    String message =
        "It appears that protocol '" + values[startIndex] + "' is being " +
            "applied to nothing - there are no nodes to the left of this " +
            "Protocol REF.  The resulting SDRF graph will not be valid.";

    ErrorItem error =
        ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
            .generateErrorItem(
                message,
                ErrorCode.BAD_SDRF_ORDERING,
                this.getClass());

    throw new ParseException(error, false, message);
  }
}
