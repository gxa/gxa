package uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node;

import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.DerivedArrayDataMatrixNode;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.exception.UnmatchedTagException;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.AbstractSDRFHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.SDRFDatatypeHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.attribute.ArrayDesignFileHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.attribute.ArrayDesignHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.attribute.ArrayDesignRefHandler;

/**
 * A handler that handles Derived Array Data Matrix nodes in the SDRF graph
 * <p/>
 * Tag: Derived Array Data Matrix File <br/>Allowed attributes: Array Design
 * File/Ref, Comment
 *
 * @author Tony Burdett
 * @date 22-Jan-2009
 */
public class DerivedArrayDataMatrixHandler extends AbstractSDRFHandler
    implements SDRFDatatypeHandler {
  public DerivedArrayDataMatrixHandler() {
    setTag("derivedarraydatamatrixfile");
  }

  public int assess() {
    for (int i = 1; i < values.length;) {
      if (headers[i].startsWith("comment")) {
        // don't need to do anything here
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
        return i;
      }
      i++;
    }

    // iterated over every column, so must have reached the end
    return values.length;
  }

  public void readValues() throws ParseException {
    // find the SourceNode to modify
    DerivedArrayDataMatrixNode dadmf;

    if (headers[0].equals(tag)) {
      // first row, so lookup or make a new source
      synchronized (investigation.SDRF) {
        dadmf = investigation.SDRF.lookupNode(values[0],
                                              DerivedArrayDataMatrixNode.class);
        if (dadmf == null) {
          dadmf = new DerivedArrayDataMatrixNode();
          dadmf.setNodeType(headers[0]);
          dadmf.setNodeName(values[0]);
          investigation.SDRF.storeNode(dadmf);
          addNextNodeForCompilation(dadmf);
        }
      }

      // now do the rest
      boolean emptyHeaderSkipped = false;
      for (int i = 1; i < values.length;) {
        if (headers[i].startsWith("comment")) {
          String type = headers[i].substring(headers[i].lastIndexOf("[") + 1,
                                             headers[i].lastIndexOf("]"));
          dadmf.comments.put(type, values[i]);
        }
        else if (headers[i].startsWith("arraydesign")) {
          ArrayDesignHandler handler;
          if (headers[i].endsWith("file")) {
            handler = new ArrayDesignFileHandler();
          }
          else {
            handler = new ArrayDesignRefHandler();
          }
          i += handleAttribute(dadmf, handler, headers, values, i);
        }
        else if (headers[i].equals("")) {
          // skip the case where the header is an empty string
          emptyHeaderSkipped = true;
        }
        else {
          // got to something we don't recognise 
          // this is either the end, or a bad column name
          // check the name of the next node and put it in this one
          // but be wary of Factor Value nodes
          int j = i;
          while (j < headers.length &&
              (headers[j].startsWith("factorvalue") ||
                  headers[j].startsWith("unit") ||
                  headers[j].startsWith("termsourceref") ||
                  headers[j].startsWith("comment"))) {
            // loop over every factor value column
            j++;
          }
          i = j;

          if (i < headers.length) {
            // add child node, as this isn't the very last node in the row

            // update the child node
            updateChildNode(dadmf, i);
            break;
          }
        }
        i++;
      }

      // iterated over every column, so must have reached the end
      // update node in SDRF
      investigation.SDRF.updateNode(dadmf);

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

  public String getDatatypeName() {
    return "processedDataMatrix";
  }
}