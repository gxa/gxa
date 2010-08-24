package uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node;

import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AssayNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.HybridizationNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SDRFNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.ScanNode;
import uk.ac.ebi.arrayexpress2.magetab.exception.*;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.AbstractSDRFHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.attribute.FactorValueHandler;
import uk.ac.ebi.arrayexpress2.magetab.lang.Status;
import uk.ac.ebi.arrayexpress2.magetab.utils.MAGETABUtils;

/**
 * A special type of 'dummy' handler for handling factor values.  As factor
 * value attributes can "float" around in the SDRF spreadsheet, they are not
 * always explicitly attached to their parent node.  As such, they are handled
 * by this handler as if they were nodes.  However, once their location has been
 * determined, this handler tracks back to the parent "data" type node and
 * invokes an attribute handler, passing the parent node in.  Essentially, this
 * acts as a wrapper to accommodate for the floating nature of factor value
 * attributes.
 * <p/>
 * Tag: Factor Value <br/>Allowed Attributes (none here - see {@link
 * uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.attribute.FactorValueHandler}
 *
 * @author Tony Burdett
 * @date 22-Jan-2009
 */
public class FactorValueNodeHandler extends AbstractSDRFHandler {
  protected int startIndex;

  public FactorValueNodeHandler() {
    setTag("factorvalue");
  }

  public String handlesTag() {
    return "factorvalue[]";
  }

  public boolean canHandle(String tag) {
    return tag.startsWith(getTag());
  }

  public void setStartIndex(int startIndex) {
    this.startIndex = startIndex;
  }

  public void read() throws ParseException {
    if (!headers[startIndex].startsWith(tag)) {
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
      if (!headers[startIndex].startsWith(getTag())) {
        String message =
            "Tag is wrong for this handler - " + getClass().getSimpleName() +
                " accepts '" + getTag() + "' but got '" + headers[startIndex] +
                "'";

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
      if (!headers[startIndex].startsWith(getTag())) {
        String message =
            "Tag is wrong for this handler - " + getClass().getSimpleName() +
                " accepts '" + getTag() + "' but got '" + headers[startIndex] +
                "'";

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
    getLog().debug("SDRF Handler finished validating");
  }

  public int assess() {
    // delegate to the attribute handler
    String[] headerData =
        MAGETABUtils.extractRange(headers, startIndex, headers.length);
    String[] valuesData =
        MAGETABUtils.extractRange(values, startIndex, values.length);

    getLog().debug("Assessing number of column that can be handled after "
        + headers[startIndex] + "...");

    // we can just use a single channel constructor arg,
    // as it won't affect assessment and we discard this handler anyway
    FactorValueHandler assessHandler = new FactorValueHandler(1);
    int index = assessAttribute(
        assessHandler, headerData, valuesData, startIndex);

    getLog().debug("FactorValueHandler can read " + index + " columns after " +
        headers[startIndex]);

    // read forward 1, because attributes return last read value but nodes
    // expect the index to read next
    return index + 1;
  }

  public void readValues() throws ParseException {
    // this actually doesn't do anything,
    // it is a special type of node to fix a peculiarity of the MAGE-TAB spec -
    // it simply has to retrieve the last hyb,
    // and set the factor value attribute on it
    getLog().debug("Counting back from " + startIndex +
        " to find prior hyb for " + headers[startIndex]);

    // the node to which this factor value will be anchored
    SDRFNode anchorNode = null;
    // the header and value data columsn to be read by the factor value handler
    String[] headerData = null;
    String[] valuesData = null;

    // the scanner channel specified by the LabeledExtract
    // Assumes single channel if absent.
    int scannerChannel = 1;

    for (int i = startIndex; i > 0; i--) {
      getLog().debug("Backwards read.  Next header is: " + headers[i]);
      if (headers[i].equals("hybridizationname")) {
        // find our anchor node
        anchorNode =
            investigation.SDRF.lookupNode(values[i],
                                          HybridizationNode.class);

        // now we've done the lookup, delegate to attribute handler
        headerData =
            MAGETABUtils.extractRange(headers, startIndex, headers.length);
        valuesData =
            MAGETABUtils.extractRange(values, startIndex, values.length);
      }
      else if (headers[i].equals("assayname")) {
        // find our anchor node
        anchorNode =
            investigation.SDRF.lookupNode(values[i],
                                          AssayNode.class);

        // now we've done the lookup, delegate to attribute handler
        headerData =
            MAGETABUtils.extractRange(headers, startIndex, headers.length);
        valuesData =
            MAGETABUtils.extractRange(values, startIndex, values.length);
      }
      else if (headers[i].equals("scanname")) {
        // find our anchor node
        anchorNode =
            investigation.SDRF.lookupNode(values[i],
                                          ScanNode.class);

        // now we've done the lookup, delegate to attribute handler
        headerData =
            MAGETABUtils.extractRange(headers, startIndex, headers.length);
        valuesData =
            MAGETABUtils.extractRange(values, startIndex, values.length);
      }
      else if (headers[i].equals("label")) {
        // this is our channel key, lookup against SDRF
        scannerChannel = investigation.SDRF.getChannelNumber(values[i]);
        break;
      }
      else if (headers[i].equals("")) {
        // skip the case where the header is an empty string
      }
      else {
        getLog().trace(
            "Read backwards shows next column is " + headers[i]);
      }
    }

    // we've read backwards as far as we need now

    // check we acquired the node to anchor this factor value to
    if (anchorNode != null) {
      // add for compilation
      addNextNodeForCompilation(anchorNode);

      getLog().debug("Read " + headers[startIndex] + " " + values[startIndex] +
          ", attaching to " + anchorNode.getNodeType() + " " +
          anchorNode.getNodeName());

      // now we've done the lookup, delegate to attribute handler
      FactorValueHandler handler = new FactorValueHandler(scannerChannel);
      handleAttribute(anchorNode, handler, headerData, valuesData, 0);
    }
    else {
      // if we get to here, we found no Hybridization/Assay/Scan Name column
      getLog().debug("Failed to find a Data column " +
          "(Hybridization/Assay/Scan Name) in the headers available.");
      StringBuffer sb = new StringBuffer();
      for (String h : headers) {
        sb.append(h).append("\t");
      }
      getLog().debug("Headers available are " + sb.toString());

      String message =
          "There is a Factor Value at row " + startIndex + ", but there are " +
              "no prior Data columns (Hybridization/Assay/Scan Name) to " +
              "associate this Factor Value with";

      ErrorItem error =
          ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
              .generateErrorItem(
                  message,
                  ErrorCode.UNLABELED_HYB,
                  this.getClass());

      throw new InconsistentEntryCountException(error, true, message);
    }
  }

  protected void writeValues() throws ObjectConversionException {
    // this actually doesn't write anything,
    // because it's just a placeholder for an attribute that does a hyb lookup
    // when writing, the hyb will create the object
  }

  /**
   * Returns the hybridization node to which this factor value node is
   * associated.  This method overrides the normal default behaviour; it returns
   * hybridization nodes.  If you try to retrieve factor values from the
   * hybridization object in the <code>writeValues()</code> method of the {@link
   * uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.HybridizationHandler},
   * you are not guaranteed to get a comprehensive set.  This is due to a
   * peculiarity of the SDRF spec: factor values are allowed to "float" anywhere
   * downstream of the hybridization to which they are associated.
   * <p/>
   * As such, extra factor values may crop up during parsing, and when they do
   * this handler will be triggered.  As they are read by this handler, the
   * upstream hybridization node is recovered and the newly read factor values
   * attached.  In this handler the next node for compilation is this upstream
   * hybridization node that is retrieved.  This provides a convenient way of
   * recovering the pre-existing hybridization nodes and fetching the updated
   * factor values.
   *
   * @return the upstream hybridization node that factor values get attached to
   */
  protected SDRFNode getNextNodeForCompilation() {
    return super.getNextNodeForCompilation();
  }
}