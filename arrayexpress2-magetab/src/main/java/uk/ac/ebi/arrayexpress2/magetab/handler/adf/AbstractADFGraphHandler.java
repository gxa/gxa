package uk.ac.ebi.arrayexpress2.magetab.handler.adf;

import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABArrayDesign;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node.ADFNode;
import uk.ac.ebi.arrayexpress2.magetab.exception.IllegalLineLengthException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.exception.UnmatchedTagException;
import uk.ac.ebi.arrayexpress2.magetab.handler.AbstractReadWriteValidateHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.node.attribute.ADFAttributeHandler;
import uk.ac.ebi.arrayexpress2.magetab.lang.Status;
import uk.ac.ebi.arrayexpress2.magetab.utils.MAGETABUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * An abstract implementation of {@link uk.ac.ebi.arrayexpress2.magetab.handler.adf.ADFGraphHandler}.
 * This implementation governs all commong functionality such as setting of
 * data, general handling order, and progress tracking.  Classes that extend
 * this simply need to override {@link #readValues()} and {@link #writeValues()}
 * as appropriate.
 *
 * @author Tony Burdett
 * @date 15-Feb-2010
 */
public abstract class AbstractADFGraphHandler
    extends AbstractReadWriteValidateHandler
    implements ADFGraphHandler {
  /**
   * The MAGE-TAB object model so far
   */
  protected MAGETABArrayDesign arrayDesign;
  /**
   * Header data that is next to be read.  Indexed in the same order as data in
   * the file
   */
  protected String[] headers;
  /**
   * Values that are next to be read.  This string array is indexed to match the
   * headers.
   */
  protected String[] values;

  // node listing, which will be updated as we read
  protected List<ADFNode> nodesToCompile = new ArrayList<ADFNode>();

  public String handlesName() {
    if (values == null) {
      return "";
    }
    else {
      return values[0];
    }
  }

  public void setArrayDesign(MAGETABArrayDesign arrayDesign) {
    this.arrayDesign = arrayDesign;
  }

  public void setData(String[] headers, String[] values) {
    this.headers = headers;
    this.values = values;
  }

  public void handle() throws ParseException, ObjectConversionException {
    try {
      super.handle();

      // progress updater
      if (getTaskIndex() != -1) {
        arrayDesign.ADF.updateTaskList(getTaskIndex(), Status.PERSISTING);
        arrayDesign.ADF.increaseProgressBy(increase);
        getLog().trace("Handler " + this.toString() + " finished handling, " +
            "ADF progress now at " + arrayDesign.ADF.getProgress() + " " +
            "total at " + arrayDesign.getProgress());
      }
    }
    catch (ParseException e) {
      if (getTaskIndex() != -1) {
        if (e.isCriticalException()) {
          getLog().debug("Critical parse exception, handler " +
              getClass().getSimpleName() + " failed (" +
              e.getErrorItem().getComment() + ")");
          arrayDesign.ADF.updateTaskList(getTaskIndex(), Status.FAILED);
        }
        else {
          arrayDesign.ADF.updateTaskList(getTaskIndex(), Status.COMPLETE);
        }
      }
      throw e;
    }
    catch (ObjectConversionException e) {
      if (getTaskIndex() != -1) {
        if (e.isCriticalException()) {
          getLog().debug("Critical object conversion exception, handler " +
              getClass().getSimpleName() + " failed (" +
              e.getErrorItem().getComment() + ")");
          arrayDesign.ADF.updateTaskList(getTaskIndex(), Status.FAILED);
        }
        else {
          arrayDesign.ADF.updateTaskList(getTaskIndex(), Status.COMPLETE);
        }
      }
      throw e;
    }
  }

  public void read() throws ParseException {
    if (headers.length < 1) {
      String message =
          "There is no data to be read!";

      ErrorItem error =
          ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
              .generateErrorItem(
                  message,
                  ErrorCode.ADF_FIELD_PRESENT_BUT_NO_DATA,
                  this.getClass());

      throw new ParseException(error, false, message);
    }
    else {
      if (!headers[0].equals(tag)) {
        String message =
            "Tag is wrong for this handler - " + getClass().getSimpleName() +
                " accepts '" + tag + "' but got '" + headers[0] + "'";

        ErrorItem error =
            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                .generateErrorItem(
                    message,
                    ErrorCode.UNKNOWN_ADF_ROW_HEADING,
                    this.getClass());

        // tag must be wrong
        throw new UnmatchedTagException(error, false, message);
      }
      else if (headers.length < values.length) {
        String message =
            "Wrong number of elements on this line - allowed: " +
                (allowedLength < Integer.MAX_VALUE ? allowedLength
                    : "unlimited") + " found: " + values.length;

        ErrorItem error =
            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                .generateErrorItem(
                    message,
                    ErrorCode.MULTIPLE_COMMENTS_ADF,
                    this.getClass());

        throw new IllegalLineLengthException(error, false, message);
      }
      else {
        // everything ok, so update status
        if (getTaskIndex() != -1) {
          arrayDesign.ADF.updateTaskList(getTaskIndex(), Status.READING);
        }
        // read
        try {
          readValues();
        }
        catch (NullPointerException e) {
          // if the node name is null
          // we can ignore this, because each handler checks for this,
          // Optionally, handlers can assign their own inferred name if missing
        }
      }
    }
    getLog().trace("ADF Handler finished reading");
  }

  public void write() throws ObjectConversionException {
    if (headers.length < 1) {
      String message =
          "Nothing to validate! This handler has no data";

      ErrorItem error =
          ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
              .generateErrorItem(
                  message,
                  ErrorCode.ADF_FIELD_PRESENT_BUT_NO_DATA,
                  this.getClass());

      throw new ObjectConversionException(error, false, message);
    }
    else {
      if (!headers[0].equals(tag)) {
        String message =
            "Tag is wrong for this handler - " + getClass().getSimpleName() +
                " accepts '" + tag + "' but got '" + headers[0] + "'";

        ErrorItem error =
            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                .generateErrorItem(
                    message,
                    ErrorCode.UNKNOWN_ADF_ROW_HEADING,
                    this.getClass());

        // tag must be wrong
        throw new ObjectConversionException(error, true, message);
      }
      else if (headers.length < values.length) {
        String message =
            "Wrong number of elements on this line - allowed: " +
                (allowedLength < Integer.MAX_VALUE ? allowedLength
                    : "unlimited") + " found: " + values.length;

        ErrorItem error =
            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                .generateErrorItem(
                    message,
                    ErrorCode.MULTIPLE_COMMENTS_ADF,
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

      throw new ObjectConversionException(error, false, message);
    }
    else {
      if (!headers[0].equals(tag)) {
        String message =
            "Tag is wrong for this handler - " + getClass().getSimpleName() +
                " accepts '" + tag + "' but got '" + headers[0] + "'";

        ErrorItem error =
            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                .generateErrorItem(
                    message,
                    ErrorCode.UNKNOWN_ADF_ROW_HEADING,
                    this.getClass());

        // tag must be wrong
        throw new ObjectConversionException(error, false, message);
      }
      else if (headers.length < values.length) {
        String message =
            "Wrong number of elements on this line - allowed: " +
                (allowedLength < Integer.MAX_VALUE ? allowedLength
                    : "unlimited") + " found: " + values.length;

        ErrorItem error =
            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                .generateErrorItem(
                    message,
                    ErrorCode.MULTIPLE_COMMENTS_ADF,
                    this.getClass());

        throw new ObjectConversionException(error, false, message);
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

  /**
   * Performs the unit of work to read the data into the internal datamodel.
   * Override this method in implementations, so that you don't need to
   * implement status updating and checking code.  Implementations should
   * determine how much data should be read from the data queue - access the
   * data that still needs to be read by calling
   *
   * @throws ParseException if the header cannot be parsed or there was an error
   *                        reading the value
   */
  protected void readValues() throws ParseException {
    // default empty implementation
  }

  /**
   * Performs the unit of work to write data out from the internal datamodel.
   * Override this method in implementations, so that you don't need to
   * implement status updating and checking code.  Implementations should define
   * their own ways of determining what data should be written here
   *
   * @throws uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException
   *          if there is an error writing data out
   */
  protected void writeValues() throws ObjectConversionException {
    // default empty implementation
  }

  /**
   * Performs the unit of work to validate data from the internal datamodel.
   * Override this method in implementations, so that you don't need to
   * implement status updating and checking code.  Implementations should define
   * their own ways of determining what data should be written here
   *
   * @throws uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException
   *          if there is an error writing data out
   */
  protected void validateValues() throws ObjectConversionException {
    // default empty implementation
  }

  /**
   * Add a "node" object from the ADF data model into a cache.  This cache
   * contains the objects that are ready for compilation into AE2 data model
   * objects, and would be peeled off during the writeValues() operation,
   * turning them into full database objects.  Normally, you would use this
   * method to add every node created during readValue() operations into a
   * store.
   *
   * @param node the ADF node to store
   */
  protected synchronized void addNextNodeForCompilation(ADFNode node) {
    nodesToCompile.add(node);
  }

  /**
   * Get a "node" object from the ADF data model to a cache.  This cache
   * contains the objects that are ready for compilation into AE2 data model
   * objects, and this method peels off the next node in the list.  Usually you
   * would use this during the writeValues() operation.
   *
   * @return the next ADF node ready for conversion
   */
  protected synchronized ADFNode getNextNodeForCompilation() {
    if (nodesToCompile.size() > 0) {
      ADFNode node = nodesToCompile.get(0);
      nodesToCompile.remove(node);
      return node;
    }
    else {
      return null;
    }
  }

  protected int assessAttribute(ADFAttributeHandler handler, String[] headers,
                                String[] values, int startIndex) {
    String[] headerData =
        MAGETABUtils.extractRange(headers, startIndex, headers.length);
    String[] valuesData =
        MAGETABUtils.extractRange(values, startIndex, values.length);
    handler.setData(headerData, valuesData);
    return handler.assess();
  }

  protected int handleAttribute(ADFNode parentNode, ADFAttributeHandler handler,
                                String[] headers, String[] values,
                                int startIndex) throws ParseException {
    String[] headerData =
        MAGETABUtils.extractRange(headers, startIndex, headers.length);
    String[] valuesData =
        MAGETABUtils.extractRange(values, startIndex, values.length);
    // set params for the child handler
    handler.setParentNode(parentNode);
    handler.setData(headerData, valuesData);
    // inherit other params from this handler
    handler.setArrayDesign(arrayDesign);
    handler.setHandlerMode(getHandlerMode());
    handler.setTaskIndex(-1);
    int endIndex = handler.assess();
    try {
      handler.handle();
      return endIndex;
    }
    catch (ObjectConversionException e) {
      throw new ParseException(e.getErrorItem(), e.isCriticalException(),
                               "An error occurred whilst adding attributes " +
                                   "to ADFNodes in the datamodel", e);
    }
  }
}
