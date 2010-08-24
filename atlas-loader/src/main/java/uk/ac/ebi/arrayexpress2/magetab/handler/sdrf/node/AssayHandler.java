package uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node;

import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AssayNode;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.exception.UnmatchedTagException;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.AbstractSDRFHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.attribute.*;

/**
 * A handler that handles Assay nodes in the SDRF graph
 * <p/>
 * Tag: Assay Name <br/>Allowed attributes: Factor Value, Array Design File/Ref,
 * Technology Type, Comment
 *
 * @author Tony Burdett
 * @date 30-Apr-2009
 */
public class AssayHandler extends AbstractSDRFHandler {
  public AssayHandler() {
    setTag("assayname");
  }

  public int assess() {
    int kickOutAt = values.length;

    for (int i = 1; i < values.length;) {
      if (headers[i].startsWith("comment")) {
        // don't need to do anything here
      }
      else if (headers[i].startsWith("factorvalue")) {
        // we have to kick out at the first factor value we see,
        // so we can leave this to be handled by a node handler

        // don't overwrite though, if we're kicking out earlier than this
        if (i < kickOutAt) {
          kickOutAt = i;
        }

        // and work out how many to skip
        FactorValueHandler handler =
            new FactorValueHandler(1);
        i += assessAttribute(handler, headers, values, i);
      }
      else if (headers[i].equals("")) {
        // skip the case where the header is an empty string
      }
      else if (headers[i].startsWith("arraydesign")) {
        ArrayDesignHandler handler;
        if (headers[i].endsWith("file")) {
          handler = new ArrayDesignFileHandler();
        }
        else {
          handler = new ArrayDesignRefHandler();
        }
        i += assessAttribute(handler, headers, values, i);
      }
      else if (headers[i].startsWith("technologytype")) {
        TechnologyTypeHandler handler = new TechnologyTypeHandler();
        i += assessAttribute(handler, headers, values, i);

      }
      else {
        // got to something we don't recognise
        // this is either the end, or a bad column name
        if (i < kickOutAt) {
          return i;
        }
        else {
          return kickOutAt;
        }
      }
      i++;
    }

    // iterated over every column, so must have reached the end
    return values.length;
  }

  public void readValues() throws ParseException {
    // find the SourceNode to modify
    AssayNode assay;

    if (headers[0].equals(tag)) {
      // first row, so lookup or make a new source
      synchronized (investigation.SDRF) {
        assay = investigation.SDRF.lookupNode(values[0], AssayNode.class);
        if (assay == null) {
          assay = new AssayNode();
          assay.setNodeType(headers[0]);
          assay.setNodeName(values[0]);
          investigation.SDRF.storeNode(assay);
          addNextNodeForCompilation(assay);
        }
      }
      // now do the rest
      boolean emptyHeaderSkipped = false;
      for (int i = 1; i < values.length;) {
        if (headers[i].startsWith("comment")) {
          String type = headers[i].substring(headers[i].lastIndexOf("[") + 1,
                                             headers[i].lastIndexOf("]"));
          assay.comments.put(type, values[i]);
        }
        else if (headers[i].startsWith("factorvalue")) {
          FactorValueHandler handler =
              new FactorValueHandler(1);
          // use assement to just skip these columns
          i += assessAttribute(handler, headers, values, i);
        }
        else if (headers[i].startsWith("arraydesign")) {
          ArrayDesignHandler handler;
          if (headers[i].endsWith("file")) {
            handler = new ArrayDesignFileHandler();
          }
          else {
            handler = new ArrayDesignRefHandler();
          }
          i += handleAttribute(assay, handler, headers, values, i);
        }
        else if (headers[i].startsWith("technologytype")) {
          TechnologyTypeHandler handler = new TechnologyTypeHandler();
          i += handleAttribute(assay, handler, headers, values, i);
        }
        else if (headers[i].equals("")) {
          // skip the case where the header is an empty string
          emptyHeaderSkipped = true;
        }
        else {
          // got to something we don't recognise
          // this is either the end, or a bad column name
          // update the child node
          updateChildNode(assay, i);
          break;
        }
        i++;
      }

      // iterated over every column, so must have reached the end
      // update node in SDRF
      investigation.SDRF.updateNode(assay);

      // throw exception if we had to skip something
      if (emptyHeaderSkipped) {
        String message =
            "One or more columns with empty headers were detected " +
                "and skipped";

        ErrorItem error =
            ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
                .generateErrorItem(
                    message,
                    ErrorCode.UNKNOWN_SDRF_HEADING,
                    this.getClass());

        throw new UnmatchedTagException(error, false, message);
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

  /**
   * Write the values that were parsed into memory from a MAGE-TAB file out to
   * an external source.  This would normally be a database, and XML document,
   * an interface or some other repository.  This implementation is a
   * non-writing implementation so calling this method won't actually do
   * anything.
   *
   * @throws uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException
   *          if there is an error converting in memory objects to a serialized
   *          form
   */
  public void writeValues() throws ObjectConversionException {
  }
}
