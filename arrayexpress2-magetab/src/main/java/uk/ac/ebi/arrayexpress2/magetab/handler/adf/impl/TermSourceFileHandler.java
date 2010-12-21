package uk.ac.ebi.arrayexpress2.magetab.handler.adf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.adf.AbstractADFHeaderHandler;

/**
 * A Handler that handles the ADF rows that point to the file from which term
 * source references are taken from.  This is coupled with a name of a term
 * source.  Ideally, this row should contain real, resolvable URLs accessible
 * from the web - once distributed, term source file lines that point to a file
 * on a local drive are fairly useless.
 * <p/>
 * Tag: Term Source File
 *
 * @author Tony Burdett
 * @date 15-Feb-2010
 */
public class TermSourceFileHandler extends AbstractADFHeaderHandler {
  public TermSourceFileHandler() {
    setTag("termsourcefile");
  }

  public void readValue(String value) throws ParseException {
    arrayDesign.ADF.termSourceFile.add(value);
  }
}
