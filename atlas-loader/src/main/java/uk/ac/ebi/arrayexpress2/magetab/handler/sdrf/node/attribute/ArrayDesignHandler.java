package uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.attribute;

import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.ArrayDesignNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.HybridizationNode;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.exception.UnmatchedTagException;
import uk.ac.ebi.gxa.utils.StringUtil;

/**
 * A handler implementation that will handle Array Design fields in the SDRF.
 * This is an abstract class - array designs can be either "file" or "ref"
 * depending on whether this references a local file or an accessioned array
 * design from a public resource.
 * <p/>
 * Tag: Array Design File/Ref<br/> Allowed child attributes: Term Source Ref,
 * Term Accession Number, Comment
 *
 * @author Tony Burdett
 * @date 22-Jan-2009
 */
public abstract class ArrayDesignHandler extends AbstractSDRFAttributeHandler {
  public int assess() {
    for (int i = 1; i < values.length;) {
      if (headers[i].equals("termsourceref")) {
        // ok
      }
      else if (headers[i].equals("termaccessionnumber")) {
        // ok
      }
      else if (headers[i].startsWith("comment")) {
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

  public void readValues() throws ParseException {
    // find the SourceNode to modify
    ArrayDesignNode adf;

    if (headers[0].equals(tag)) {
        if (!StringUtil.isEmpty(values[0])) {
        adf = new ArrayDesignNode();
        adf.setNodeType(headers[0]);
        adf.setNodeName(values[0]);
        addNextNodeForCompilation(adf);

        // now do the rest
        boolean emptyHeaderSkipped = false;
        for (int i = 1; i < values.length;) {
          if (headers[i].equals("termsourceref")) {
            adf.termSourceREF = values[i];
          }
          else if (headers[i].equals("termaccessionnumber")) {
            adf.termAccessionNumber = values[i];
          }
          else if (headers[i].startsWith("comment")) {
            String type = headers[i].substring(headers[i].lastIndexOf("[") + 1,
                                               headers[i].lastIndexOf("]"));
            adf.comments.put(type, values[i]);
          }
          else if (headers[i].equals("")) {
            // skip the case where the header is an empty string
            emptyHeaderSkipped = true;
          }
          else {
            // got to something we don't recognise, so this is the end

            // first, update parentNode
            synchronized (parentNode) {
              if (parentNode instanceof HybridizationNode) {
                HybridizationNode hyb = (HybridizationNode) parentNode;
                boolean seenBefore = false;
                for (ArrayDesignNode ad : hyb.arrayDesigns) {
                  if (ad.getNodeName().equals(adf.getNodeName())) {
                    // these characteristics have been seen before
                    seenBefore = true;
                  }
                }

                // add if we haven't seen it before
                if (!seenBefore) {
                  hyb.arrayDesigns.add(adf);
                }
              }
              else {
                String message =
                    "Array Designs can be applied to Hybridization " +
                        "nodes, but the parent here was a " +
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