package uk.ac.ebi.arrayexpress2.magetab.handler.adf.node.attribute;

import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node.ReporterNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node.attribute.ControlTypeAttribute;
import uk.ac.ebi.arrayexpress2.magetab.exception.UnmatchedTagException;
import uk.ac.ebi.gxa.utils.StringUtil;

/**
 * Handles control type attributes in the ADF graph part.
 * <p/>
 * Tag: Control Type<br/> Allowed Child Attributes: Term Source REF, Term
 * Accession Number
 *
 * @author Tony Burdett
 * @date 17-Feb-2010
 */
public class ControlTypeHandler extends AbstractADFAttributeHandler {
  public ControlTypeHandler() {
    setTag("controltype");
  }

  public int assess() {
    for (int i = 1; i < values.length;) {
      if (headers[i].equals("termsourceref")) {
        // ok
      }
      else if (headers[i].equals("termaccessionnumber")) {
        // ok
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
    ControlTypeAttribute controlType;

    if (headers[0].equals(tag)) {
        if (!StringUtil.isEmpty(values[0])) {
        // first row, so make a new attribute node
        controlType = new ControlTypeAttribute();
        controlType.setNodeType(headers[0]);
        controlType.setNodeName(values[0]);

        // now do the rest
        for (int i = 1; i < values.length;) {
          if (headers[i].equals("termsourceref")) {
            controlType.termSourceREF = values[i];
          }
          else if (headers[i].equals("termaccessionnumber")) {
            controlType.termAccessionNumber = values[i];
          }
          else {
            // got to something we don't recognise, so this is the end

            // first, update parentNode
            synchronized (parentNode) {
              if (parentNode instanceof ReporterNode) {
                ReporterNode reporterNode =
                    (ReporterNode) parentNode;
                reporterNode.controlTypeAttributes.add(controlType);
              }
              else {
                String message =
                    "Control Types can be applied to Reporter nodes, but the " +
                        "parent here was a " + parentNode.getClass().getName();

                ErrorItem error =
                    ErrorItemFactory
                        .getErrorItemFactory(getClass().getClassLoader())
                        .generateErrorItem(
                            message,
                            ErrorCode.BAD_REPORTER,
                            this.getClass());

                throw new UnmatchedTagException(error, false, message);
              }
            }

            return;
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
                  ErrorCode.UNKNOWN_ADF_COLUMN_HEADING,
                  this.getClass());

      throw new UnmatchedTagException(error, false, message);
    }
  }
}
