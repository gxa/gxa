package uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node;

import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.HybridizationNode;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.exception.UnmatchedTagException;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.AbstractSDRFHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.attribute.ArrayDesignFileHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.attribute.ArrayDesignHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.attribute.ArrayDesignRefHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.attribute.FactorValueHandler;

/**
 * A handler that handles Hybridization nodes in the SDRF graph
 * <p/>
 * Tag: Hybridization Name <br/>Allowed attributes: Factor Value, Comment
 *
 * @author Tony Burdett
 * @date 22-Jan-2009
 */
public class HybridizationHandler extends AbstractSDRFHandler {
  public HybridizationHandler() {
    setTag("hybridizationname");
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
        FactorValueHandler handler = new FactorValueHandler(1);
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
        i += assessAttribute(handler, headers, values, i);
      }
      else if (headers[i].equals("")) {
        // skip the case where the header is an empty string
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
    HybridizationNode hyb;

    if (headers[0].equals(tag)) {
      // first row, so lookup or make a new source
      synchronized (investigation.SDRF) {
        hyb = investigation.SDRF.lookupNode(values[0], HybridizationNode.class);
        if (hyb == null) {
          hyb = new HybridizationNode();
          hyb.setNodeType(headers[0]);
          hyb.setNodeName(values[0]);
          investigation.SDRF.storeNode(hyb);
          addNextNodeForCompilation(hyb);
        }
      }

      // now do the rest
      boolean emptyHeaderSkipped = false;
      for (int i = 1; i < values.length;) {
        if (headers[i].startsWith("comment")) {
          String type = headers[i].substring(headers[i].lastIndexOf("[") + 1,
                                             headers[i].lastIndexOf("]"));
          hyb.comments.put(type, values[i]);
        }
        else if (headers[i].startsWith("factorvalue")) {
          FactorValueHandler handler = new FactorValueHandler(1);
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
          i += handleAttribute(hyb, handler, headers, values, i);
        }
        else if (headers[i].equals("")) {
          // skip the case where the header is an empty string
          emptyHeaderSkipped = true;
        }
        else {
          // got to something we don't recognise
          // this is either the end, or a bad column name
          // update the child node
          updateChildNode(hyb, i);
          break;
        }
        i++;
      }

      // iterated over every column, so must have reached the end
      // update node in SDRF
      investigation.SDRF.updateNode(hyb);

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
}