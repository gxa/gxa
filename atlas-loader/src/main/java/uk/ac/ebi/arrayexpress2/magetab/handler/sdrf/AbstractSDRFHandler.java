package uk.ac.ebi.arrayexpress2.magetab.handler.sdrf;

import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SDRFNode;
import uk.ac.ebi.arrayexpress2.magetab.exception.IllegalLineLengthException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.exception.UnmatchedTagException;
import uk.ac.ebi.arrayexpress2.magetab.handler.AbstractReadWriteValidateHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.attribute.SDRFAttributeHandler;
import uk.ac.ebi.arrayexpress2.magetab.lang.Status;
import uk.ac.ebi.arrayexpress2.magetab.utils.MAGETABUtils;
import uk.ac.ebi.gxa.utils.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * An abstract implementation of an SDRFHandler.  This contains most of the
 * bolierplate code, so that specific handlers only need define the name of the
 * tag, the maximum number of values allowed on the line marked by this tag, and
 * how to set the field on the MAGETABInvestigation object.  This should be done
 * by overriding the values for the fields {@link #tag} and {@link
 * #allowedLength} and providing an implementation of {@link #readValues()} and
 * {@link #writeValues()} .
 *
 * @author Tony Burdett
 * @date 22-Jan-2009
 */
public abstract class AbstractSDRFHandler
    extends AbstractReadWriteValidateHandler
    implements SDRFHandler {
  /**
   * The MAGE-TAB object model so far
   */
  protected MAGETABInvestigation investigation;
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
  protected List<SDRFNode> nodesToCompile = new ArrayList<SDRFNode>();

  public String handlesName() {
    if (values == null) {
      return "";
    }
    else {
      return values[0];
    }
  }

  public void setInvestigation(MAGETABInvestigation investigation) {
    this.investigation = investigation;
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
        investigation.SDRF.updateTaskList(getTaskIndex(), Status.PERSISTING);
        investigation.SDRF.increaseProgressBy(increase);
        getLog().trace("Handler " + this.toString() + " finished handling, " +
            "SDRF progress now at " + investigation.SDRF.getProgress() + " " +
            "total at " + investigation.getProgress());
      }
    }
    catch (ParseException e) {
      if (getTaskIndex() != -1) {
        if (e.isCriticalException()) {
          getLog().debug("Critical parse exception, handler " +
              getClass().getSimpleName() + " failed (" +
              e.getErrorItem().getComment() + ")");
          investigation.SDRF.updateTaskList(getTaskIndex(), Status.FAILED);
        }
        else {
          investigation.SDRF.updateTaskList(getTaskIndex(), Status.COMPLETE);
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
          investigation.SDRF.updateTaskList(getTaskIndex(), Status.FAILED);
        }
        else {
          investigation.SDRF.updateTaskList(getTaskIndex(), Status.COMPLETE);
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
                  ErrorCode.SDRF_FIELD_PRESENT_BUT_NO_DATA,
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
                    ErrorCode.UNKNOWN_SDRF_HEADING,
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
    getLog().trace("SDRF Handler finished reading");
  }

  public void write() throws ObjectConversionException {
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
      if (!headers[0].equals(tag)) {
        String message =
            "Tag is wrong for this handler - " + getClass().getSimpleName() +
                " accepts '" + tag + "' but got '" + headers[0] + "'";

        ErrorItem error =
            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                .generateErrorItem(
                    message,
                    ErrorCode.UNKNOWN_SDRF_HEADING,
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
                    ErrorCode.UNKNOWN_SDRF_HEADING,
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
   * Add a "node" object from the SDRF data model into a cache.  This cache
   * contains the objects that are ready for compilation into AE2 data model
   * objects, and would be peeled off during the writeValues() operation,
   * turning them into full database objects.  Normally, you would use this
   * method to add every node created during readValue() operations into a
   * store.
   *
   * @param node the SDRF node to store
   */
  protected synchronized void addNextNodeForCompilation(SDRFNode node) {
    nodesToCompile.add(node);
  }

  /**
   * Get a "node" object from the SDRF data model to a cache.  This cache
   * contains the objects that are ready for compilation into AE2 data model
   * objects, and this method peels off the next node in the list.  Usually you
   * would use this during the writeValues() operation.
   *
   * @return the next SDRF node ready for conversion
   */
  protected synchronized SDRFNode getNextNodeForCompilation() {
    if (nodesToCompile.size() > 0) {
      SDRFNode node = nodesToCompile.get(0);
      nodesToCompile.remove(node);
      return node;
    }
    else {
      return null;
    }
  }

  protected int assessAttribute(SDRFAttributeHandler handler, String[] headers,
                                String[] values, int startIndex) {
    String[] headerData =
        MAGETABUtils.extractRange(headers, startIndex, headers.length);
    String[] valuesData =
        MAGETABUtils.extractRange(values, startIndex, values.length);
    handler.setData(headerData, valuesData);
    return handler.assess();
  }

  protected int handleAttribute(SDRFNode parentNode,
                                SDRFAttributeHandler handler,
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
    handler.setInvestigation(investigation);
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
                                   "to SDRFNodes in the datamodel", e);
    }
  }

  protected void updateChildNode(SDRFNode node, int valueIndex) {
    // check child node type
    // loop over values until we get to something with a value present
    int i = valueIndex;
    while (i < values.length) {
      // value present?
      if (StringUtil.isEmpty(values[i])) {
        // no value, continue
        i++;
      }
      else {
        if (headers[i].endsWith("name") ||
            headers[i].endsWith("ref") ||
            headers[i].endsWith("file")) {
          // nodes end with "name", "ref" or "file" strings, so this is the child node
          break;
        }
        else {
          // this is not a recognised header node
          i++;
        }
      }
    }

    if (i < values.length) {
      // add child node value
      String childNodeType = headers[i];
      String childNodeValue;
      if (childNodeType.endsWith("protocolref")) {
        childNodeValue = node.getNodeName() + ":" + values[i];

        // and read forward to pick up the parameters too
        i++;
        while (i < values.length &&
            !headers[i].endsWith("protocolref")) {
          if (headers[i].startsWith("parametervalue")) {
            String parameterType =
                headers[i].substring(headers[i].lastIndexOf("[") + 1,
                                     headers[i].lastIndexOf("]"));
            String parameterValue;
            if (StringUtil.isEmpty(values[i])) {
              // just ignore this parameter
            }
            else {
              childNodeValue = childNodeValue.concat(
                  "[" + parameterType + "=" + values[i] + "]");
            }
          }
          i++;
        }
      }
      else {
        childNodeValue = values[i];
      }
      node.addChildNode(childNodeType, childNodeValue);
    }
  }
}
