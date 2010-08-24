package uk.ac.ebi.arrayexpress2.magetab.handler.sdrf.node.attribute;

import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.ExtractNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.LabeledExtractNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SampleNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.SourceNode;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.MaterialTypeAttribute;
import uk.ac.ebi.arrayexpress2.magetab.exception.UnmatchedTagException;

/**
 * Handles material type attributes in the SDRF graph.
 * <p/>
 * Tag: Material Type<br/> Allowed child attributes: Term Source Ref, Term
 * Accession Number
 *
 * @author Tony Burdett
 * @date 22-Jan-2009
 */
public class MaterialTypeHandler extends AbstractSDRFAttributeHandler {
  public MaterialTypeHandler() {
    setTag("materialtype");
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
    MaterialTypeAttribute materialType;

    if (headers[0].equals(tag)) {
      if (values[0] != null && !values[0].equals("")) {
        // first row, so make a new attribute node
        materialType = new MaterialTypeAttribute();
        materialType.setNodeType(headers[0]);
        materialType.setNodeName(values[0]);
        addNextNodeForCompilation(materialType);

        // now do the rest
        boolean emptyHeaderSkipped = false;
        for (int i = 1; i < values.length;) {
          if (headers[i].equals("termsourceref")) {
            materialType.termSourceREF = values[i];
          }
          else if (headers[i].equals("termaccessionnumber")) {
            materialType.termAccessionNumber = values[i];
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
                if (sourceNode.materialType == null ||
                    sourceNode.materialType.getNodeName() == null ||
                    !sourceNode.materialType.getNodeName()
                        .equals(materialType.getNodeName())) {
                  sourceNode.materialType = materialType;
                }
              }
              else if (parentNode instanceof SampleNode) {
                SampleNode sampleNode = (SampleNode) parentNode;
                if (sampleNode.materialType == null ||
                    sampleNode.materialType.getNodeName() == null ||
                    !sampleNode.materialType.getNodeName()
                        .equals(materialType.getNodeName())) {
                  sampleNode.materialType = materialType;
                }
              }
              else if (parentNode instanceof ExtractNode) {
                ExtractNode extract = (ExtractNode) parentNode;
                if (extract.materialType == null ||
                    extract.materialType.getNodeName() == null ||
                    !extract.materialType.getNodeName()
                        .equals(materialType.getNodeName())) {
                  extract.materialType = materialType;
                }
              }
              else if (parentNode instanceof LabeledExtractNode) {
                LabeledExtractNode labeledExtract =
                    (LabeledExtractNode) parentNode;
                if (labeledExtract.materialType == null ||
                    labeledExtract.materialType.getNodeName() == null ||
                    !labeledExtract.materialType.getNodeName()
                        .equals(materialType.getNodeName())) {
                  labeledExtract.materialType = materialType;
                }
              }
              else {
                String message =
                    "MaterialType can be applied to Source, Sample, Extract " +
                        "or LabeledExtract nodes, but the parent here was a " +
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