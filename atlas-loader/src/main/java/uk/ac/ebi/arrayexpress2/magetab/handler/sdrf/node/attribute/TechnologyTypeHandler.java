package uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.attribute;

import com.google.common.base.Strings;
import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.AssayNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.TechnologyTypeAttribute;
import uk.ac.ebi.arrayexpress2.magetab.exception.UnmatchedTagException;

/**
 * Handles technology type attributes in the SDRF graph.
 * <p/>
 * Tag: Technology Type <br/>Allowed child attributes: Term Source Ref, Term
 * Accession Number
 *
 * @author Tony Burdett
 * @date 30-Apr-2009
 */
public class TechnologyTypeHandler extends AbstractSDRFAttributeHandler {
  public TechnologyTypeHandler() {
    setTag("technologytype");
  }

  public int assess() {
    for (int i = 1; i < values.length;) {
      if (headers[i].equals("termsourceref")) {
        // ok
      }
      else if (headers[i].equals("termaccessionnumber")) {
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
    TechnologyTypeAttribute techType;

    if (headers[0].equals(tag)) {
        if (!Strings.isNullOrEmpty(values[0])) {
        // first row, so make a new attribute node
        techType = new TechnologyTypeAttribute();
        techType.setNodeType(headers[0]);
        techType.setNodeName(values[0]);

        // now do the rest
        boolean emptyHeaderSkipped = false;
        for (int i = 1; i < values.length;) {
          if (headers[i].equals("termsourceref")) {
            techType.termSourceREF = values[i];
          }
          else if (headers[i].equals("termaccessionnumber")) {
            techType.termAccessionNumber = values[i];
          }
          else if (headers[i].equals("")) {
            // skip the case where the header is an empty string
            emptyHeaderSkipped = true;
          }
          else {
            // got to something we don't recognise, so this is the end

            // first, update parentNode
            synchronized (parentNode) {
              if (parentNode instanceof AssayNode) {
                AssayNode assayNode = (AssayNode) parentNode;
                if (assayNode.technologyType == null ||
                    assayNode.technologyType.getNodeName() == null ||
                    !assayNode.technologyType.getNodeName()
                        .equals(techType.getNodeName())) {
                  assayNode.technologyType = techType;
                }
              }
              else {
                String message =
                    "Technology Type can be applied to Assay nodes, but the " +
                        "parent here was a " + parentNode.getClass().getName();

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
