package uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl;

import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.exception.IllegalLineLengthException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.exception.UnmatchedTagException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.AbstractADFHeaderHandler;
import uk.ac.ebi.arrayexpress2.magetab.lang.Status;
import uk.ac.ebi.arrayexpress2.magetab.utils.MAGETABUtils;

/**
 * A handler that handles ADF rows describing comments
 * <p/>
 * Tag: Comment[]
 *
 * @author Tony Burdett
 * @date 15-Feb-2010
 */
public class CommentHandler extends AbstractADFHeaderHandler {
  public CommentHandler() {
    setTag("comment");
  }

  public String handlesTag() {
    return "comment[]";
  }

  public boolean canHandle(String tag) {
    if (tag.startsWith(getTag())) {
      if (!tag.equalsIgnoreCase("comment[arrayexpressaccession]")) {
        return true;
      }
    }

    return false;
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

    if (!header.startsWith(tag)) {
      // tag must be wrong

      // generate error and throw the exception
      String message =
          "Tag is wrong for this handler - " + getClass().getSimpleName() +
              " accepts '" + tag + "' but got '" + header + "'";
      ErrorItem error =
          ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
              .generateErrorItem(
                  message,
                  ErrorCode.UNKNOWN_ADF_ROW_HEADING,
                  this.getClass());
      // throw the exception
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
      String type =
          header.substring(header.indexOf("[") + 1, header.lastIndexOf("]"));
      readType(type);

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
      if (!header.startsWith(tag)) {
        String message =
            "Tag is wrong for this handler - " + getClass().getSimpleName() +
                " accepts '" + tag + "' but got '" + header + "'";

        ErrorItem error =
            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                .generateErrorItem(
                    message, ErrorCode.UNKNOWN_ADF_ROW_HEADING,
                    this.getClass());

        throw new ObjectConversionException(error, true, message);
      }
      else if (numTokens > allowedLength) {
        String message = "Wrong number of elements on this line - allowed: " +
            (allowedLength < Integer.MAX_VALUE ? allowedLength
                : "unlimited") + " found: " + numTokens;

        ErrorItem error =
            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                .generateErrorItem(
                    message, 23, this.getClass());

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
      if (!header.startsWith(tag)) {
        String message =
            "Tag is wrong for this handler - " + getClass().getSimpleName() +
                " accepts '" + tag + "' but got '" + header + "'";

        ErrorItem error =
            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                .generateErrorItem(
                    message, ErrorCode.UNKNOWN_ADF_ROW_HEADING,
                    this.getClass());

        throw new ObjectConversionException(error, false, message);
      }
      else if (numTokens > allowedLength) {
        String message = "Wrong number of elements on this line - allowed: " +
            (allowedLength < Integer.MAX_VALUE ? allowedLength
                : "unlimited") + " found: " + numTokens;

        ErrorItem error =
            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                .generateErrorItem(
                    message, 23, this.getClass());

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
      getLog().trace("IDF Handler finished validating");
    }
    catch (ParseException e) {
      throw new ObjectConversionException(e.getErrorItem(),
                                          e.isCriticalException(), e);
    }
  }


  private String lastType;

  public void readType(String type) {
    this.lastType = type;
  }

  public void readValue(String value) throws ParseException {
    arrayDesign.ADF.addComment(lastType, value);
  }
}
