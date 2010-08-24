package uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.attribute;

import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.ProtocolApplicationNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.PerformerAttribute;
import uk.ac.ebi.arrayexpress2.magetab.exception.UnmatchedTagException;

/**
 * Handles performer attributes in the SDRF graph.
 * <p/>
 * Tag: Performer<br/> Allowed child attributes: Comment
 *
 * @author Tony Burdett
 * @date 22-Jan-2009
 */
public class PerformerHandler extends AbstractSDRFAttributeHandler {
  public PerformerHandler() {
    setTag("performer");
  }

  public int assess() {
    // now do the rest
    for (int i = 1; i < values.length;) {
      if (headers[i].startsWith("comment")) {
        // ok
      }
      else if (headers[i].equals("")) {
        // skip the case where the header is an empty string
      }
      else {
        // got to something we don't recognise, so this is the end
        return i - 1;
      }
      i++;
    }

    // iterated over every column, so must have reached the end
    return values.length - 1;
  }

  public void readValues() throws UnmatchedTagException {
    // find the SourceNode to modify
    PerformerAttribute performer;

    if (headers[0].equals(tag)) {
      if (values[0] != null && !values[0].equals("")) {
        // first row, so make a new attribute node
        performer = new PerformerAttribute();
        performer.setNodeType(headers[0]);
        performer.setNodeName(values[0]);
        addNextNodeForCompilation(performer);

        // now do the rest
        boolean emptyHeaderSkipped = false;
        for (int i = 1; i < values.length;) {
          if (headers[i].startsWith("comment")) {
            String type = headers[i].substring(headers[i].lastIndexOf("[") + 1,
                                               headers[i].lastIndexOf("]"));
            performer.comments.put(type, values[i]);
          }
          else if (headers[i].equals("")) {
            // skip the case where the header is an empty string
            emptyHeaderSkipped = true;
          }
          else {
            // got to something we don't recognise, so this is the end

            // first, update parentNode
            synchronized (parentNode) {
              if (parentNode instanceof ProtocolApplicationNode) {
                ProtocolApplicationNode protocolApplication =
                    (ProtocolApplicationNode) parentNode;
                if (protocolApplication.performer == null ||
                    protocolApplication.performer.getNodeName() == null ||
                    !protocolApplication.performer.getNodeName()
                        .equals(performer.getNodeName())) {
                  protocolApplication.performer = performer;
                }
              }
              else {
                String message =
                    "Performer can be applied to Protocol Application nodes, " +
                        "but the parent here was a " +
                        parentNode.getClass().getName();

                ErrorItem error =
                    ErrorItemFactory
                        .getErrorItemFactory(getClass().getClassLoader())
                        .generateErrorItem(
                            message,
                            ErrorCode.BAD_SDRF_ORDERING,
                            this.getClass());

                throw new UnmatchedTagException(error, false, message);
              }
            }

            if (emptyHeaderSkipped) {
              String message =
                  "One or more columns with empty headers were detected " +
                      "and skipped";

              ErrorItem error =
                  ErrorItemFactory
                      .getErrorItemFactory(getClass().getClassLoader())
                      .generateErrorItem(
                          message,
                          ErrorCode.UNKNOWN_SDRF_HEADING,
                          this.getClass());

              throw new UnmatchedTagException(error, false, message);
            }
            else {
              return;
            }
          }
          i++;
        }

        // iterated over every column, so must have reached the end
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