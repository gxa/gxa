package uk.ac.ebi.arrayexpress2.magetab.handler.adf.node;

import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node.CompositeElementNode;
import uk.ac.ebi.arrayexpress2.magetab.exception.IllegalLineLengthException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.exception.UnmatchedTagException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.AbstractADFGraphHandler;
import uk.ac.ebi.arrayexpress2.magetab.lang.Status;
import uk.ac.ebi.gxa.utils.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * A handler that handles composite element nodes in the ADF graph.
 * <p/>
 * Tag: Composite Element Name <br/> Allowed Attributes: Composite Element
 * Database Entry, Composite Element Comment, Comment
 *
 * @author Tony Burdett
 * @date 17-Feb-2010
 */
public class CompositeElementHandler extends AbstractADFGraphHandler {
  public CompositeElementHandler() {
    setTag("compositeelementname");
  }

  public boolean canHandle(String tag) {
    return tag.startsWith(getTag());
  }

  public void read() throws ParseException {
    if (!headers[0].startsWith(tag)) {
      String message =
          "Tag is wrong for this handler - " + getClass().getSimpleName() +
              " accepts '*" + getTag() + "' but got '" +
              headers[0] + "'";

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
                " accepts '" + getTag() + "' but got '" + headers[0] +
                "'";

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
      if (!headers[0].startsWith(getTag())) {
        String message =
            "Tag is wrong for this handler - " + getClass().getSimpleName() +
                " accepts '" + getTag() + "' but got '" + headers[0] +
                "'";

        ErrorItem error =
            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                .generateErrorItem(
                    message,
                    ErrorCode.UNKNOWN_ADF_COLUMN_HEADING,
                    this.getClass());

        throw new ObjectConversionException(error, false, message);
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
    getLog().debug("SDRF Handler finished validating");
  }

  public int assess() {
    for (int i = 1; i < values.length;) {
      if (headers[i].startsWith("compositeelementdatabaseentry")) {
        // don't need to do anything here, just read on to the next column
      }
      else if (headers[i].startsWith("compositeelementcomment")) {
        // don't need to do anything here
      }
      else if (headers[i].startsWith("comment")) {
        // don't need to do anything here
      }
      else {
        // got to something we don't recognise
        // this is either the end, or a non-handled column name
        return i;
      }
      i++;
    }

    // iterated over every column, so must have reached the end
    return values.length;
  }

  public void readValues() throws ParseException {
    // find the ReporterNode to modify
    CompositeElementNode compositeElementNode;

    if (headers[0].equals(tag)) {
      // first values, so lookup or make a new sourceNode
      synchronized (arrayDesign.ADF) {
        compositeElementNode =
            arrayDesign.ADF.lookupNode(values[0], CompositeElementNode.class);
        if (compositeElementNode == null) {
          compositeElementNode = new CompositeElementNode();
          compositeElementNode.setNodeType(headers[0]);
          compositeElementNode.setNodeName(values[0]);
          arrayDesign.ADF.storeNode(compositeElementNode);
          addNextNodeForCompilation(compositeElementNode);
        }
      }

      // now do the rest
      for (int i = 1; i < values.length;) {
        if (headers[i].startsWith("compositeelementdatabaseentry")) {
          String type =
              headers[i].substring(headers[i].lastIndexOf("[") + 1,
                                   headers[i].lastIndexOf("]"));
          if (compositeElementNode.compositeElementDatabaseEntries
              .containsKey(type)) {
            compositeElementNode.compositeElementDatabaseEntries.get(type)
                .add(values[i]);
          }
          else {
            List<String> cdeValues = new ArrayList<String>();
            cdeValues.add(values[i]);
            compositeElementNode.compositeElementDatabaseEntries
                .put(type, cdeValues);
          }
        }
        else if (headers[i].startsWith("compositeelementcomment")) {
          String type =
              headers[i].substring(headers[i].lastIndexOf("[") + 1,
                                   headers[i].lastIndexOf("]"));
          compositeElementNode.comments.put(type, values[i]);
        }
        else if (headers[i].startsWith("comment")) {
          String type =
              headers[i].substring(headers[i].lastIndexOf("[") + 1,
                                   headers[i].lastIndexOf("]"));
          compositeElementNode.comments.put(type, values[i]);
        }
        else {
          // got to something we don't recognise
          // this is either the end, or a bad column name
          // check the name of the next node and put it in this one

          // check child node type
          // loop over values until we get to something with a value present
          int k = i;
          while (k < values.length &&
              (StringUtil.isEmpty(values[k]))) {
            k++;
          }

          if (k < values.length) {
            // add child node value
            String childNodeType = headers[k];
            String childNodeValue = values[k];
            compositeElementNode.addChildNode(childNodeType, childNodeValue);
          }

          break;
        }
        i++;
      }
      // iterated over every column, so must have reached the end
      // update node in ADF
      arrayDesign.ADF.updateNode(compositeElementNode);
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
