package uk.ac.ebi.arrayexpress2.magetab.handler.adf;

import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.mged.magetab.error.ErrorItemFactory;
import uk.ac.ebi.arrayexpress2.magetab.exception.ObjectConversionException;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.lang.Status;

/**
 * A special type of handler that can handle any line from any file, but handles
 * these lines by doing nothing to the model.  This means that any reliance on
 * the progress updaters will not be harmed by having ignored lines in the
 * file.
 *
 * @author Tony Burdett
 * @date 22-May-2009
 */
public class IgnoredHeaderHandler extends AbstractADFGraphHandler {
  public IgnoredHeaderHandler() {
    setTag("*");
  }

  public boolean canHandle(String tag) {
    return true;
  }

  public int assess() {
    return 1;
  }

  public void read() throws ParseException {
    // don't need to read anything, but do update and report non-critical exception
    if (getTaskIndex() != -1) {
      arrayDesign.ADF.updateTaskList(getTaskIndex(), Status.COMPLETE);
    }

    // throw non-critical exception
    String message = "Ignoring value '" + values[0] + "', the header '" +
        headers[0] + "' cannot be read in the current configuration.  " +
        "This may have compromised the structure of the ADF graph.";

    ErrorItem error =
        ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
            .generateErrorItem(
                message, ErrorCode.UNKNOWN_ADF_COLUMN_HEADING, this.getClass());

    throw new ParseException(error, false);
  }

  public void write() throws ObjectConversionException {
    // don't need to write anything - this is probably not required,
    // but someone could have set the mode to "write only"
    if (getTaskIndex() != -1) {
      arrayDesign.ADF.updateTaskList(getTaskIndex(), Status.COMPLETE);
    }

    // throw non-critical exception
    String message = "Unable to write data using IgnoredHeaderHandler - " +
        "there is no associated field in the SDRF to bind to";

    ErrorItem error =
        ErrorItemFactory.getErrorItemFactory(getClass().getClassLoader())
            .generateErrorItem(
                message, ErrorCode.UNKNOWN_ADF_COLUMN_HEADING, this.getClass());

    throw new ObjectConversionException(error, false);
  }

  public void validate() throws ObjectConversionException {
    // don't need to validate anything, as this line is ignored
    // update not required as read() sets task complete
  }
}