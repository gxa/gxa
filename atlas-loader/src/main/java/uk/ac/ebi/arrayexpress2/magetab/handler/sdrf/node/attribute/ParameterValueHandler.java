package uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.attribute;

import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.ProtocolApplicationNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.ParameterValueAttribute;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.UnitAttribute;
import uk.ac.ebi.arrayexpress2.magetab.exception.IllegalLineLengthException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.exception.UnmatchedTagException;
import uk.ac.ebi.arrayexpress2.magetab.lang.Status;

/**
 * Handles parameter value attributes in the SDRF graph.
 * <p/>
 * Tag: Parameter Value[]<br/> Allowed child attributes: Unit, Comment, Term
 * Source Ref, Term Accession Number
 *
 * @author Tony Burdett
 * @date 22-Jan-2009
 */
public class ParameterValueHandler extends AbstractSDRFAttributeHandler {
  public ParameterValueHandler() {
    setTag("parametervalue");
  }

  public String handlesTag() {
    return "parametervalue[]";
  }

  public boolean canHandle(String tag) {
    return tag.startsWith(getTag());
  }

  public void read() throws ParseException {
    if (!headers[0].startsWith(getTag())) {
      String message =
          "Tag is wrong for this handler - " + getClass().getSimpleName() +
              " accepts '*" + getTag() + "' but got '" + headers[0] + "'";

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

      throw new ObjectConversionException(error, false, message);
    }
    else {
      if (!headers[0].startsWith(getTag())) {
        String message =
            "Tag is wrong for this handler - " + getClass().getSimpleName() +
                " accepts '" + getTag() + "' but got '" + headers[0] + "'";

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
  }

  public void validate() throws ObjectConversionException {
    if (headers.length < 1) {
      String message =
          "Nothing to validate! This handler has no data";

      ErrorItem error =
          ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
              .generateErrorItem(
                  message,
                  ErrorCode.SDRF_FIELD_PRESENT_BUT_NO_DATA,
                  this.getClass());

      throw new ObjectConversionException(error, false, message);
    }
    else {
      if (!headers[0].startsWith(getTag())) {
        String message =
            "Tag is wrong for this handler - " + getClass().getSimpleName() +
                " accepts '" + getTag() + "' but got '" + headers[0] + "'";

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
                headers.length + " found: " + values.length;

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
      if (headers[i].startsWith("unit")) {
        // ok
      }
      else if (headers[i].startsWith("comment")) {
        // ok
      }
      else if (headers[i].equals("termsourceref")) {
        // ok
      }
      else if (headers[i].equals("termaccessionnumber")) {
        // ok
      }
      else if (headers[i].equals("")) {
        // skip the case where the header is an empty string
      }
      else {
        // got to something we don't recognise, so this is the end
        return i - 1;
      }
      i++;
    }

    // iterated over every column, so must have reached the end
    return values.length - 1;
  }

  public void readValues() throws UnmatchedTagException {
    // find the SourceNode to modify
    ParameterValueAttribute parameterValue;

    if (headers[0].startsWith(tag)) {

      if (values[0] != null && !values[0].equals("")) {
        // first row, so make a new attribute node
        parameterValue = new ParameterValueAttribute();

        String type =
            headers[0].substring(headers[0].lastIndexOf("[") + 1,
                                 headers[0].lastIndexOf("]"));
        parameterValue.setNodeType(headers[0]);
        parameterValue.setNodeName(values[0]);
        parameterValue.type = type;
        addNextNodeForCompilation(parameterValue);

        // now do the rest
        boolean emptyHeaderSkipped = false;
        for (int i = 1; i < values.length;) {
          if (headers[i].startsWith("unit")) {
            String unit_type =
                headers[i].substring(headers[i].lastIndexOf("[") + 1,
                                     headers[i].lastIndexOf("]"));
            if (values[i] != null && !values[i].equals("")) {
              UnitAttribute unit = new UnitAttribute();
              unit.setNodeName(values[i]);
              unit.type = unit_type;

              // now check possible termsourceref of units
              if (headers.length > i + 1) {
                i++;
                if (headers[i].equals("termsourceref")) {
                  unit.termSourceREF = values[i];
                }
                else if (headers[i].equals("")) {
                  // skip the case where the header is an empty string
                  emptyHeaderSkipped = true;
                }
              }

              // and set the unit
              parameterValue.unit = unit;
            }
          }
          else if (headers[i].startsWith("comment")) {
            String comment_type =
                headers[i].substring(headers[i].lastIndexOf("[") + 1,
                                     headers[i].lastIndexOf("]"));
            parameterValue.comments.put(comment_type, values[i]);
          }
          else if (headers[i].equals("")) {
            // skip the case where the header is an empty string
            emptyHeaderSkipped = true;
          }
          else {
            // got to something we don't recognise, so this is the end

            // first, update parentNode
            synchronized (parentNode) {
              if (parentNode instanceof ProtocolApplicationNode) {
                ProtocolApplicationNode protocolApplication =
                    (ProtocolApplicationNode) parentNode;
                protocolApplication.parameterValues.add(parameterValue);
              }
              else {
                String message =
                    "ParameterValue can be applied to Protocol Application " +
                        "nodes, but the parent here was a " +
                        parentNode.getClass().getName();

                ErrorItem error =
                    ErrorItemFactory
                        .getErrorItemFactory(getClass().getClassLoader())
                        .generateErrorItem(
                            message,
                            ErrorCode.BAD_SDRF_ORDERING,
                            this.getClass());

                throw new UnmatchedTagException(error, false, message);
              }
            }

            if (emptyHeaderSkipped) {
              String message =
                  "One or more columns with empty headers were detected " +
                      "and skipped";

              ErrorItem error =
                  ErrorItemFactory
                      .getErrorItemFactory(getClass().getClassLoader())
                      .generateErrorItem(
                          message,
                          ErrorCode.UNKNOWN_SDRF_HEADING,
                          this.getClass());

              throw new UnmatchedTagException(error, false, message);
            }
            else {
              return;
            }
          }
          i++;
        }

        // iterated over every column, so must have reached the end
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
}