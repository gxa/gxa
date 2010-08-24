package uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.attribute;

import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.HybridizationNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.ScanNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.FactorValueAttribute;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.UnitAttribute;
import uk.ac.ebi.arrayexpress2.magetab.exception.IllegalLineLengthException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.exception.UnmatchedTagException;
import uk.ac.ebi.arrayexpress2.magetab.lang.Status;

/**
 * Handles factor value attributes in the SDRF graph.  Factor Values are
 * attributes of data nodes (hybridization, assay, scan nodes) but in multi
 * channel experiments are tied to a single channel by the labelling dye used.
 * In this case, the sscanner channel is assessed and attached to the resulting
 * factor value attribute
 * <p/>
 * Tag: Factor Value[]<br/> Allowed child attributes: Unit, Term Source Ref,
 * Term Accession Number
 *
 * @author Tony Burdett
 * @date 22-Jan-2009
 */
public class FactorValueHandler extends AbstractSDRFAttributeHandler {
  private int scannerChannel;

  public FactorValueHandler(int scannerChannel) {
    setTag("factorvalue");
    this.scannerChannel = scannerChannel;
  }

  public String handlesTag() {
    return "factorvalue[]()";
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
    FactorValueAttribute fv;

    if (headers[0].startsWith(tag)) {
      if (values[0] != null) {
        // first row, so make a new attribute node
        fv = new FactorValueAttribute();

        String type =
            headers[0].substring(headers[0].lastIndexOf("[") + 1,
                                 headers[0].lastIndexOf("]"));
        fv.setNodeType(headers[0]);
        fv.setNodeName(values[0]);
        fv.type = type;
        fv.scannerChannel = scannerChannel;
        addNextNodeForCompilation(fv);
        getLog().debug(
            "Factor value: " + fv.getNodeName() + ", derived from channel " +
                scannerChannel);

        if (headers[0].contains("(")) {
          fv.optionalType =
              headers[0].substring(headers[0].lastIndexOf("("),
                                   headers[0].lastIndexOf(")"));
        }

        // now do the rest
        boolean emptyHeaderSkipped = false;
        for (int i = 1; i < values.length; i++) {
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
              fv.unit = unit;
            }
          }
          else if (headers[i].equals("termsourceref")) {
            if (fv.termSourceREF == null) {
              fv.termSourceREF = values[i];
            }
            else {
              // previous column must have been term source ref too -
              // this is probably the term source ref for the parent node
              break;
            }
          }
          else if (headers[i].equals("termaccessionnumber")) {
            if (fv.termAccessionNumber == null) {
              fv.termAccessionNumber = values[i];
            }
            else {
              // previous column must have been term source accession too -
              // this is probably the term source accession for the parent node
              break;
            }
          }
          else if (headers[i].equals("")) {
            // skip the case where the header is an empty string
            emptyHeaderSkipped = true;
          }
          else {
            // got to something we don't recognise, so this is the end
            break;
          }
        }

        // iterated over every column, so must have reached the end

        // first, update parentNode
        synchronized (parentNode) {
          if (parentNode instanceof HybridizationNode) {
            HybridizationNode hyb = (HybridizationNode) parentNode;
            boolean contains = false;
            synchronized (hyb) {
              for (FactorValueAttribute factorValue : hyb.factorValues) {
                if (factorValue.equals(fv)) {
                  contains = true;
                  break;
                }
              }

              if (!contains) {
                getLog().trace(
                    "Adding " + fv.getNodeName() + " [factor value, type = " +
                        fv.getNodeType() + " to " + hyb.getNodeName());
                hyb.factorValues.add(fv);
              }
            }
          }
          else if (parentNode instanceof ScanNode) {
            ScanNode scan = (ScanNode) parentNode;
            boolean contains = false;
            synchronized (scan) {
              for (FactorValueAttribute factorValue : scan.factorValues) {
                if (factorValue.equals(fv)) {
                  contains = true;
                  break;
                }
              }

              if (!contains) {
                getLog().trace(
                    "Adding " + fv.getNodeName() + " [factor value, type = " +
                        fv.getNodeType() + " to " + scan.getNodeName());
                scan.factorValues.add(fv);
              }
            }
          }
          else {
            String message =
                "FactorValue can be applied to Hybridization nodes, but the " +
                    "parent here was a " + parentNode.getClass().getName();

            ErrorItem error =
                ErrorItemFactory
                    .getErrorItemFactory(getClass().getClassLoader())
                    .generateErrorItem(
                        message,
                        ErrorCode.BAD_SDRF_ORDERING,
                        this.getClass());

            throw new UnmatchedTagException(error, false, message);
          }

          // and exit
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
