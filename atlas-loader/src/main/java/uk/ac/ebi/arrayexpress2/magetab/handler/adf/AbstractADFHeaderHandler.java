package uk.ac.ebi.arrayexpress2.magetab.handler.adf;

import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABArrayDesign;
import uk.ac.ebi.arrayexpress2.magetab.exception.IllegalLineLengthException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.exception.UnmatchedTagException;
import uk.ac.ebi.arrayexpress2.magetab.handler.AbstractReadWriteValidateHandler;
import uk.ac.ebi.arrayexpress2.magetab.lang.Status;
import uk.ac.ebi.arrayexpress2.magetab.utils.MAGETABUtils;
import uk.ac.ebi.gxa.utils.StringUtil;

/**
 * An abstract implementation of {@link ADFHeaderHandler}. This implementation
 * governs all commong functionality such as setting of data, general handling
 * order, and progress tracking.  Classes that extend this simply need to
 * override {@link #readValue(String)} and {@link #writeValues()} as
 * appropriate.
 *
 * @author Tony Burdett
 * @date 15-Feb-2010
 */
public abstract class AbstractADFHeaderHandler
    extends AbstractReadWriteValidateHandler implements ADFHeaderHandler {
  // investigation and line
  protected MAGETABArrayDesign arrayDesign;
  protected String line;

  public void setArrayDesign(MAGETABArrayDesign arrayDesign) {
    this.arrayDesign = arrayDesign;
  }

  public synchronized void setData(String line) {
    getLog().trace("Updating handler [" + toString() + "] with data: " + line);
    if (StringUtil.isEmpty(line)) {
      this.line = line;
    }
    else {
      this.line = line.trim();
    }
  }

  /**
   * Invoke the current read handler.  This will read data assigned to this
   * handler into the MAGETABInvestigation object, where the exact data read is
   * determined by the concrete implementations
   * <p/>
   * Before completing handling, the status of the current task index is updated
   * to Status.COMPLETE if everything succeeded, or Status.FAILED otherwise
   *
   * @throws uk.ac.ebi.arrayexpress2.magetab.exception.ParseException
   *          if the handler failed to read data from it's source
   * @throws uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException
   *          if the handler failed to write out appropriate objects
   */
  public void handle() throws ParseException, ObjectConversionException {
    try {
      super.handle();

      // progress updater
      arrayDesign.ADF.updateTaskList(getTaskIndex(), Status.COMPLETE);
      arrayDesign.ADF.increaseProgressBy(increase);
      getLog().trace("Handler " + this.toString() + " finished handling, " +
          "ADF progress now at " + arrayDesign.ADF.getProgress() + " " +
          "total at " + arrayDesign.getProgress());
    }
    catch (ParseException e) {
      getLog().error(
          "Parse Exception occurred for " +
              this.getClass().getSimpleName() + ", index " + getTaskIndex() +
              ": " + e.getMessage());
      if (e.isCriticalException()) {
        getLog().debug("Critical parse exception, handler " +
            getClass().getSimpleName() + " failed (" +
            e.getErrorItem().getComment() + ")");
        arrayDesign.ADF.updateTaskList(getTaskIndex(), Status.FAILED);
      }
      else {
        arrayDesign.ADF.updateTaskList(getTaskIndex(), Status.COMPLETE);
      }
      throw e;
    }
    catch (ObjectConversionException e) {
      getLog().error(
          "Object Conversion Exception occurred for " +
              this.getClass().getSimpleName() + ", index " + getTaskIndex() +
              ": " + e.getMessage());
      if (e.isCriticalException()) {
        getLog().debug("Critical object conversion exception, handler " +
            getClass().getSimpleName() + " failed (" +
            e.getErrorItem().getComment() + ")");
        arrayDesign.ADF.updateTaskList(getTaskIndex(), Status.FAILED);
      }
      else {
        arrayDesign.ADF.updateTaskList(getTaskIndex(), Status.COMPLETE);
      }
      throw e;
    }
  }

  /**
   * Cause the handler to read from it's source into some internal buffer.  In
   * this implementation, the overriding method handles status updating and any
   * other standard operations and delegates the specific unit of work to {@link
   * #readValue(String)}, which should be overridden by implementing classes.
   *
   * @throws uk.ac.ebi.arrayexpress2.magetab.exception.ParseException
   *          if parsing failed
   */
  public synchronized void read() throws ParseException {
//    String[] tokens = line.split("\t", -1);
    String[] tokens = MAGETABUtils.splitLine(line, false);
    int numTokens = tokens.length - 1;

    String header = MAGETABUtils.digestHeader(tokens[0]);
    String[] values = MAGETABUtils.extractRange(tokens, 1, tokens.length);

    if (!header.equals(tag)) {
      String message =
          "Tag is wrong for this handler - " + getClass().getSimpleName() +
              " accepts '" + tag + "' but got '" + header + "'";

      ErrorItem error =
          ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
              .generateErrorItem(
                  message,
                  ErrorCode.UNKNOWN_ADF_COLUMN_HEADING,
                  this.getClass());

      // tag must be wrong
      throw new UnmatchedTagException(error, false, message);
    }
    else if (numTokens > allowedLength) {
      String message =
          "Wrong number of elements on this line - allowed: " +
              (allowedLength < Integer.MAX_VALUE ? allowedLength
                  : "unlimited") + " found: " + numTokens;

      ErrorItem error =
          ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
              .generateErrorItem(
                  message,
                  23,
                  this.getClass());

      throw new IllegalLineLengthException(error, false, message);
    }
    else {
      // everything ok, so update status
      if (getTaskIndex() != -1) {
        arrayDesign.ADF.updateTaskList(getTaskIndex(), Status.READING);
      }
      // read
      if (values.length > 0) {
        // if allowed length is fixed, only read this far
        int i = 0;
        for (String value : values) {
          if (i < allowedLength) {
            readValue(value);
            i++;
          }
          else {
            String message =
                "Cardinality breach - " + tokens[0] + " fields can contain " +
                    allowedLength + " values only";

            ErrorItem error =
                ErrorItemFactory
                    .getErrorItemFactory(getClass().getClassLoader())
                    .generateErrorItem(
                        message,
                        23,
                        this.getClass());

            throw new ParseException(error, false, message);
          }
        }
      }
      else {
        // no tokens, so do standard empty value
        readEmptyValue();
      }
    }
    getLog().trace("ADF Handler finished reading");
  }

  /**
   * Causes the handler to write objects out based on data in the internal
   * buffer.  This data can usually be accessed directly from a cache or by
   * calling a method on the specific implementing class. In this
   * implementation, the overriding method handles status updating and any other
   * standard operations and delegates the specific unit of work to {@link
   * #writeValues()}, which should be overridden by implementing classes.
   *
   * @throws uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException
   *          if data in the buffer could not be converted
   */
  public synchronized void write() throws ObjectConversionException {
//    String[] tokens = line.split("\t", -1);
    try {
      String[] tokens = MAGETABUtils.splitLine(line, false);
      int numTokens = tokens.length - 1;

      String header = MAGETABUtils.digestHeader(tokens[0]);
      if (!header.equals(tag)) {
        String message =
            "Tag is wrong for this handler - " + getClass().getSimpleName() +
                " accepts '" + tag + "' but got '" + header + "'";

        ErrorItem error =
            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                .generateErrorItem(
                    message,
                    ErrorCode.UNKNOWN_ADF_COLUMN_HEADING,
                    this.getClass());

        // tag must be wrong
        throw new ObjectConversionException(error, true, message);
      }
      else if (numTokens > allowedLength) {
        String message =
            "Wrong number of elements on this line - allowed: " +
                (allowedLength < Integer.MAX_VALUE ? allowedLength
                    : "unlimited") + " found: " + numTokens;

        ErrorItem error =
            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                .generateErrorItem(
                    message,
                    23,
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
      getLog().trace("ADF Handler finished writing");
    }
    catch (ParseException e) {
      throw new ObjectConversionException(e.getErrorItem(),
                                          e.isCriticalException(), e);
    }
  }

  /**
   * Causes the handler to validate objects. In this implementation, the
   * overriding method handles status updating and any other standard operations
   * and delegates the specific unit of work to {@link #validateValues()}, which
   * should be overridden by implementing classes.
   *
   * @throws ObjectConversionException if there is a problem validating objects
   */
  public void validate() throws ObjectConversionException {
//    String[] tokens = line.split("\t", -1);
    try {
      String[] tokens = MAGETABUtils.splitLine(line, false);
      int numTokens = tokens.length - 1;

      String header = MAGETABUtils.digestHeader(tokens[0]);
      if (!header.equals(tag)) {
        String message =
            "Tag is wrong for this handler - " + getClass().getSimpleName() +
                " accepts '" + tag + "' but got '" + header + "'";

        ErrorItem error =
            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                .generateErrorItem(
                    message,
                    ErrorCode.UNKNOWN_ADF_COLUMN_HEADING,
                    this.getClass());

        // tag must be wrong
        throw new ObjectConversionException(error, false, message);
      }
      else if (numTokens > allowedLength) {
        String message =
            "Wrong number of elements on this line - allowed: " +
                (allowedLength < Integer.MAX_VALUE ? allowedLength
                    : "unlimited") + " found: " + numTokens;

        ErrorItem error =
            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                .generateErrorItem(
                    message,
                    23,
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
      getLog().trace("ADF Handler finished validating");
    }
    catch (ParseException e) {
      throw new ObjectConversionException(e.getErrorItem(),
                                          e.isCriticalException(), e);
    }
  }

  /**
   * Performs the unit of work to read the data into the internal datamodel.
   * Override this method in implementations, so that you don't need to
   * implement status updating and checking code.
   *
   * @param value the value to read into the cache
   * @throws ParseException if the header cannot be parsed or there was an error
   *                        reading the value
   */
  protected void readValue(String value) throws ParseException {
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

  protected synchronized void readEmptyValue() {
    // this does nothing but updates the handler to complete
    arrayDesign.ADF.updateTaskList(getTaskIndex(), Status.COMPLETE);
  }
}
