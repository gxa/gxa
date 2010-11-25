package uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.attribute;

import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.ExtractNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.LabeledExtractNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SampleNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SourceNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.CharacteristicsAttribute;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.UnitAttribute;
import uk.ac.ebi.arrayexpress2.magetab.exception.IllegalLineLengthException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.exception.UnmatchedTagException;
import uk.ac.ebi.arrayexpress2.magetab.lang.Status;
import uk.ac.ebi.gxa.utils.StringUtil;

/**
 * Handles characteristic attributes in the SDRF graph.
 * <p/>
 * Tag: Characteristics[]<br/> Allowed child attributes: Unit, Term Source Ref,
 * Term Accession Number
 *
 * @author Tony Burdett
 * @date 22-Jan-2009
 */
public class CharacteristicsHandler extends AbstractSDRFAttributeHandler {
  public CharacteristicsHandler() {
    setTag("characteristics");
  }

  public String handlesTag() {
    return "characteristics[]";
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
    getLog().trace("SDRF Handler finished writing");
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
    CharacteristicsAttribute characteristics;

    if (headers[0].startsWith(tag)) {
      // make sure attribute is not empty
        if (!StringUtil.isEmpty(values[0])) {
        // first row, so make a new attribute node
        characteristics = new CharacteristicsAttribute();

        String type =
            headers[0].substring(headers[0].lastIndexOf("[") + 1,
                                 headers[0].lastIndexOf("]"));
        characteristics.setNodeType(headers[0]);
        characteristics.setNodeName(values[0]);
        characteristics.type = type;
        addNextNodeForCompilation(characteristics);

        // now do the rest
        boolean emptyHeaderSkipped = false;
        for (int i = 1; i < values.length;) {
          if (headers[i].startsWith("unit")) {
            String unit_type =
                headers[i].substring(headers[i].lastIndexOf("[") + 1,
                                     headers[i].lastIndexOf("]"));
              if (!StringUtil.isEmpty(values[i])) {
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
                }
              }

              // and set the unit
              characteristics.unit = unit;
            }
          }
          else if (headers[i].equals("termsourceref")) {
            characteristics.termSourceREF = values[i];
          }
          else if (headers[i].equals("termaccessionnumber")) {
            characteristics.termAccessionNumber = values[i];
          }
          else if (headers[i].equals("")) {
            // skip the case where the header is an empty string
            emptyHeaderSkipped = true;
          }
          else {
            // got to something we don't recognise, so this is the end

            // first, update parentNode
            synchronized (parentNode) {
              if (parentNode instanceof SourceNode) {
                SourceNode sourceNode = (SourceNode) parentNode;
                boolean seenBefore = false;
                for (CharacteristicsAttribute c : sourceNode.characteristics) {
                  if (c.type.equals(characteristics.type) &&
                      c.getNodeName().equals(characteristics.getNodeName())) {
                    // these characteristics have been seen before
                    seenBefore = true;
                  }
                }

                // add if we haven't seen it before
                if (!seenBefore) {
                  sourceNode.characteristics.add(characteristics);
                }
              }
              else if (parentNode instanceof SampleNode) {
                SampleNode sampleNode = (SampleNode) parentNode;
                boolean seenBefore = false;
                for (CharacteristicsAttribute c : sampleNode.characteristics) {
                  if (c.type.equals(characteristics.type) &&
                      c.getNodeName().equals(characteristics.getNodeName())) {
                    // these characteristics have been seen before
                    seenBefore = true;
                  }
                }

                // add if we haven't seen it before
                if (!seenBefore) {
                  sampleNode.characteristics.add(characteristics);
                }
              }
              else if (parentNode instanceof ExtractNode) {
                ExtractNode extract = (ExtractNode) parentNode;
                boolean seenBefore = false;
                for (CharacteristicsAttribute c : extract.characteristics) {
                  if (c.type.equals(characteristics.type) &&
                      c.getNodeName().equals(characteristics.getNodeName())) {
                    // these characteristics have been seen before
                    seenBefore = true;
                  }
                }

                // add if we haven't seen it before
                if (!seenBefore) {
                  extract.characteristics.add(characteristics);
                }
              }
              else if (parentNode instanceof LabeledExtractNode) {
                LabeledExtractNode labeledExtract =
                    (LabeledExtractNode) parentNode;
                boolean seenBefore = false;
                for (CharacteristicsAttribute c : labeledExtract.characteristics) {
                  if (c.type.equals(characteristics.type) &&
                      c.getNodeName().equals(characteristics.getNodeName())) {
                    // these characteristics have been seen before
                    seenBefore = true;
                  }
                }

                // add if we haven't seen it before
                if (!seenBefore) {
                  labeledExtract.characteristics.add(characteristics);
                }
              }
              else {
                String message =
                    "Characteristics can be applied to Source, Sample, " +
                        "Extract or LabeledExtract nodes, but the parent " +
                        "here was a " + parentNode.getClass().getName();

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