package uk.ac.ebi.arrayexpress2.magetab.handler.adf.node.attribute;

import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node.ReporterNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node.attribute.ReporterGroupAttribute;
import uk.ac.ebi.arrayexpress2.magetab.exception.IllegalLineLengthException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.exception.UnmatchedTagException;
import uk.ac.ebi.arrayexpress2.magetab.lang.Status;
import uk.ac.ebi.gxa.utils.StringUtil;

/**
 * Handles reporter group attributes in the ADF graph part.
 * <p/>
 * Tag: Reporter Group <br/>Allowed Child Attributes: Term Source REF, Term
 * Accession Number
 *
 * @author Tony Burdett
 * @date 17-Feb-2010
 */
public class ReporterGroupHandler extends AbstractADFAttributeHandler {
  public ReporterGroupHandler() {
    setTag("reportergroup");
  }

  public String handlesTag() {
    return "reportergroup[]";
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
                  ErrorCode.UNKNOWN_ADF_COLUMN_HEADING,
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
                  ErrorCode.UNKNOWN_ADF_COLUMN_HEADING,
                  this.getClass());

      throw new IllegalLineLengthException(error, false, message);
    }
    else {
      // everything ok, so update status
      if (getTaskIndex() != -1) {
        arrayDesign.ADF.updateTaskList(getTaskIndex(), Status.READING);
      }
      // read
      readValues();
    }
    getLog().trace("ADF Handler finished reading");
  }

  public void write() throws ObjectConversionException {
    if (headers.length < 1) {
      String message =
          "Nothing to convert! This handler has no data";

      ErrorItem error =
          ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
              .generateErrorItem(
                  message,
                  ErrorCode.ADF_FIELD_PRESENT_BUT_NO_DATA,
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
                    ErrorCode.UNKNOWN_ADF_COLUMN_HEADING,
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
                    ErrorCode.UNKNOWN_ADF_COLUMN_HEADING,
                    this.getClass());

        throw new ObjectConversionException(error, true, message);
      }
      else {
        // everything ok, so update status
        if (getTaskIndex() != -1) {
          arrayDesign.ADF.updateTaskList(getTaskIndex(), Status.COMPILING);
        }
        // write
        writeValues();
      }
    }
    getLog().trace("ADF Handler finished writing");
  }

  public void validate() throws ObjectConversionException {
    if (headers.length < 1) {
      String message =
          "Nothing to validate! This handler has no data";

      ErrorItem error =
          ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
              .generateErrorItem(
                  message,
                  ErrorCode.ADF_FIELD_PRESENT_BUT_NO_DATA,
                  this.getClass());

      throw new ObjectConversionException(error, true, message);
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
                    ErrorCode.UNKNOWN_ADF_COLUMN_HEADING,
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
                    ErrorCode.UNKNOWN_ADF_COLUMN_HEADING,
                    this.getClass());

        throw new ObjectConversionException(error, true, message);
      }
      else {
        // everything ok, so update status
        if (getTaskIndex() != -1) {
          arrayDesign.ADF.updateTaskList(getTaskIndex(), Status.VALIDATING);
        }
        // write
        validateValues();
      }
    }
    getLog().trace("ADF Handler finished validating");
  }

  public int assess() {
    for (int i = 1; i < values.length;) {
      if (headers[i].equals("termsourceref")) {
        // ok
      }
      else if (headers[i].equals("termaccessionnumber")) {
        // ok
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
    ReporterGroupAttribute reporterGroup;

    if (headers[0].startsWith(tag)) {
      // make sure attribute is not empty
        if (!StringUtil.isEmpty(values[0])) {
        // first row, so make a new attribute node
        reporterGroup = new ReporterGroupAttribute();

        String type =
            headers[0].substring(headers[0].lastIndexOf("[") + 1,
                                 headers[0].lastIndexOf("]"));
        reporterGroup.setNodeType(headers[0]);
        reporterGroup.setNodeName(values[0]);
        reporterGroup.type = type;
        addNextNodeForCompilation(reporterGroup);

        // now do the rest
        for (int i = 1; i < values.length;) {
          if (headers[i].equals("termsourceref")) {
            reporterGroup.termSourceREF = values[i];
          }
          else if (headers[i].equals("termaccessionnumber")) {
            reporterGroup.termAccessionNumber = values[i];
          }
          else {
            // got to something we don't recognise, so this is the end

            // first, update parentNode
            synchronized (parentNode) {
              if (parentNode instanceof ReporterNode) {
                ReporterNode reporterNode = (ReporterNode) parentNode;
                boolean seenBefore = false;
                for (ReporterGroupAttribute c : reporterNode.reporterGroupAttributes) {
                  if (c.type.equals(reporterGroup.type) &&
                      c.getNodeName().equals(reporterGroup.getNodeName())) {
                    // these characteristics have been seen before
                    seenBefore = true;
                  }
                }

                // add if we haven't seen it before
                if (!seenBefore) {
                  reporterNode.reporterGroupAttributes.add(reporterGroup);
                }
              }
              else {
                String message =
                    "Reporter Groups can only be applied to Reporter nodes, " +
                        "but the parent here was a " +
                        parentNode.getClass().getName();

                ErrorItem error =
                    ErrorItemFactory
                        .getErrorItemFactory(getClass().getClassLoader())
                        .generateErrorItem(
                            message,
                            ErrorCode.BAD_REPORTER,
                            this.getClass());

                throw new UnmatchedTagException(error, false, message);
              }
            }
            return;
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
                  ErrorCode.UNKNOWN_ADF_COLUMN_HEADING,
                  this.getClass());

      throw new UnmatchedTagException(error, false, message);
    }
  }
}
