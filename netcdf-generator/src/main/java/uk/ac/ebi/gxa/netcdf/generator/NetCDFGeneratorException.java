package uk.ac.ebi.gxa.netcdf.generator;

/**
 * An exception that occurs whenever something went wrong during NetCDF
 * generation
 *
 * @author Tony Burdett
 * @date 28-Sep-2009
 */
public class NetCDFGeneratorException extends Exception {
  public NetCDFGeneratorException() {
    super();
  }

  public NetCDFGeneratorException(String s) {
    super(s);
  }

  public NetCDFGeneratorException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public NetCDFGeneratorException(Throwable throwable) {
    super(throwable);
  }
}
