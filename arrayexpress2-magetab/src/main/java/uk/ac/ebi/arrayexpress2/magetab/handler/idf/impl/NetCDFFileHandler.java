package uk.ac.ebi.arrayexpress2.magetab.handler.idf.impl;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.idf.AbstractIDFHandler;

public class NetCDFFileHandler extends AbstractIDFHandler {
  public NetCDFFileHandler() {
    setTag("netcdffile");
  }

  public void readValue(String value) throws ParseException {
    // read
    investigation.IDF.netCDFFile.add(value);
  }
}
