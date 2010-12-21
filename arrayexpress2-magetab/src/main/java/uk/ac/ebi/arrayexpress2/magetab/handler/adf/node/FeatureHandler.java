package uk.ac.ebi.arrayexpress2.magetab.handler.adf.node;

import com.google.common.base.Strings;
import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.adf.node.FeatureNode;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.exception.UnmatchedTagException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.AbstractADFGraphHandler;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.node.attribute.ReporterGroupHandler;

/**
 * A handler that handles feature nodes in the ADF graph.  Features are special
 * types of nodes in as much as they are not explicity named (with a *** Name)
 * column header, like all other nodes.  Instead, they are the aggregation of
 * the four integer-type column headers.
 * <p/>
 * Tags: Block Column, Block Row, Column, Row Allowed Attributes: Comment
 *
 * @author Tony Burdett
 * @date 17-Feb-2010
 */
public class FeatureHandler extends AbstractADFGraphHandler {
  public FeatureHandler() {
    setTag("blockcolumn");
  }

  public int assess() {
    for (int i = 1; i < values.length;) {
      if (headers[i].equals("blockrow")) {
        // don't need to do anything here, just read on to the next column
      }
      else if (headers[i].equals("column")) {
        // don't need to do anything here, just read on to the next column
      }
      else if (headers[i].equals("row")) {
        ReporterGroupHandler handler = new ReporterGroupHandler();
        i += assessAttribute(handler, headers, values, i);
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
    try {
      if (headers[0].equals(tag)) {
        // first values, so lookup or make a new sourceNode
        FeatureNode featureNode = new FeatureNode();

        // set the block column value, as this is the first thing
        featureNode.blockColumn = Integer.parseInt(values[0]);

        // now do the rest
        for (int i = 1; i < values.length;) {
          if (headers[i].equals("blockrow")) {
            featureNode.blockRow = Integer.parseInt(values[i]);
          }
          else if (headers[i].equals("column")) {
            featureNode.column = Integer.parseInt(values[i]);
          }
          else if (headers[i].equals("row")) {
            featureNode.row = Integer.parseInt(values[i]);
          }
          else if (headers[i].startsWith("comment")) {
            String type =
                headers[i].substring(headers[i].lastIndexOf("[") + 1,
                                     headers[i].lastIndexOf("]"));
            featureNode.comments.put(type, values[i]);
          }
          else {
            // got to something we don't recognise
            // this is either the end, or a bad column name
            // check the name of the next node and put it in this one

            // check child node type
            // loop over values until we get to something with a value present
            int k = i;
              while (k < values.length &&
                (Strings.isNullOrEmpty(values[k]))) {
              k++;
            }

            if (k < values.length) {
              // add child node value
              String childNodeType = headers[k];
              String childNodeValue = values[k];
              featureNode.addChildNode(childNodeType, childNodeValue);
            }

            break;
          }
          i++;
        }

        // finally, set the name of featureNode - just use coordinates
        String identifier =
            featureNode.blockColumn + "." + featureNode.column + ":" +
                featureNode.blockRow + "." + featureNode.row;
        featureNode.setNodeType(headers[0]);
        featureNode.setNodeName(identifier);

        // lookup node - fail if duplicate spots
        synchronized (arrayDesign.ADF) {
          FeatureNode existingNode = arrayDesign.ADF.lookupNode(
              identifier, FeatureNode.class);
          if (existingNode != null) {
            String message =
                "Duplicated feature spot at column " + featureNode.blockColumn +
                    "." + featureNode.column + " and row " +
                    featureNode.blockRow + "." + featureNode.row + " " +
                    "(existing feature node with identifier " +
                    existingNode.getNodeName() + ").  Duplicate data will " +
                    "not be saved";

            ErrorItem error =
                ErrorItemFactory
                    .getErrorItemFactory(getClass().getClassLoader())
                    .generateErrorItem(
                        message,
                        ErrorCode.DUPLICATED_FEATURES,
                        this.getClass());

            throw new ParseException(error, false, message);
          }
          else {
            addNextNodeForCompilation(featureNode);
            arrayDesign.ADF.storeNode(featureNode);
          }
        }

        // iterated over every column, so must have reached the end
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
    catch (NumberFormatException e) {
      String message =
          "Feature position could not be determined - check position is " +
              "specified by an integer value";

      ErrorItem error =
          ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
              .generateErrorItem(
                  message,
                  ErrorCode.INCOMPLETE_FEATURES,
                  this.getClass());

      throw new UnmatchedTagException(error, false, message);
    }
  }
}
