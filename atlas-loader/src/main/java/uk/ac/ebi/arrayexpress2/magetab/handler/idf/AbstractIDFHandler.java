package uk.ac.ebi.arrayexpress2.magetab.handler.idf;

import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.exception.IllegalLineLengthException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.exception.UnmatchedTagException;
import uk.ac.ebi.arrayexpress2.magetab.handler.AbstractReadWriteValidateHandler;
import uk.ac.ebi.arrayexpress2.magetab.lang.Status;
import uk.ac.ebi.arrayexpress2.magetab.utils.MAGETABUtils;
import uk.ac.ebi.gxa.utils.StringUtil;

/**
 * An abstract implementation of an IDFHandler.  This contains most of the
 * bolierplate code, so that specific handlers only need define the name of the
 * tag, the maximum number of values allowed on the line marked by this tag, and
 * how to set the field on the MAGETABInvestigation object.  This should be done
 * by overriding the values for the fields {@link #tag} and {@link
 * #allowedLength} and providing an implementation of {@link
 * #readValue(String)}.
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public abstract class AbstractIDFHandler
    extends AbstractReadWriteValidateHandler implements IDFHandler {
  // investigation and line
  protected MAGETABInvestigation investigation;
  protected String line;

  public void setInvestigation(MAGETABInvestigation investigation) {
    this.investigation = investigation;
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
   * @throws ParseException            if the handler failed to read data from
   *                                   it's source
   * @throws ObjectConversionException if the handler failed to write out
   *                                   appropriate objects
   */
  public void handle() throws ParseException, ObjectConversionException {
    try {
      super.handle();

      // progress updater
      investigation.IDF.updateTaskList(getTaskIndex(), Status.COMPLETE);
      investigation.IDF.increaseProgressBy(increase);
      getLog().trace("Handler " + this.toString() + " finished handling, " +
          "IDF progress now at " + investigation.IDF.getProgress() + " " +
          "total at " + investigation.getProgress());
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
        investigation.IDF.updateTaskList(getTaskIndex(), Status.FAILED);
      }
      else {
        investigation.IDF.updateTaskList(getTaskIndex(), Status.COMPLETE);
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
        investigation.IDF.updateTaskList(getTaskIndex(), Status.FAILED);
      }
      else {
        investigation.IDF.updateTaskList(getTaskIndex(), Status.COMPLETE);
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
                  ErrorCode.UNKNOWN_IDF_HEADING,
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
        investigation.IDF.updateTaskList(getTaskIndex(), Status.READING);
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
    getLog().trace("IDF Handler finished reading");
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
                    ErrorCode.UNKNOWN_IDF_HEADING,
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
          investigation.IDF.updateTaskList(getTaskIndex(), Status.COMPILING);
        }
        // write
        writeValues();
      }
      getLog().trace("IDF Handler finished writing");
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
                    ErrorCode.UNKNOWN_IDF_HEADING,
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
          investigation.IDF.updateTaskList(getTaskIndex(), Status.VALIDATING);
        }
        // write
        validateValues();
      }
      getLog().trace("IDF Handler finished validating");
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
    investigation.IDF.updateTaskList(getTaskIndex(), Status.COMPLETE);
  }
}
