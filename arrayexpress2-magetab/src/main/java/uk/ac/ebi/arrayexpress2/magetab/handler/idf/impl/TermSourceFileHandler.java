package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

/**
 * A Handler that handles the IDF rows that point to the file from which term
 * source references are taken from.  This is coupled with a name of a term
 * source.  Ideally, this row should contain real, resolvable URLs accessible
 * from the web - once distributed, term source file lines that point to a file
 * on a local drive are fairly useless.
 * <p/>
 * Tag: Term Source File
 *
 * @author Tony Burdett
 * @date 21-Jan-2009
 */
public class TermSourceFileHandler extends AbstractIDFHandler {
  public TermSourceFileHandler() {
    setTag("termsourcefile");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.termSourceFile.add(value);
  }
}
