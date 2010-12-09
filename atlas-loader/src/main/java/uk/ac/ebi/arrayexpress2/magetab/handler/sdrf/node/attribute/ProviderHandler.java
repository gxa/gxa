package uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.attribute;

import com.google.common.base.Strings;
import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SourceNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.ProviderAttribute;
import uk.ac.ebi.arrayexpress2.magetab.exception.UnmatchedTagException;

/**
 * Handles provider attributes in the SDRF graph.
 * <p/>
 * Tag: Provider<br/> Allowed child attributes: Comment
 *
 * @author Tony Burdett
 * @date 22-Jan-2009
 */
public class ProviderHandler extends AbstractSDRFAttributeHandler {
  public ProviderHandler() {
    setTag("provider");
  }

  public int assess() {
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
    ProviderAttribute provider;

    if (headers[0].equals(tag)) {
        if (!Strings.isNullOrEmpty(values[0])) {
        // first row, so make a new attribute node
        provider = new ProviderAttribute();
        provider.setNodeType(headers[0]);
        provider.setNodeName(values[0]);
        addNextNodeForCompilation(provider);

        // now do the rest
        boolean emptyHeaderSkipped = false;
        for (int i = 1; i < values.length;) {
          if (headers[i].startsWith("comment")) {
            String type = headers[i].substring(headers[i].lastIndexOf("[") + 1,
                                               headers[i].lastIndexOf("]"));
            provider.comments.put(type, values[i]);
          }
          else if (headers[i].equals("")) {
            // skip the case where the header is an empty string
            emptyHeaderSkipped = true;
          }
          else {
            // got to something we don't recognise, so this is the end

            // first, update parentNode
            synchronized (parentNode) {
              if (parentNode instanceof SourceNode) {
                SourceNode sourceNode = (SourceNode) parentNode;
                if (sourceNode.provider == null ||
                    sourceNode.provider.getNodeName() == null ||
                    !sourceNode.provider.getNodeName()
                        .equals(provider.getNodeName())) {
                  sourceNode.provider = provider;
                }
              }
              else {
                String message =
                    "Provider can be applied to Source nodes, but the parent " +
                        "here was a " + parentNode.getClass().getName();

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