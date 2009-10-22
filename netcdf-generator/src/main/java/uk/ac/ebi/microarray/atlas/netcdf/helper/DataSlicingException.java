package uk.ac.ebi.microarray.atlas.netcdf.helper;

import uk.ac.ebi.microarray.atlas.netcdf.NetCDFGeneratorException;

/**
 * An exception generated whenever an attempt to evaluate a slice of data
 * appropriate for NetCDF generation fails.
 *
 * @author Tony Burdett
 * @date 22-Oct-2009
 */
public class DataSlicingException extends NetCDFGeneratorException {
  public DataSlicingException() {
    super();
  }

  public DataSlicingException(String s) {
    super(s);
  }

  public DataSlicingException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public DataSlicingException(Throwable throwable) {
    super(throwable);
  }
}
