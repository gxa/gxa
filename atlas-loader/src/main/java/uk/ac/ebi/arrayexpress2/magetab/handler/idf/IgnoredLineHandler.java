package uk.ac.ebi.arrayexpress2.magetab.handler.idf;

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
public class IgnoredLineHandler extends AbstractIDFHandler {
  public IgnoredLineHandler() {
    setTag("*");
  }

  public boolean canHandle(String tag) {
    return true;
  }

  public void read() throws ParseException {
    // don't need to read anything, but do update
    if (getTaskIndex() != -1) {
      investigation.IDF.updateTaskList(getTaskIndex(), Status.READING);
    }
    readEmptyValue();
    getLog().trace("IDF Handler reading, ignored line: " + line);
  }

  public void write() throws ObjectConversionException {
    // don't need to write anything, but do update
    if (getTaskIndex() != -1) {
      investigation.IDF.updateTaskList(getTaskIndex(), Status.COMPILING);
    }
    getLog().trace("IDF Handler writing, ignored line: " + line);
  }

  public void validate() throws ObjectConversionException {
    // don't need to validate anything, but do update
    if (getTaskIndex() != -1) {
      investigation.IDF.updateTaskList(getTaskIndex(), Status.VALIDATING);
    }
    getLog().trace("IDF Handler validating, ignored line: " + line);
  }
}
